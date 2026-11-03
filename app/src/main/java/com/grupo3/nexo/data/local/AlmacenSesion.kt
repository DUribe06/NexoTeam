package com.grupo3.nexo.data.local

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.grupo3.nexo.data.remote.SesionDto
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * Tokens y usuario de la sesión abierta. Los cambios se ven de inmediato en [sesion], porque el interceptor y el
 * autenticador de OkHttp la leen desde hilos de red sin poder suspender; el disco se actualiza después.
 */
interface AlmacenSesion {
    val sesion: StateFlow<SesionDto?>

    /** Espera a que se lea la sesión guardada: antes de eso, un null no significa que no haya sesión. */
    suspend fun esperarCarga() {}

    fun guardar(nueva: SesionDto)
    fun borrar()
}

private val Context.datosSesion by preferencesDataStore(name = "sesion")
private val CLAVE_SESION = stringPreferencesKey("sesion")

@Singleton
class AlmacenSesionDataStore @Inject constructor(
    @ApplicationContext contexto: Context,
    private val json: Json
) : AlmacenSesion {

    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val datos = contexto.datosSesion
    private val _sesion = MutableStateFlow<SesionDto?>(null)
    /** Si ya hubo un cambio, lo leído del disco al arrancar es viejo y no debe pisarlo. */
    private var modificada = false

    private val cargada = CompletableDeferred<Unit>()

    override val sesion: StateFlow<SesionDto?> = _sesion.asStateFlow()

    override suspend fun esperarCarga() = cargada.await()

    init {
        alcance.launch {
            val preferencias = datos.data.catch { e ->
                if (e is IOException) emit(emptyPreferences()) else throw e
            }.first()
            val leida = preferencias[CLAVE_SESION]?.let(::decodificar)
            synchronized(this@AlmacenSesionDataStore) {
                if (!modificada) _sesion.value = leida
            }
            cargada.complete(Unit)
        }
    }

    override fun guardar(nueva: SesionDto) = cambiar(nueva)

    override fun borrar() = cambiar(null)

    private fun cambiar(valor: SesionDto?) {
        synchronized(this) {
            modificada = true
            _sesion.value = valor
        }
        alcance.launch {
            // Se escribe lo vigente al ejecutarse, no [valor]: así el disco termina igual a la memoria en cualquier orden
            datos.edit { preferencias ->
                val vigente = _sesion.value
                if (vigente == null) preferencias.remove(CLAVE_SESION)
                else preferencias[CLAVE_SESION] = json.encodeToString(SesionDto.serializer(), vigente)
            }
        }
    }

    /** Una sesión guardada que ya no se puede leer equivale a no tener sesión: se vuelve a entrar. */
    private fun decodificar(texto: String): SesionDto? =
        try {
            json.decodeFromString(SesionDto.serializer(), texto)
        } catch (e: SerializationException) {
            null
        } catch (e: IllegalArgumentException) {
            null
        }
}
