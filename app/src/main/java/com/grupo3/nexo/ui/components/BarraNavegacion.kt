package com.grupo3.nexo.ui.components

import android.provider.Settings
import androidx.annotation.DrawableRes
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.grupo3.nexo.R
import com.grupo3.nexo.ui.navigation.Rutas
import com.grupo3.nexo.ui.theme.Bosque
import com.grupo3.nexo.ui.theme.BosqueProfundo
import com.grupo3.nexo.ui.theme.Salvia
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.exp
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.math.sin

private data class Destino(
    val ruta: String,
    val etiqueta: String,
    @DrawableRes val icono: Int,
    val navegar: String = ruta
)

/* Íconos Phosphor Regular, la misma familia que la campana de la cabecera.
 * Buscar no está aquí: se abre desde el buscador y las áreas del Inicio. */
private val destinos = listOf(
    Destino(Rutas.Inicio.ruta, "Inicio", R.drawable.ph_casa),
    Destino(Rutas.Solicitudes.ruta, "Solicitudes", R.drawable.ph_calendario),
    Destino(Rutas.Chat.ruta, "Chats", R.drawable.ph_chat),
    Destino(Rutas.Perfil.ruta, "Perfil", R.drawable.ph_usuario)
)

private val ALTO_ONDA = 22.dp
private val ALTO_BARRA = 52.dp
/** Alto con el que se diseñó la onda (viewBox de 88): su forma no cambia aunque la barra sea más baja. */
private val ALTO_DISENO_ONDA = 86.dp
private val TAM_ESLABON = 34.dp
/** Color y grosor del trazo de los eslabones (medidos en eslabon_1.png / eslabon_2.png). */
private val ColorEslabon = Color(0xFF153A28)
private const val GROSOR_BRAZO = 2.75f

/** Separación (dp) entre las caras de los dos brazos con los eslabones juntos: hasta ahí la liga aún no existe. */
private const val SEPARACION_REPOSO = 6.9f

/** Cara cortada del brazo abierto: su centro, la dirección en que sale el brazo y el vector del centro a una esquina. */
private class Cara(val centro: Offset, val dir: Offset, val mitad: Offset)

/**
 * Cara cortada en diagonal del brazo abierto de un eslabón. Medida en eslabon_1.png (240 px): va de (122, 107) a
 * (136,5, 92) y el brazo sale por ella hacia arriba a la izquierda. La liga nace exactamente de esa cara (sus dos
 * esquinas son las de la liga), así no queda hueco ni sobra material. El eslabón 2 es el 1 girado media vuelta
 * ([lado] = -1) y corrido 6 px a la izquierda y 1 abajo. Sigue el giro y la escala del eslabón.
 */
private fun caraEslabon(p: Pieza, lado: Float, tam: Float): Cara {
    val corrimientoX = if (lado < 0f) -6f / 240f else 0f
    val corrimientoY = if (lado < 0f) 1f / 240f else 0f
    val ancho = tam * p.escala * p.ex
    val alto = tam * p.escala * p.ey
    val r = Math.toRadians(p.giro.toDouble())
    val cos = kotlin.math.cos(r).toFloat()
    val sen = kotlin.math.sin(r).toFloat()
    fun girar(x: Float, y: Float) = Offset(x * cos - y * sen, x * sen + y * cos)
    val c = girar((lado * 9.3f / 240f + corrimientoX) * ancho, (lado * -20.4f / 240f + corrimientoY) * alto)
    val m = girar(lado * 7.2f / 240f * ancho, lado * -7.3f / 240f * alto)
    return Cara(Offset(p.x + c.x, p.y + c.y), girar(lado * -0.42f, lado * -0.91f), m)
}

/** Alto en reposo de la joroba bajo los eslabones (unidades del viewBox de 88). */
private const val ALTO_JOROBA = 12f

/* Curvas de la coreografía (equivalentes a las de la maqueta). */
private val SalidaSuave: Easing = CubicBezierEasing(0.25f, 0.46f, 0.45f, 0.94f)
private val SalidaFuerte: Easing = CubicBezierEasing(0.215f, 0.61f, 0.355f, 1f)
private val EntradaSuave: Easing = CubicBezierEasing(0.55f, 0.085f, 0.68f, 0.53f)
private val EntradaFuerte: Easing = CubicBezierEasing(0.55f, 0.055f, 0.675f, 0.19f)
private val Ambas: Easing = CubicBezierEasing(0.455f, 0.03f, 0.515f, 0.955f)
private val EntradaMuyFuerte: Easing = CubicBezierEasing(0.895f, 0.03f, 0.685f, 0.22f)

/** Algo que se anima a mano: un eslabón (posición absoluta) o un ícono (desplazamiento sobre su sitio). */
private class Pieza {
    var x by mutableFloatStateOf(0f)
    var y by mutableFloatStateOf(0f)
    var giro by mutableFloatStateOf(0f)
    var escala by mutableFloatStateOf(1f)
    var ex by mutableFloatStateOf(1f)
    var ey by mutableFloatStateOf(1f)
    var alfa by mutableFloatStateOf(1f)

    fun libre() { x = 0f; y = 0f; giro = 0f; escala = 1f; ex = 1f; ey = 1f; alfa = 1f }
    fun atrapada() { libre(); alfa = 0f; ex = 0.4f; ey = 0.4f }
}

/** Onda del fondo: sigue a los eslabones con un resorte y se levanta más cuanto más rápido van. */
private class Onda {
    var cx by mutableFloatStateOf(Float.NaN)
    var vx by mutableFloatStateOf(0f)
    var alto by mutableFloatStateOf(ALTO_JOROBA)
    var va by mutableFloatStateOf(0f)
}

private fun lerp(a: Float, b: Float, t: Float) = a + (b - a) * t

/** Evita que un NaN o infinito llegue a `roundToInt()`, que lanzaría una excepción. */
private fun Float.finito() = if (isFinite()) this else 0f

/** Máximo suave: une dos jorobas sin dejar una "V" en punta donde se cruzan. */
private fun maxSuave(a: Float, b: Float, k: Float): Float {
    val h = (0.5f + 0.5f * (b - a) / k).coerceIn(0f, 1f)
    return lerp(a, b, h) + k * h * (1 - h)
}

/**
 * La liga como una cuerda elástica de verdad: una cadena de puntos con inercia (integración de Verlet),
 * tramos que mantienen su largo, algo de rigidez para que se doble sin quiebres y los extremos clavados
 * en la boca de cada eslabón. Así se retrasa, ondula cuando los eslabones se mueven y vibra al soltarse.
 */
private class Cuerda {
    val tramos = 18
    val x = FloatArray(tramos + 1)
    val y = FloatArray(tramos + 1)
    private val xPrevio = FloatArray(tramos + 1)
    private val yPrevio = FloatArray(tramos + 1)
    private val suaveX = FloatArray(tramos + 1)
    private val suaveY = FloatArray(tramos + 1)
    var activa = false
        private set
    /** Cambia en cada paso para que el lienzo vuelva a dibujarse. */
    var cuadro by mutableIntStateOf(0)

    fun soltar() {
        if (activa) { activa = false; cuadro++ }
    }

    private var aPrevio = Offset.Unspecified
    private var bPrevio = Offset.Unspecified

    fun paso(a: Offset, da: Offset, b: Offset, db: Offset, dt: Float, dp: Float) {
        val n = tramos
        if (!activa) {
            for (i in 0..n) {
                val t = i / n.toFloat()
                x[i] = lerp(a.x, b.x, t); y[i] = lerp(a.y, b.y, t)
                xPrevio[i] = x[i]; yPrevio[i] = y[i]
            }
            aPrevio = a; bPrevio = b
            activa = true
        }
        // Si un extremo avanzó mucho desde el cuadro anterior, se simula en pasos pequeños interpolando
        // su recorrido: así la cuerda no rebota en zigzag aunque un cuadro llegue tarde
        val recorrido = max((a - aPrevio).getDistance(), (b - bPrevio).getDistance())
        val subpasos = (recorrido / (3f * dp)).toInt().coerceIn(1, 16)
        for (k in 1..subpasos) {
            val t = k / subpasos.toFloat()
            subpaso(lerp(aPrevio, a, t), da, lerp(bPrevio, b, t), db, dt / subpasos, dp)
        }
        aPrevio = a; bPrevio = b
        cuadro++
    }

    private fun subpaso(a: Offset, da: Offset, b: Offset, db: Offset, dt: Float, dp: Float) {
        val n = tramos
        val separacion = hypot(b.x - a.x, b.y - a.y)
        // Apenas floja: cuelga un poco y ondula, pero no se arruga
        val largoTramo = (separacion * 1.02f + 3f * dp) / n
        val gravedad = 520f * dp * dt * dt
        // Fricción por segundo, independiente de cuántos pasos haya
        val friccion = 0.0015f.pow(dt)
        val limite = 4f * dp
        for (i in 1 until n) {
            val vx = ((x[i] - xPrevio[i]) * friccion).coerceIn(-limite, limite)
            val vy = ((y[i] - yPrevio[i]) * friccion).coerceIn(-limite, limite)
            xPrevio[i] = x[i]; yPrevio[i] = y[i]
            x[i] += vx; y[i] += vy + gravedad
        }
        x[0] = a.x; y[0] = a.y; x[n] = b.x; y[n] = b.y
        repeat(8) { vuelta ->
            // Se alterna el sentido de la corrección para que ningún extremo acumule el error
            for (m in 0 until n) {
                val i = if (vuelta % 2 == 0) m else n - 1 - m
                val dx = x[i + 1] - x[i]
                val dy = y[i + 1] - y[i]
                val d = hypot(dx, dy).coerceAtLeast(0.0001f)
                val error = (d - largoTramo) / d
                val pesoA = if (i == 0) 0f else if (i + 1 == n) 1f else 0.5f
                val pesoB = 1f - pesoA
                x[i] += dx * error * pesoA; y[i] += dy * error * pesoA
                x[i + 1] -= dx * error * pesoB; y[i + 1] -= dy * error * pesoB
            }
            // Rigidez: cada punto se acerca al promedio de sus vecinos (curvas suaves, sin codos ni dientes)
            for (i in 1 until n) {
                suaveX[i] = (x[i - 1] + x[i + 1]) / 2
                suaveY[i] = (y[i - 1] + y[i + 1]) / 2
            }
            for (i in 1 until n) {
                x[i] = lerp(x[i], suaveX[i], 0.3f)
                y[i] = lerp(y[i], suaveY[i], 0.3f)
            }
        }
    }
}

private suspend fun tramo(ms: Int, curva: Easing, alAvanzar: (Float) -> Unit) =
    animate(0f, 1f, animationSpec = tween(ms, easing = curva)) { v, _ -> alAvanzar(v) }

private suspend fun resorte(desde: Float, hasta: Float, amortiguacion: Float, rigidez: Float, alAvanzar: (Float) -> Unit) =
    animate(desde, hasta, animationSpec = spring(amortiguacion, rigidez)) { v, _ -> alAvanzar(v) }

/* Borde superior original de la barra (viewBox 414 × 88): M0 22 C115 4 299 36 414 14, muestreado como y(x). */
private val bordeBase: List<Pair<Float, Float>> = (0..120).map { k ->
    val t = k / 120f
    val u = 1 - t
    (3 * u * u * t * 115 + 3 * u * t * t * 299 + t * t * t * 414) to (u * u * u * 22 + 3 * u * u * t * 4 + 3 * u * t * t * 36 + t * t * t * 14)
}

private fun yBase(x: Float): Float {
    for (k in 1 until bordeBase.size) {
        val (x1, y1) = bordeBase[k]
        if (x1 >= x) {
            val (x0, y0) = bordeBase[k - 1]
            return y0 + (y1 - y0) * ((x - x0) / ((x1 - x0).takeIf { it != 0f } ?: 1f))
        }
    }
    return 14f
}

/**
 * Barra de navegación inferior, solo con íconos. Los dos eslabones del logo marcan la pestaña activa:
 * al cambiar, uno sale disparado y el otro lo sigue estirando una liga hasta soltarse como resortera;
 * golpean al nuevo ícono y se cierran sobre él ("Liga elástica"). El fondo blanco se levanta bajo los eslabones
 * y los sigue. Respeta la opción del sistema de quitar animaciones.
 */
@Composable
fun BarraNavegacion(
    actual: String,
    pendientes: Int,
    noLeidos: Int,
    alSeleccionar: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val indiceRuta = destinos.indexOfFirst { it.ruta == actual }
    // En pantallas que no son pestañas (no debería pasar) se queda donde estaba.
    val ultimoIndice = remember { mutableIntStateOf(0) }
    if (indiceRuta >= 0) ultimoIndice.intValue = indiceRuta
    val activo = ultimoIndice.intValue

    val contexto = LocalContext.current
    val sinAnimaciones = remember {
        Settings.Global.getFloat(contexto.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
    }
    val haptica = LocalHapticFeedback.current

    val e1 = remember { Pieza() }
    val e2 = remember { Pieza() }
    val iconos = remember { List(destinos.size) { Pieza() } }
    val globos = remember { List(destinos.size) { mutableFloatStateOf(1f) } }
    // Una onda bajo cada eslabón: juntos forman una sola joroba; al separarse, cada uno levanta la suya
    val ondas = remember { listOf(Onda(), Onda()) }
    val liga = remember { mutableStateOf(false) }
    val anterior = remember { mutableIntStateOf(-1) }
    val velX = remember { mutableFloatStateOf(0f) }   // velocidad del centro de los eslabones, en px/s
    val velY = remember { mutableFloatStateOf(0f) }
    val cuerda = remember { Cuerda() }
    val eslabones = remember { arrayOf(e1, e2) }
    // Búferes reutilizados entre cuadros para no generar basura mientras se anima
    val bordePath = remember { Path() }
    val rellenoPath = remember { Path() }
    val puntosLiga = remember { ArrayList<Offset>(80) }
    val arcos = remember { FloatArray(80) }

    // La franja de la onda se monta sobre el contenido, para que este pase por detrás de la curva.
    Column(modifier.fillMaxWidth().solaparHaciaArriba(ALTO_ONDA)) {
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(ALTO_ONDA + ALTO_BARRA)
        ) {
            val densidad = LocalDensity.current
            val ancho = constraints.maxWidth.toFloat()
            val alto = constraints.maxHeight.toFloat()
            val tamEslabon = with(densidad) { TAM_ESLABON.toPx() }
            val dp = densidad.density
            fun centro(i: Int) = Offset(ancho * (i + 0.5f) / destinos.size, with(densidad) { (ALTO_ONDA + ALTO_BARRA / 2).toPx() })

            // Coreografía: se relanza en cada cambio de pestaña; si llega otro toque a mitad, la anterior se cancela
            // y la nueva arranca desde donde quedaron los eslabones.
            LaunchedEffect(activo, ancho) {
                val previo = anterior.intValue
                anterior.intValue = activo
                val b = centro(activo)
                if (previo < 0 || previo == activo || sinAnimaciones || ancho == 0f) {
                    listOf(e1, e2).forEach { it.libre(); it.x = b.x; it.y = b.y }
                    iconos.forEachIndexed { k, p -> if (k == activo) p.atrapada() else p.libre() }
                    globos.forEachIndexed { k, g -> g.floatValue = if (k == activo) 0f else 1f }
                    ondas.forEach { if (it.cx.isNaN() || sinAnimaciones) it.cx = b.x * 414f / ancho.coerceAtLeast(1f) }
                    return@LaunchedEffect
                }
                // Más ágil entre pestañas vecinas; el recorrido largo conserva el tiempo completo
                val ritmo = (0.72f + 0.14f * (abs(activo - previo) - 1)).coerceAtMost(1f)
                try {
                    coreografiaLiga(
                        e1, e2, iconos, globos, liga, previo, activo, b, dp, ritmo,
                        alSoltar = { haptica.performHapticFeedback(HapticFeedbackType.TextHandleMove) }
                    ) {
                        haptica.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                    }
                } finally {
                    // Si un toque nuevo la cancela a medias, la liga no se queda tendida con estado a medio animar
                    liga.value = false
                }
            }

            // Latido por cuadro: velocidad de los eslabones y resorte de la onda.
            LaunchedEffect(ancho) {
                if (sinAnimaciones || ancho == 0f) return@LaunchedEffect
                var ultimo = 0L
                var px = Float.NaN
                var py = Float.NaN
                while (true) {
                    val quieto = withFrameNanos { t ->
                        val dt = if (ultimo == 0L) 0.016f else ((t - ultimo) / 1e9f).coerceIn(0.001f, 0.032f)
                        ultimo = t
                        val x = (e1.x + e2.x) / 2
                        val y = (e1.y + e2.y) / 2
                        if (!px.isNaN()) {
                            val nx = velX.floatValue + ((x - px) / dt - velX.floatValue) * 0.35f
                            val ny = velY.floatValue + ((y - py) / dt - velY.floatValue) * 0.35f
                            // En reposo no se escribe nada, para no redibujar sin motivo.
                            velX.floatValue = if (abs(nx) < 0.5f) 0f else nx
                            velY.floatValue = if (abs(ny) < 0.5f) 0f else ny
                        }
                        px = x; py = y
                        // La cuerda se simula solo mientras la liga existe
                        if (liga.value) {
                            val a = caraEslabon(e1, 1f, tamEslabon)
                            val b = caraEslabon(e2, -1f, tamEslabon)
                            cuerda.paso(a.centro, a.dir, b.centro, b.dir, dt, dp)
                        } else {
                            cuerda.soltar()
                        }
                        var ondasQuietas = true
                        for (k in 0..1) {
                            val onda = ondas[k]
                            val objetivo = eslabones[k].x * 414f / ancho
                            if (onda.cx.isNaN()) onda.cx = objetivo
                            val quieta = abs(objetivo - onda.cx) < 0.05f && abs(onda.vx) < 0.02f && abs(onda.va) < 0.02f && abs(onda.alto - ALTO_JOROBA) < 0.02f
                            if (!quieta) {
                                ondasQuietas = false
                                onda.vx += (170f * (objetivo - onda.cx) - 19f * onda.vx) * dt
                                onda.cx += onda.vx * dt
                                // Se levanta poco con la velocidad y sin rebotes: más ancha que alta, nunca en punta
                                val altoObjetivo = ALTO_JOROBA + min(7f, abs(onda.vx) * 0.016f)
                                onda.va += (220f * (altoObjetivo - onda.alto) - 26f * onda.va) * dt
                                onda.alto += onda.va * dt
                            }
                        }
                        ondasQuietas && !liga.value && !cuerda.activa && velX.floatValue == 0f && velY.floatValue == 0f
                    }
                    if (quieto) {
                        // Todo en reposo: se deja de pedir cuadros hasta que algo vuelva a moverse
                        val foto = listOf(e1.x, e1.y, e2.x, e2.y, liga.value)
                        snapshotFlow { listOf(e1.x, e1.y, e2.x, e2.y, liga.value) }.first { it != foto }
                        ultimo = 0L
                        px = Float.NaN
                    }
                }
            }

            // Fondo blanco con la onda y una sombra difusa hacia arriba (Android 9+; antes queda solo el filo)
            val pinturaFondo = remember {
                Paint().asFrameworkPaint().apply {
                    isAntiAlias = true
                    color = android.graphics.Color.WHITE
                    setShadowLayer(14f * dp, 0f, -2f * dp, BosqueProfundo.copy(alpha = 0.09f).toArgb())
                }
            }
            Canvas(Modifier.fillMaxSize()) {
                val sx = size.width / 414f
                val sy = ALTO_DISENO_ONDA.toPx() / 88f
                val reposo = centro(activo).x / sx
                val borde = bordePath.also { it.rewind() }
                var k = 0f
                while (k <= 414f) {
                    var altura0 = 0f
                    var altura1 = 0f
                    var hundimiento = 0f
                    for (n in 0..1) {
                        val onda = ondas[n]
                        val cx = if (onda.cx.isNaN()) reposo else onda.cx
                        val anchoCampana = 32f + min(36f, abs(onda.vx) * 0.04f)
                        val estela = min(5f, abs(onda.vx) * 0.005f)
                        val g = exp(-((k - cx) * (k - cx)) / (2 * anchoCampana * anchoCampana))
                        val tr = exp(-((k - (cx - sign(onda.vx) * anchoCampana * 1.8f)).let { it * it }) / (2 * anchoCampana * anchoCampana))
                        if (n == 0) altura0 = onda.alto * g else altura1 = onda.alto * g
                        hundimiento += estela * tr
                    }
                    val levante = maxSuave(altura0, altura1, 6f)
                    val yy = (yBase(k) - levante + hundimiento) * sy
                    if (k == 0f) borde.moveTo(0f, yy) else borde.lineTo(k * sx, yy)
                    k += 3f
                }
                val relleno = rellenoPath.apply {
                    rewind()
                    addPath(borde)
                    lineTo(size.width, size.height)
                    lineTo(0f, size.height)
                    close()
                }
                drawIntoCanvas { it.nativeCanvas.drawPath(relleno.asAndroidPath(), pinturaFondo) }
                drawPath(borde, Bosque.copy(alpha = 0.07f), style = Stroke(width = 1.dp.toPx()))
            }

            // La liga: una curva suave que pasa por los puntos de la cuerda. Nace centrada en la cara cortada de cada
            // brazo y tan ancha como ella, y se afina hacia el centro. Va por debajo de los eslabones: la parte que
            // entra en el brazo queda tapada, así no hay hueco, ni costura, ni nada encima.
            Canvas(Modifier.fillMaxSize()) {
                cuerda.cuadro
                if (liga.value && cuerda.activa) {
                    val n = cuerda.tramos
                    val caraA = caraEslabon(e1, 1f, tamEslabon)
                    val caraB = caraEslabon(e2, -1f, tamEslabon)
                    // Los extremos se dibujan en la cara de este cuadro (la simulación pudo correr un cuadro antes):
                    // el desfase se reparte a lo largo de la cuerda, así nunca se ve despegada de un eslabón
                    val desfaseA = caraA.centro - Offset(cuerda.x[0], cuerda.y[0])
                    val desfaseB = caraB.centro - Offset(cuerda.x[n], cuerda.y[n])
                    val separacion = (caraB.centro - caraA.centro).getDistance() / dp
                    // Nace al separarse los eslabones: aparece al pasar de la separación en reposo y gana opacidad enseguida
                    val aparicion = ((separacion - SEPARACION_REPOSO) / 4f).coerceIn(0f, 1f)
                    if (aparicion > 0f) {
                        // Catmull-Rom: la cuerda se dibuja como una curva continua, no como una poligonal
                        val puntos = puntosLiga.also { it.clear() }
                        fun p(i: Int): Offset {
                            val k = i.coerceIn(0, n)
                            val t = k / n.toFloat()
                            return Offset(cuerda.x[k], cuerda.y[k]) + desfaseA * (1 - t) + desfaseB * t
                        }
                        for (i in 0 until n) {
                            val q0 = p(i - 1); val q1 = p(i); val q2 = p(i + 1); val q3 = p(i + 2)
                            for (k in 0 until 4) {
                                val t = k / 4f
                                val t2 = t * t
                                val t3 = t2 * t
                                puntos += (q1 * 2f + (q2 - q0) * t + (q0 * 2f - q1 * 5f + q2 * 4f - q3) * t2 + (q1 * 3f - q0 - q2 * 3f + q3) * t3) * 0.5f
                            }
                        }
                        puntos += p(n)
                        val ultimo = puntos.lastIndex
                        arcos[0] = 0f
                        for (i in 1..ultimo) arcos[i] = arcos[i - 1] + (puntos[i] - puntos[i - 1]).getDistance()
                        val total = arcos[ultimo].coerceAtLeast(0.001f)
                        // Los extremos cubren justo la cara cortada del brazo (su ancho es el de la cara); el centro se afina
                        // al estirarse (conserva su volumen), con un mínimo de 2 dp
                        val extremoA = caraA.mitad.getDistance() * 2f
                        val extremoB = caraB.mitad.getDistance() * 2f
                        val centro = (GROSOR_BRAZO * kotlin.math.sqrt(110f / max(separacion, 110f))).coerceAtLeast(2f) * dp
                        fun grosor(i: Int): Float {
                            val s = arcos[i] / total
                            return lerp(lerp(extremoA, extremoB, s), centro, sin(PI.toFloat() * s)) * aparicion
                        }
                        // Segmentos con puntas redondas: se unen sin huecos ni picos, también donde la liga se dobla
                        for (i in 0 until ultimo) {
                            drawLine(
                                ColorEslabon, puntos[i], puntos[i + 1],
                                strokeWidth = (grosor(i) + grosor(i + 1)) / 2f, cap = StrokeCap.Round
                            )
                        }
                    }
                }
            }

            // Íconos
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(ALTO_BARRA)
                    .align(Alignment.BottomCenter)
                    .selectableGroup()
            ) {
                destinos.forEachIndexed { i, destino ->
                    val insignia = when (destino.ruta) {
                        Rutas.Solicitudes.ruta -> pendientes
                        Rutas.Chat.ruta -> noLeidos
                        else -> 0
                    }
                    val descripcion = buildString {
                        append(destino.etiqueta)
                        if (insignia > 0) append(", $insignia ${if (destino.ruta == Rutas.Chat.ruta) "sin leer" else "pendientes"}")
                    }
                    Box(
                        contentAlignment = Alignment.Center,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                            .selectable(
                                selected = i == activo,
                                onClick = { alSeleccionar(destino.navegar) },
                                role = Role.Tab,
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null
                            )
                            .semantics { contentDescription = descripcion }
                    ) {
                        val p = iconos[i]
                        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(48.dp)) {
                            Icon(
                                painterResource(destino.icono),
                                contentDescription = null,
                                tint = Salvia,
                                modifier = Modifier
                                    .size(24.dp)
                                    .graphicsLayer {
                                        translationX = p.x * dp
                                        translationY = p.y * dp
                                        rotationZ = p.giro
                                        scaleX = p.escala * p.ex
                                        scaleY = p.escala * p.ey
                                        alpha = p.alfa
                                    }
                            )
                            if (insignia > 0) {
                                val g = globos[i]
                                InsigniaConteo(
                                    insignia,
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = (-2).dp, y = 4.dp)
                                        .graphicsLayer {
                                            alpha = g.floatValue
                                            scaleX = 0.4f + 0.6f * g.floatValue
                                            scaleY = scaleX
                                        }
                                )
                            }
                        }
                    }
                }
            }

            // Los dos eslabones del logo
            listOf(e1 to R.drawable.eslabon_1, e2 to R.drawable.eslabon_2).forEach { (p, imagen) ->
                Image(
                    painterResource(imagen),
                    contentDescription = null,
                    modifier = Modifier
                        .size(TAM_ESLABON)
                        .offset { IntOffset((p.x - tamEslabon / 2).finito().roundToInt(), (p.y - tamEslabon / 2).finito().roundToInt()) }
                        .graphicsLayer {
                            rotationZ = p.giro
                            scaleX = p.escala * p.ex
                            scaleY = p.escala * p.ey
                        }
                        // Estiramiento automático en la dirección del movimiento (suave en esta coreografía).
                        // Con la liga tendida no se deforman: la que se estira es la liga, y su unión debe calzar
                        .graphicsLayer {
                            val vel = hypot(velX.floatValue, velY.floatValue)
                            val s = if (liga.value) 0f else min(0.45f, vel / (2200f * dp)) * 0.4f
                            rotationZ = Math.toDegrees(atan2(velY.floatValue, velX.floatValue).toDouble()).toFloat()
                            scaleX = 1f + s
                            scaleY = 1f - s * 0.5f
                        }
                        .graphicsLayer {
                            rotationZ = -Math.toDegrees(atan2(velY.floatValue, velX.floatValue).toDouble()).toFloat()
                        }
                )
            }
        }
        Box(
            Modifier
                .fillMaxWidth()
                .background(Color.White)
                .windowInsetsBottomHeight(WindowInsets.navigationBars)
        )
    }
}

/**
 * "Liga elástica": los eslabones se aprietan → el delantero sale disparado y se ancla sobre el nuevo ícono,
 * mientras el otro se queda atrás estirando una liga entre los dos → el de atrás se suelta como una
 * resortera → golpea al nuevo ícono, que sale girando, y se cierran con un clic. El ícono anterior
 * queda libre con un salto.
 */
private suspend fun coreografiaLiga(
    e1: Pieza, e2: Pieza, iconos: List<Pieza>, globos: List<androidx.compose.runtime.MutableFloatState>,
    liga: androidx.compose.runtime.MutableState<Boolean>,
    previo: Int, nuevo: Int, b: Offset, dp: Float, ritmo: Float, alSoltar: () -> Unit, alCerrar: () -> Unit
) = coroutineScope {
    val a = Offset((e1.x + e2.x) / 2, (e1.y + e2.y) / 2)
    val dir = if (b.x >= a.x) 1f else -1f
    val lider = if (dir > 0) e2 else e1
    val ancla = if (dir > 0) e1 else e2
    val icono = iconos[nuevo]

    /** Corre [bloque] cuando hayan pasado [ms] (escalados por el ritmo) desde el inicio de la coreografía. */
    fun en(ms: Long, bloque: suspend () -> Unit) = launch { delay((ms * ritmo).toLong()); bloque() }
    suspend fun paso(ms: Int, curva: Easing, alAvanzar: (Float) -> Unit) = tramo((ms * ritmo).roundToInt(), curva, alAvanzar)

    // Los íconos que no participan vuelven a su sitio (por si una coreografía anterior quedó a medias).
    iconos.forEachIndexed { k, p ->
        if (k != nuevo && k != previo) launch {
            val s = floatArrayOf(p.x, p.y, p.giro, p.escala, p.ex, p.ey, p.alfa)
            paso(200, SalidaSuave) { t ->
                p.x = lerp(s[0], 0f, t); p.y = lerp(s[1], 0f, t); p.giro = lerp(s[2], 0f, t)
                p.escala = lerp(s[3], 1f, t); p.ex = lerp(s[4], 1f, t); p.ey = lerp(s[5], 1f, t); p.alfa = lerp(s[6], 1f, t)
            }
        }
    }
    globos.forEachIndexed { k, g -> if (k != nuevo && k != previo && g.floatValue < 1f) launch { val g0 = g.floatValue; paso(150, SalidaSuave) { g.floatValue = lerp(g0, 1f, it) } } }

    // 0 ms: los dos se aprietan
    val s0 = e1.escala
    launch { paso(70, EntradaSuave) { t -> e1.escala = lerp(s0, 0.9f, t); e2.escala = e1.escala } }
    icono.alfa = 1f

    // 70 ms: aparece la liga entre los dos; el delantero sale disparado y se ancla sobre el destino
    en(70) {
        liga.value = true
        val l0 = floatArrayOf(lider.x, lider.y, lider.giro)
        paso(260, SalidaFuerte) { t ->
            lider.x = lerp(l0[0], b.x + dir * 4 * dp, t); lider.y = lerp(l0[1], b.y - 2 * dp, t)
            lider.giro = lerp(l0[2], dir * 30f, t); lider.escala = lerp(0.9f, 1f, t)
        }
    }
    // 70 ms: el de atrás retrocede un poco, tensando la liga
    en(70) {
        val a0 = floatArrayOf(ancla.x, ancla.giro)
        paso(100, SalidaSuave) { t ->
            ancla.x = lerp(a0[0], a.x - dir * 6 * dp, t); ancla.giro = lerp(a0[1], -dir * 18f, t); ancla.escala = lerp(0.9f, 1f, t)
        }
    }
    // 200 ms: el nuevo ícono se prepara para el golpe
    en(200) { paso(180, SalidaSuave) { t -> icono.x = lerp(0f, -dir * 5f, t); icono.ex = lerp(1f, 0.85f, t) } }
    // 300 ms: la liga lo arrastra y se estira
    en(300) {
        val x0 = ancla.x
        paso(140, EntradaSuave) { t -> ancla.x = lerp(x0, a.x + dir * 4 * dp, t); ancla.ex = lerp(1f, 1.15f, t) }
    }
    // 440 ms: se suelta como resortera hacia el destino
    en(440) {
        alSoltar()
        val a1 = floatArrayOf(ancla.x, ancla.y, ancla.giro)
        paso(240, EntradaMuyFuerte) { t ->
            ancla.x = lerp(a1[0], b.x - dir * 3 * dp, t); ancla.y = lerp(a1[1], b.y, t)
            ancla.giro = lerp(a1[2], dir * 10f, t); ancla.ex = lerp(1.15f, 1f, t)
        }
    }
    // 450 ms: el ícono anterior queda libre con un salto
    if (previo in iconos.indices) en(450) {
        val p = iconos[previo]
        p.alfa = 0f; p.escala = 0.35f; p.y = 6f; p.giro = -dir * 14f; p.ex = 1f; p.ey = 1f; p.x = 0f
        launch { paso(160, SalidaSuave) { p.alfa = it } }
        launch { resorte(6f, 0f, 0.5f, 380f) { p.y = it } }
        launch { resorte(-dir * 14f, 0f, 0.3f, 300f) { p.giro = it } }
        resorte(0.35f, 1f, 0.5f, 380f) { p.escala = it }
    }
    if (previo in globos.indices) en(550) { resorte(0f, 1f, 0.5f, 500f) { globos[previo].floatValue = it.coerceIn(0f, 1.2f) } }
    // 640 ms: el golpe saca al nuevo ícono girando y lo esconde
    en(560) { val g = globos[nuevo]; val g0 = g.floatValue; paso(120, SalidaSuave) { g.floatValue = lerp(g0, 0f, it) } }
    en(640) {
        val i0 = floatArrayOf(icono.x, icono.ex)
        paso(120, SalidaFuerte) { t -> icono.x = lerp(i0[0], dir * 14f, t); icono.giro = lerp(0f, dir * 38f, t); icono.ex = lerp(i0[1], 1f, t) }
    }
    en(760) { paso(120, EntradaSuave) { t -> icono.alfa = 1 - t; icono.escala = lerp(1f, 0.4f, t) } }
    en(800) { icono.x = 0f; icono.giro = 0f; icono.escala = 1f; icono.ex = 0.4f; icono.ey = 0.4f }

    // 660 ms: cierre con clic
    delay((660 * ritmo).toLong())
    val c1 = floatArrayOf(e1.x, e1.y, e1.giro)
    val c2 = floatArrayOf(e2.x, e2.y, e2.giro)
    paso(120, EntradaFuerte) { t ->
        e1.x = lerp(c1[0], b.x, t); e1.y = lerp(c1[1], b.y, t); e1.giro = lerp(c1[2], dir * 5f, t)
        e2.x = lerp(c2[0], b.x, t); e2.y = lerp(c2[1], b.y, t); e2.giro = lerp(c2[2], -dir * 5f, t)
    }
    liga.value = false
    e1.x = b.x; e1.y = b.y; e2.x = b.x; e2.y = b.y
    alCerrar()
    launch { resorte(dir * 5f, 0f, 0.35f, 420f) { e1.giro = it; e2.giro = -it } }
    paso(80, SalidaSuave) { t -> e1.escala = lerp(1f, 1.12f, t); e2.escala = e1.escala }
    paso(80, SalidaSuave) { t -> e1.escala = lerp(1.12f, 1f, t); e2.escala = e1.escala }
}
