package com.grupo3.nexo.data.avisos

import android.app.ActivityManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.hilt.work.HiltWorker
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.grupo3.nexo.MainActivity
import com.grupo3.nexo.R
import com.grupo3.nexo.data.local.AlmacenSesion
import com.grupo3.nexo.data.remote.NexoApi
import com.grupo3.nexo.data.remote.RolDto
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Pregunta al servidor si hay algo nuevo y lo avisa con una notificación. Android lo corre como mínimo cada 15 minutos y
 * puede demorarlo para ahorrar batería: es un aviso de "pronto", no al instante.
 */
@HiltWorker
class RevisarNovedadesWorker @AssistedInject constructor(
    @Assisted private val contexto: Context,
    @Assisted parametros: WorkerParameters,
    private val api: NexoApi,
    private val almacen: AlmacenSesion,
    private val json: Json
) : CoroutineWorker(contexto, parametros) {

    override suspend fun doWork(): Result {
        almacen.esperarCarga()
        if (almacen.sesion.value == null) return Result.success()
        try {
            val datos = leerDatos()
            val guardadas = contexto.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE)
            val antes = guardadas.getString(CLAVE_FOTO, null)?.let { texto ->
                runCatching { json.decodeFromString<FotoNovedades>(texto) }.getOrNull()
            }
            val (novedades, foto) = calcularNovedades(antes, datos, Instant.now())
            // Con la app abierta, la persona ya lo está viendo en pantalla
            if (!appEnPrimerPlano()) novedades.forEach(::notificar)
            guardadas.edit().putString(CLAVE_FOTO, json.encodeToString(foto)).apply()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Sin red o con el servidor caído no hay nada que avisar: se reintenta en la siguiente vuelta.
            // Solo el tipo de error: el mensaje podría traer datos de la respuesta
            Log.w(ETIQUETA, "No se pudo revisar: ${e.javaClass.simpleName}")
        }
        return Result.success()
    }

    private suspend fun leerDatos(): DatosNovedades {
        val perfil = api.perfilPropio()
        val esTutor = perfil.perfilTutorActivo
        return DatosNovedades(
            recibidas = if (esTutor) api.solicitudes(RolDto.TUTOR) else emptyList(),
            enviadas = api.solicitudes(RolDto.ESTUDIANTE),
            chats = api.chats(RolDto.ESTUDIANTE) + if (esTutor) api.chats(RolDto.TUTOR) else emptyList(),
            sesiones = api.sesiones(RolDto.ESTUDIANTE) + if (esTutor) api.sesiones(RolDto.TUTOR) else emptyList()
        )
    }

    private fun appEnPrimerPlano(): Boolean {
        val proceso = ActivityManager.RunningAppProcessInfo()
        ActivityManager.getMyMemoryState(proceso)
        return proceso.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND
    }

    private fun notificar(novedad: Novedad) {
        val gestor = NotificationManagerCompat.from(contexto)
        if (!gestor.areNotificationsEnabled()) return
        gestor.createNotificationChannel(
            NotificationChannel(CANAL, "Tutorías y mensajes", NotificationManager.IMPORTANCE_DEFAULT)
        )
        val abrir = PendingIntent.getActivity(
            contexto, 0, Intent(contexto, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val notificacion = NotificationCompat.Builder(contexto, CANAL)
            .setSmallIcon(R.drawable.ph_campana)
            .setContentTitle(novedad.titulo)
            .setContentText(novedad.texto)
            .setStyle(NotificationCompat.BigTextStyle().bigText(novedad.texto))
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .build()
        try {
            gestor.notify(novedad.clave.hashCode(), notificacion)
        } catch (e: SecurityException) {
            // La persona retiró el permiso entre la comprobación y el aviso
        }
    }

    companion object {
        private const val ETIQUETA = "NexoAvisos"
        private const val TRABAJO = "revisar-novedades"
        private const val CANAL = "novedades"
        private const val PREFERENCIAS = "avisos"
        private const val CLAVE_FOTO = "foto"
        private const val MINUTOS_ENTRE_REVISIONES = 15L

        fun programar(contexto: Context) {
            val pedido = PeriodicWorkRequestBuilder<RevisarNovedadesWorker>(MINUTOS_ENTRE_REVISIONES, TimeUnit.MINUTES)
                .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
                .build()
            WorkManager.getInstance(contexto).enqueueUniquePeriodicWork(TRABAJO, ExistingPeriodicWorkPolicy.KEEP, pedido)
        }

        /** Al cerrar sesión: la siguiente cuenta no debe heredar ni los avisos ni lo que ya se vio. */
        fun detener(contexto: Context) {
            WorkManager.getInstance(contexto).cancelUniqueWork(TRABAJO)
            contexto.getSharedPreferences(PREFERENCIAS, Context.MODE_PRIVATE).edit().clear().apply()
            NotificationManagerCompat.from(contexto).cancelAll()
        }
    }
}
