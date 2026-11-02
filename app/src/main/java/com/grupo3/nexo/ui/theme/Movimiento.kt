package com.grupo3.nexo.ui.theme

import android.provider.Settings
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LifecycleResumeEffect

/*
 * Movimiento de Nexo: transiciones cortas y con propósito, sin brillos.
 * Toda la app usa estos tokens. Las excepciones aprobadas, con valores propios, son la
 * coreografía de BarraNavegacion, las escenas y la pila del Inicio y las animaciones del acceso.
 */

/* Duraciones (ms) */
/** Salida de fundidos. */
const val DuracionInstante = 120
/** Color, borde, foco y pestaña. */
const val DuracionCorta = 160
/** Entrada de fundidos y cambio de contenido. */
const val DuracionMedia = 220
/** Volver atrás y salidas con desplazamiento. */
const val DuracionNormal = 300
/** Avanzar de pantalla y mostrar la barra. */
const val DuracionLarga = 380
/** Aparición escalonada. */
const val DuracionAmplia = 480
/** Desplazamiento entre pestañas (valor exacto, sin redondear). */
const val DuracionPestana = 340
/** Pulso del esqueleto (la ida). */
const val DuracionPulso = 850

/* Escalonado (ms) */
const val RetrasoInicial = 40
const val RetrasoEscalon = 60
/** El fundido de entrada espera a que termine la salida. */
const val RetrasoFundido = 60

/* Curvas: las tres primeras son las enfatizadas de Material 3 que ya usa NexoApp.kt */
val CurvaEntrada: Easing = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
val CurvaSalida: Easing = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
val CurvaEnfatizada: Easing = CubicBezierEasing(0.2f, 0f, 0f, 1f)
val CurvaEstandar: Easing = FastOutSlowInEasing
val CurvaAparicion: Easing = EaseOutCubic

/* Resortes */
fun <T> resortePresion(): SpringSpec<T> = spring(dampingRatio = 1f, stiffness = 900f)
fun <T> resorteIndicador(): SpringSpec<T> = spring(dampingRatio = 0.78f, stiffness = 500f)
fun <T> resorteCascada(): SpringSpec<T> = spring(dampingRatio = 0.68f, stiffness = 320f)
/** Para confirmar algo: casilla, enviar, estrellas. */
fun <T> resorteRebote(): SpringSpec<T> = spring(dampingRatio = 0.5f, stiffness = 600f)

/* Escalas al presionar */
const val EscalaPresionTarjeta = 0.98f
const val EscalaPresionBoton = 0.97f
const val EscalaPresionBotonCompacto = 0.96f

/* Specs listas */
fun <T> especColor() = tween<T>(DuracionCorta)
fun <T> especFundidoEntrada() = tween<T>(DuracionMedia, delayMillis = RetrasoFundido)
fun <T> especFundidoSalida() = tween<T>(DuracionInstante)
fun <T> especAparicion(orden: Int) = tween<T>(
    durationMillis = DuracionAmplia,
    delayMillis = RetrasoInicial + orden * RetrasoEscalon,
    easing = CurvaAparicion,
)
fun especPulso(): InfiniteRepeatableSpec<Float> =
    infiniteRepeatable(tween(DuracionPulso, easing = CurvaEstandar), RepeatMode.Reverse)

fun cambioContenido(): ContentTransform =
    fadeIn(especFundidoEntrada()) togetherWith fadeOut(especFundidoSalida())

/**
 * Vale true si el usuario puso en 0 la escala de animaciones del sistema.
 * Lo provee NexoTema.
 */
val LocalAnimacionesReducidas = staticCompositionLocalOf { false }

/** Se vuelve a leer al reanudar la app: el ajuste puede cambiar desde los ajustes del sistema. */
@Composable
internal fun leerAnimacionesReducidas(): Boolean {
    val resolver = LocalContext.current.contentResolver
    val leer = {
        runCatching {
            Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
        }.getOrDefault(false)
    }
    var reducidas by remember(resolver) { mutableStateOf(leer()) }
    LifecycleResumeEffect(resolver) {
        reducidas = leer()
        onPauseOrDispose { }
    }
    return reducidas
}
