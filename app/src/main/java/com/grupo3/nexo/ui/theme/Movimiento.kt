package com.grupo3.nexo.ui.theme

import android.provider.Settings
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.InfiniteRepeatableSpec
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.VisibilityThreshold
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
import androidx.compose.ui.unit.Dp
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
/** Resorte por defecto de Compose, explícito para los `animate*AsState` que no llevan otro. */
fun <T> resorteSuave(): SpringSpec<T> = spring()
/** Resorte por defecto de `animateDpAsState` (umbral de visibilidad de [Dp]). */
fun resorteSuaveDp(): SpringSpec<Dp> = spring(visibilityThreshold = Dp.VisibilityThreshold)
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

/** Escala de animación del sistema (0 = apagada) a partir de la cual se consideran reducidas. */
private const val UmbralEscalaReducida = 0.1f

/**
 * Vale true si el usuario apagó la escala de animaciones del sistema o la dejó tan baja
 * (hasta [UmbralEscalaReducida]) que el movimiento ya no se percibe.
 * Lo provee NexoTema.
 */
val LocalAnimacionesReducidas = staticCompositionLocalOf { false }

/** Se vuelve a leer al reanudar la app: el ajuste puede cambiar desde los ajustes del sistema. */
@Composable
internal fun leerAnimacionesReducidas(): Boolean {
    val resolver = LocalContext.current.contentResolver
    val leer = {
        runCatching {
            Settings.Global.getFloat(resolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) <= UmbralEscalaReducida
        }.getOrDefault(false)
    }
    var reducidas by remember(resolver) { mutableStateOf(leer()) }
    LifecycleResumeEffect(resolver) {
        reducidas = leer()
        onPauseOrDispose { }
    }
    return reducidas
}

/* ---------- Propios del acceso (ms) ---------- */
/** Acercamiento lento de la foto de portada, ida y vuelta. */
const val DuracionZoomPortada = 18_000
/** Flotación del logo, ida y vuelta. */
const val DuracionFlotacionLogo = 3_000
/** Aplaste del logo al tocarlo, antes del rebote. */
const val DuracionAplasteLogo = 90
/** Entrada del contenido del botón principal al cambiar de estado. */
const val DuracionEntraBotonAcceso = 180
/** Aparición escalonada del formulario: duración, espera inicial y paso entre elementos. */
const val DuracionEntradaAcceso = 520
const val RetrasoEntradaAcceso = 80
const val PasoEntradaAcceso = 70
/** Rebote elástico del logo del acceso tras el aplaste. */
fun <T> resorteLogoAcceso(): SpringSpec<T> = spring(dampingRatio = 0.3f, stiffness = 500f)

/* ---------- Pila de sesiones del Inicio (ms) ---------- */
/** La elegida sube al frente: aparición, espera, entrada, apagado de la saliente y cambio de alto. */
const val DuracionPilaAparece = 240
const val RetrasoPilaAparece = 90
const val DuracionPilaEntra = 460
const val DuracionPilaSeApaga = 170
const val DuracionPilaTamano = 420
/** Cascada de las capas: espera inicial y retraso entre una capa y la siguiente. */
const val RetrasoCascadaPila = 260L
const val PasoCascadaPila = 110L
/** Escala de la tarjeta que baja a la pila. */
const val EscalaPilaSaliente = .94f
/** Capas de atrás: espera al aparecer y apagado al salir. */
const val RetrasoCapaPila = 80
const val DuracionCapaSeApaga = 140
