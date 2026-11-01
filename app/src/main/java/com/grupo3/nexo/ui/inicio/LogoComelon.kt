package com.grupo3.nexo.ui.inicio

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.grupo3.nexo.R
import com.grupo3.nexo.ui.theme.BosqueProfundo
import com.grupo3.nexo.ui.theme.EtiquetaMiga
import com.grupo3.nexo.ui.theme.LogoAzulPower
import com.grupo3.nexo.ui.theme.LogoMiga
import com.grupo3.nexo.ui.theme.LogoMigaSombra
import com.grupo3.nexo.ui.theme.TextoMigaClaro
import com.grupo3.nexo.ui.theme.TituloLogo
import com.grupo3.nexo.ui.theme.leerAnimacionesReducidas
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sin
import kotlin.random.Random

/*
 * "El logo se come un poder": al tocar el logo de la cabecera aparece un poder entre la insignia y el
 * nombre. La insignia se lo come con la boca en cuña, las letras se ponen azules, les sale la falda de
 * fantasma y huyen en línea recta. El logo se come la N y la e (sus fantasmas pálidos vuelven a casa y se
 * rearman). Al acabarse el poder la x y la o parpadean, se dan la vuelta y lo persiguen: el logo huye a su
 * sitio y se sacude del susto. El nombre vuelve completo por sí solo.
 *
 * La escena se calcula entera al tocar, como una tabla de cuadros a 120 por segundo (posiciones en píxeles
 * ya medidas sobre la cabecera real). Un único reloj recorre la tabla y las lecturas ocurren en las fases
 * de dibujo, así que la animación no recompone la pantalla en cada cuadro.
 */

/* ==================== Curvas ==================== */

private fun salidaCubica(u: Float) = 1 - (1 - u).pow(3)
private fun entradaSalidaCubica(u: Float) = if (u < .5f) 4 * u * u * u else 1 - (-2 * u + 2).pow(3) / 2
private fun retroceso(u: Float): Float { val s = 1.7f; val v = u - 1; return 1 + (s + 1) * v * v * v + s * v * v }
private fun temblor(t: Float, i: Int, f: Float) = sin(t * f + i * 7.3f) * cos(t * f * .61f + i * 2.1f)

/* ==================== Datos de la escena ==================== */

internal class Miga(val t0: Float, val tam: Float, val puntos: FloatArray, val giro: Float)
internal class Pop(val t0: Float, val texto: String, val x: Float, val y: Float)

private const val PASO = 1f / 120f

/* Disposición de cada cuadro de la tabla */
private const val LOGO = 0        // x, y (centro, en píxeles de pantalla), giro, escala X (con el signo del lado), escala Y, boca
private const val LETRAS = 6      // 4 letras × (x, y, giro, escala, opacidad, azul, falda)
private const val POR_LETRA = 7
private const val PODER = 34      // x, y, escala
private const val ANCHO_CUADRO = 37

/** Escena calculada: la tabla de cuadros más lo que se ve fuera de las letras y la insignia. */
internal class Linea(val cuadros: Int, val datos: FloatArray) {
    val final: Float get() = cuadros * PASO
    var anilloT = -1f
    @Volatile var migas: List<Miga> = emptyList()
    /** Migas por simular: se calculan en segundo plano para no frenar el primer cuadro. */
    val porSimular = mutableListOf<() -> Miga>()
    val pops = mutableListOf<Pop>()
    /** Instantes de cada bocado: ahí vibra el teléfono. */
    val golpes = mutableListOf<Float>()
    fun base(t: Float) = (t / PASO).toInt().coerceIn(0, cuadros - 1) * ANCHO_CUADRO
}

/* ==================== Geometría ==================== */

/** Posiciones medidas en pantalla, relativas a la cabecera completa. */
class GeometriaComelon {
    internal var cabecera = Offset.Zero
    internal var altoCabecera = 0f
    internal var anchoPantalla = 0f
    internal var logo = Rect.Zero
    internal val letras = Array(4) { Rect.Zero }
    internal val saludo = mutableListOf<Pair<Offset, TextLayoutResult>>()
    internal fun rel(c: LayoutCoordinates): Rect {
        val p = c.positionInRoot()
        return Rect(p, Size(c.size.width.toFloat(), c.size.height.toFloat()))
    }
}

/** Estado compartido entre la marca (logo + letras) y la capa de migas de la cabecera. */
class EstadoComelon internal constructor() {
    internal val geo = GeometriaComelon()
    internal val reloj = Animatable(0f)
    internal var linea by mutableStateOf<Linea?>(null)
    internal var animando by mutableStateOf(false)
    /** Se marca al tocar, antes de que arranque el reloj, para ignorar un segundo toque. */
    internal var enCurso = false
    internal val ahora: Float get() = reloj.value
}

@Composable
fun rememberEstadoComelon(): EstadoComelon = remember { EstadoComelon() }

/** Registra la cabecera completa (la referencia de coordenadas y el piso de las migas). */
fun Modifier.cabeceraComelon(e: EstadoComelon): Modifier = onGloballyPositioned {
    e.geo.cabecera = it.positionInRoot(); e.geo.altoCabecera = it.size.height.toFloat()
}

/** Registra un texto del saludo para que las migas reboten sobre sus letras. */
fun saludoComelon(e: EstadoComelon, indice: Int): Pair<Modifier, (TextLayoutResult) -> Unit> {
    var posicion = Offset.Zero
    var layout: TextLayoutResult? = null
    fun guarda() { layout?.let { l -> while (e.geo.saludo.size <= indice) e.geo.saludo += Offset.Zero to l; e.geo.saludo[indice] = posicion to l } }
    return Modifier.onGloballyPositioned { posicion = it.positionInRoot(); guarda() } to { l: TextLayoutResult -> layout = l; guarda() }
}

/* ==================== Simulación de la escena ==================== */

private val PUNTOS = listOf("200", "400")

private enum class Modo { N, M, B, CAZA, ECO, REARMA }   // reposo, huye azul, parpadea, persigue, fantasma que vuelve, se rearma
private enum class Fase { INICIO, CAZA, AVISO, HUIDA, FIN }

private class SimLetra(val i: Int, val hx: Float, val hy: Float, val w: Float, val h: Float) {
    var x = hx; var y = hy; var giro = 0f; var esc = 1f; var alfa = 1f
    var modo = Modo.N
    var comida = -9f; var ox = 0f; var oy = 0f; var dur = -1f; var rearma = 0f; var susto = 0f
}

private class Golpe(val t: Float, val x: Float, val y: Float, val w: Float, val h: Float)

/**
 * La escena en unidades de dp (1 dp = 1 unidad) y a 120 cuadros por segundo. Cada paso mueve a todos
 * los personajes un instante; al final de cada paso se escribe el cuadro en píxeles.
 */
private class Sim(val geo: GeometriaComelon, val d: Float) {
    val casa = geo.logo.center.x / d
    val cy = geo.logo.center.y / d
    val letras = List(4) { i -> val r = geo.letras[i]; SimLetra(i, r.center.x / d, r.center.y / d, r.width / d, r.height / d) }
    /** Hasta dónde huyen: se detienen antes de la campana de la cabecera. */
    val limite = geo.anchoPantalla / d - 125f

    var lx = casa; var ly = cy; var lf = 1f; var lgiro = 0f; var lp = 0f; var lph = 0f; var lesc = 1f
    var mueve = false; var bocado = -9f; var sacude = -9f
    var poderEscala = 0f; var anillo = -1f; var pausa = -9f
    var fase = Fase.INICIO; var aviso = 0f; var fin = 0f; var hecho = false
    val comidas = mutableListOf<Golpe>()
    val golpes = mutableListOf<Float>()

    fun xMax(i: Int) = max(limite - (3 - i) * 24f, letras[i].hx + 24f)

    fun paso(t: Float) {
        if (t < .44f) preludio(t)
        if (t >= .44f && fase == Fase.INICIO) fase = Fase.CAZA
        for (l in letras) {
            if ((l.modo == Modo.M || l.modo == Modo.B) && fase != Fase.INICIO) huir(l, t, .44f + (3 - l.i) * .04f, 105f, xMax(l.i))
        }
        if (fase == Fase.CAZA && t >= .55f && !enPausa(t)) {
            val obj = letras.firstOrNull { it.modo == Modo.M }
            if (obj != null && obj.i < 2) {
                if (mueveLogo(obj.x - 13f, obj.y, 165f) || hypot(lx - obj.x, ly - obj.y) < 15f) come(obj, t)
            } else { fase = Fase.AVISO; aviso = t }
        }
        if (fase == Fase.AVISO) {
            // Se acaba el poder: el logo se frena y las dos letras que quedan parpadean
            val x = letras[2]
            if (hypot(lx - (x.x - 34f), ly - x.y) > 2f) mueveLogo(x.x - 34f, cy, 55f) else mueve = false
            for (l in letras) if (l.i >= 2) l.modo = Modo.B
            if (t - aviso > .55f) { fase = Fase.HUIDA; for (l in letras) if (l.i >= 2) l.modo = Modo.CAZA }
        }
        if (fase == Fase.HUIDA) {
            for (l in letras) {
                if (l.modo != Modo.CAZA) continue
                l.giro = 3 * sin(t * 9 + l.i)
                val dx = max(l.hx, lx + 30f) - l.x
                l.x += dx.coerceIn(-200f * PASO, 200f * PASO)
                l.y += (l.hy - l.y) * min(1f, PASO * 6)
                if (lx <= casa + .5f && abs(l.x - l.hx) < .6f) { l.x = l.hx; l.y = l.hy; l.giro = 0f; l.modo = Modo.N }
            }
            if (lx > casa + .3f) mueveLogo(casa, cy, 230f)
            else if (sacude < 0) { lx = casa; lf = 1f; mueve = false; sacude = t; bocado = t }
            if (letras.all { it.modo == Modo.N } && sacude > 0) { fase = Fase.FIN; fin = t }
        }
        animaFantasmas(t)
        if (sacude > 0) {
            val e = t - sacude
            lgiro = if (e < .45f) 11 * sin(e * 45) * (1 - e / .45f) else 0f
        }
        if (fase == Fase.FIN && t - fin > .3f) hecho = true
        if (fase != Fase.HUIDA || sacude < 0) mastica() else lp = max(0f, lp - PASO * 7)
    }

    /** Aparece el poder entre el logo y la N, el logo se lo come y las letras se asustan. */
    fun preludio(t: Float) {
        poderEscala = if (t < .21f) retroceso((t / .09f).coerceIn(0f, 1f)) * (1 + .12f * sin(t * 34)) else 0f
        if (t >= .06f && t < .21f) { lx = casa + 6f * entradaSalidaCubica((t - .06f) / .15f); mueve = true }
        else if (t >= .21f && anillo < 0) { anillo = .21f; bocado = .21f; mueve = false; golpes += .21f }
        lesc = if (t >= .21f && t < .41f) 1 + .14f * sin(PI.toFloat() * (t - .21f) / .20f) else 1f
        for (l in letras) {
            if (t >= .24f + l.i * .03f && l.modo == Modo.N) { l.modo = Modo.M; l.susto = t }
            if (l.modo == Modo.M && t < .44f) {
                l.x = l.hx + 4f * salidaCubica(((t - l.susto) / .09f).coerceIn(0f, 1f)) + 1.2f * temblor(t, l.i, 70f)
                l.giro = 6f * temblor(t, l.i, 55f)
            }
        }
    }

    /** Huye en línea recta (con apenas un vaivén) hasta su tope. */
    fun huir(l: SimLetra, t: Float, t0: Float, vx: Float, xmax: Float) {
        if (t < t0) return
        val dt0 = t - t0
        if (l.x < xmax) l.x = min(xmax, l.x + vx * PASO * (dt0 / .12f).coerceIn(0f, 1f))
        l.y = l.hy + 1.8f * sin(2 * PI.toFloat() * 1.4f * dt0 + l.i * 1.3f)
        l.giro = if (l.x >= xmax) 7f * temblor(t, l.i, 60f) else 4f * sin(t * 7 + l.i)
        if (l.x >= xmax) l.x = xmax + temblor(t, l.i, 80f)
    }

    fun enPausa(t: Float): Boolean { if (t < pausa) { mueve = false; return true }; return false }

    fun mueveLogo(tx: Float, ty: Float, vel: Float): Boolean {
        val dx = tx - lx; val dy = ty - ly; val dist = hypot(dx, dy)
        if (dist < .01f) { mueve = false; return true }
        val paso = min(dist, vel * PASO)
        lx += dx / dist * paso; ly += dy / dist * paso; mueve = true
        if (abs(dx) > .5f) lf = if (dx >= 0) 1f else -1f
        val ang = (atan2(dy, abs(dx)) * 180f / PI.toFloat() * lf).coerceIn(-40f, 40f)
        lgiro += (ang - lgiro) * min(1f, PASO * 14)
        return dist <= paso + .01f
    }

    /** Abre y cierra la boca mientras camina; quieto, la cierra. */
    fun mastica() {
        if (mueve) { lph += PASO * 2 * PI.toFloat() * 4.6f; lp = .5f - .5f * cos(lph) }
        else { lp = max(0f, lp - PASO * 7); if (lp == 0f) lph = 0f }
        if (!mueve && fase != Fase.INICIO) lgiro += (0f - lgiro) * min(1f, PASO * 10)
    }

    /** El logo se come la letra: queda su fantasma, que vuelve a casa. */
    fun come(l: SimLetra, t: Float) {
        l.modo = Modo.ECO; l.comida = t; l.ox = l.x; l.oy = l.y
        comidas += Golpe(t, l.x * d, l.y * d, l.w * d, l.h * d)
        golpes += t; bocado = t; pausa = t + .12f
    }

    /** El fantasma pálido y pequeño vuelve flotando a su sitio y ahí se rearma. */
    fun animaFantasmas(t: Float) {
        for (l in letras) {
            val e = t - l.comida
            if (l.modo == Modo.ECO) {
                if (e < .12f) { l.alfa = 1f; l.esc = 1f; continue }
                if (l.dur < 0) l.dur = max(.25f, hypot(l.ox - l.hx, l.oy - l.hy) / 250f)
                val u = ((e - .12f) / l.dur).coerceIn(0f, 1f)
                l.x = l.ox + (l.hx - l.ox) * entradaSalidaCubica(u)
                l.y = l.oy + (l.hy - l.oy) * u - 12f * sin(PI.toFloat() * u)
                l.giro = 0f; l.alfa = .42f; l.esc = .78f
                if (u >= 1f) { l.modo = Modo.REARMA; l.rearma = t; l.dur = -1f }
            }
            if (l.modo == Modo.REARMA) {
                val r = ((t - l.rearma) / .32f).coerceIn(0f, 1f)
                l.esc = .78f + .22f * retroceso(r); l.alfa = .42f + .58f * r; l.x = l.hx; l.y = l.hy
                if (r >= 1f) { l.modo = Modo.N; l.esc = 1f; l.alfa = 1f }
            }
        }
    }

    /** Escribe el cuadro `n` (instante `t`) en la tabla, ya en píxeles. */
    fun escribe(datos: FloatArray, n: Int, t: Float) {
        val b = n * ANCHO_CUADRO
        val e = t - bocado
        val k = if (e >= 0f && e < .14f) sin(PI.toFloat() * e / .14f) else 0f
        datos[b + LOGO] = lx * d; datos[b + LOGO + 1] = ly * d; datos[b + LOGO + 2] = lgiro
        datos[b + LOGO + 3] = lf * lesc * (1 + .06f * k); datos[b + LOGO + 4] = lesc * (1 - .08f * k); datos[b + LOGO + 5] = lp
        for (l in letras) {
            val o = b + LETRAS + l.i * POR_LETRA
            val azul = when (l.modo) { Modo.M -> 1f; Modo.B -> if (floor(t * 8).toInt() % 2 == 1) 0f else 1f; else -> 0f }
            val falda = if (l.modo == Modo.M || l.modo == Modo.B || l.modo == Modo.CAZA || l.modo == Modo.ECO) 1f else 0f
            datos[o] = l.x * d; datos[o + 1] = l.y * d; datos[o + 2] = l.giro; datos[o + 3] = l.esc; datos[o + 4] = l.alfa
            datos[o + 5] = azul; datos[o + 6] = falda
        }
        datos[b + PODER] = (casa + 23f) * d; datos[b + PODER + 1] = cy * d; datos[b + PODER + 2] = poderEscala
    }
}

private fun simulaMiga(x0: Float, y0: Float, vx0: Float, vy0: Float, sup: List<FloatArray>, piso: Float, d: Float): FloatArray {
    // sup: [izq, der, y] por letra del saludo. Unidades en px; g y velocidades escaladas por densidad.
    val pts = FloatArray(240 * 2); val dt = 1f / 120; val g = 900f * d
    var x = x0; var y = y0; var vx = vx0; var vy = vy0; var estado = 0; var s: FloatArray? = null
    for (k in 0 until 240) {
        when (estado) {
            0 -> {
                vy += g * dt; val ny = y + vy * dt; val nx = x + vx * dt
                var hit: FloatArray? = null
                if (vy > 0) for (q in sup) if (nx >= q[0] && nx <= q[1] && y <= q[2] && ny >= q[2]) if (hit == null || q[2] < hit[2]) hit = q
                if (hit != null) {
                    y = hit[2]; x = nx
                    if (vy > 160 * d) vy = -vy * .22f else { estado = 1; s = hit; vx = (if (vx >= 0) 1 else -1) * maxOf(abs(vx) * .6f, 40f * d); vy = 0f }
                } else if (ny >= piso) { y = piso; x = nx; if (vy > 160 * d) vy = -vy * .25f else estado = 2 }
                else { x = nx; y = ny }
            }
            1 -> { vx *= (1 - 1.2f * dt); x += vx * dt; val q = s!!; if (x < q[0] || x > q[1]) { estado = 0; vy = 10f * d } }
        }
        pts[k * 2] = x; pts[k * 2 + 1] = y
    }
    return pts
}

private fun superficies(geo: GeometriaComelon): List<FloatArray> {
    val altas = "ABCDEFGHIJKLMNOPQRSTUVWXYZÁÉÍÓÚÑbdfhklt¿?"
    val out = mutableListOf<FloatArray>()
    for ((pos, l) in geo.saludo) {
        val txt = l.layoutInput.text.text
        for (i in txt.indices) {
            val ch = txt[i]; if (ch.isWhitespace() || ch == ',' || ch == '.') continue
            val b = l.getBoundingBox(i); if (b.width <= 0f) continue
            val top = pos.y + b.top + b.height * (if (altas.contains(ch)) .2f else .42f)
            out += floatArrayOf(pos.x + b.left + .5f, pos.x + b.right - .5f, top)
        }
    }
    return out
}

private fun construirEscena(geo: GeometriaComelon, d: Float, rnd: Random): Linea {
    val sim = Sim(geo, d)
    val maximo = 120 * 7
    val datos = FloatArray(maximo * ANCHO_CUADRO)
    var n = 0
    while (n < maximo && !sim.hecho) {
        val t = n * PASO
        sim.paso(t)
        sim.escribe(datos, n, t)
        n++
    }
    val ln = Linea(n, datos.copyOf(n * ANCHO_CUADRO))
    ln.anilloT = sim.anillo
    ln.golpes += sim.golpes.sorted()

    val sup = superficies(geo)
    val piso = geo.cabecera.y + geo.altoCabecera - 36 * d
    fun r(a: Float, b: Float) = a + rnd.nextFloat() * (b - a)
    sim.comidas.forEachIndexed { k, g ->
        ln.pops += Pop(g.t, PUNTOS[k.coerceAtMost(PUNTOS.lastIndex)], g.x - g.w / 2, g.y - g.h / 2)
        repeat(3) {
            val tam = r(2f, 3.4f) * d
            val x0 = g.x - 12 * d + r(-3f, 3f) * d; val y0 = g.y + r(-3f, 3f) * d
            val vx = r(-45f, 35f) * d; val vy = r(-70f, -20f) * d; val giro = r(0f, 90f)
            ln.porSimular += { Miga(g.t + .06f, tam, simulaMiga(x0, y0, vx, vy, sup, piso - tam, d), giro) }
        }
    }
    return ln
}

/** Con las animaciones reducidas, las letras solo se atenúan y vuelven: sin movimiento. */
private fun construirReducida(geo: GeometriaComelon): Linea {
    val n = (.4f / PASO).toInt()
    val datos = FloatArray(n * ANCHO_CUADRO)
    for (k in 0 until n) {
        val b = k * ANCHO_CUADRO
        datos[b + LOGO] = geo.logo.center.x; datos[b + LOGO + 1] = geo.logo.center.y
        datos[b + LOGO + 3] = 1f; datos[b + LOGO + 4] = 1f
        for (i in 0 until 4) {
            val o = b + LETRAS + i * POR_LETRA
            datos[o] = geo.letras[i].center.x; datos[o + 1] = geo.letras[i].center.y; datos[o + 3] = 1f
            datos[o + 4] = 1f - .75f * sin(PI.toFloat() * k / (n - 1))
        }
    }
    return Linea(n, datos)
}

/* ==================== Forma de la insignia con boca ==================== */

/**
 * La insignia en píxeles con la boca abierta a cada paso (a 1/32), cacheada. El cuerpo es el cuadrado
 * redondeado de 36 dp aplanado en segmentos para que `cornerPathEffect` suavice los labios; la boca es una
 * cuña de punta redonda que se le resta.
 */
private class FormasBoca(private val d: Float) {
    private val cuerpo = cuerpoRedondeado()
    private val trazos = arrayOfNulls<Path>(33)
    private val formas = arrayOfNulls<Shape>(33)
    val radioLabio = 1.6f * d

    private fun cuerpoRedondeado(): Path {
        val lado = 36f * d; val r = 11f * d; val tramos = 10
        val p = Path()
        fun arco(cx: Float, cy: Float, desde: Float) {
            for (k in 0..tramos) {
                val a = (desde + 90f * k / tramos) * PI.toFloat() / 180f
                p.lineTo(cx + r * cos(a), cy + r * sin(a))
            }
        }
        p.moveTo(r, 0f)
        arco(lado - r, r, -90f); arco(lado - r, lado - r, 0f); arco(r, lado - r, 90f); arco(r, r, 180f)
        p.close()
        return p
    }

    private fun cuna(p: Float): Path {
        val a = 38f * p; val ar = a * PI.toFloat() / 180f
        val g = 1.1f * d * min(1f, p * 8f)
        val sa = sin(ar); val ca = cos(ar)
        val hx = 23f * d; val hy = 18f * d; val largo = 40f * d
        val x1 = hx - g * sa; val y1 = hy - g * ca; val y2 = hy + g * ca
        return Path().apply {
            moveTo(x1, y1); lineTo(x1 + largo * ca, y1 - largo * sa)
            lineTo(x1 + largo * ca, y2 + largo * sa); lineTo(x1, y2)
            arcTo(Rect(hx - g, hy - g, hx + g, hy + g), 90f + a, 180f - 2 * a, false)
            close()
        }
    }

    private fun indice(p: Float) = (p.coerceIn(0f, 1f) * 32f).roundToInt()

    fun trazo(p: Float): Path {
        val q = indice(p)
        return trazos[q] ?: (if (q == 0) cuerpo else Path.combine(PathOperation.Difference, cuerpo, cuna(q / 32f))).also { trazos[q] = it }
    }

    /** La misma silueta como forma, para que la sombra siga el recorte de la boca. */
    fun forma(p: Float): Shape {
        val q = indice(p)
        return formas[q] ?: object : Shape {
            val contorno = trazo(p)
            override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density) = Outline.Generic(contorno)
        }.also { formas[q] = it }
    }
}

/* ==================== Marca: logo + letras ==================== */

@Composable
fun MarcaComelon(e: EstadoComelon, modifier: Modifier = Modifier) {
    val reducidas = leerAnimacionesReducidas()
    val densidad = LocalDensity.current.density
    e.geo.anchoPantalla = LocalConfiguration.current.screenWidthDp * densidad
    val alcance = rememberCoroutineScope()
    val haptica = LocalHapticFeedback.current
    val forma = RoundedCornerShape(11.dp)
    val estilo = TituloLogo.copy(color = Color.White)

    fun jugar() {
        if (e.enCurso) return
        e.enCurso = true
        haptica.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        val ln = if (reducidas) construirReducida(e.geo) else construirEscena(e.geo, densidad, Random.Default)
        if (ln.porSimular.isNotEmpty()) alcance.launch(Dispatchers.Default) {
            ln.migas = ln.porSimular.map { it() }.sortedBy { it.t0 }
        }
        alcance.launch {
            // El reloj vuelve a cero antes de enseñar la escena nueva: sin un cuadro de la anterior
            e.reloj.snapTo(0f)
            e.linea = ln; e.animando = true
            // La vibración sale del mismo reloj que la animación: nunca se descuadra del bocado
            val vibracion = launch {
                var siguiente = 0
                snapshotFlow { e.reloj.value }.collect { t ->
                    while (siguiente < ln.golpes.size && t >= ln.golpes[siguiente]) {
                        haptica.performHapticFeedback(HapticFeedbackType.TextHandleMove); siguiente++
                    }
                }
            }
            e.reloj.animateTo(ln.final, tween((ln.final * 1000).toInt(), easing = LinearEasing))
            vibracion.cancel()
            e.animando = false; e.enCurso = false
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clickable(
            interactionSource = remember { MutableInteractionSource() },
            indication = null,
            role = Role.Button,
            onClickLabel = "Jugar con el logo"
        ) { jugar() }
    ) {
        Box(
            Modifier
                .zIndex(1f)
                .size(36.dp)
                .onGloballyPositioned { e.geo.logo = e.geo.rel(it) }
        ) {
            // Todas las piezas quedan compuestas siempre (solo cambia su opacidad): así el primer toque no
            // tiene que cargarlas y la animación arranca sin tirones.
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(36.dp)
                    .graphicsLayer { alpha = if (e.animando) 0f else 1f }
                    .shadow(8.dp, forma, ambientColor = BosqueProfundo.copy(alpha = .3f), spotColor = BosqueProfundo.copy(alpha = .45f))
                    .background(Color.White, forma)
            ) { Image(painterResource(R.drawable.logo_nexo), contentDescription = "Nexo", modifier = Modifier.size(24.dp)) }
            InsigniaConBoca(e, densidad)
        }
        Spacer(Modifier.width(10.dp))
        "Nexo".forEachIndexed { i, ch -> LetraComelon(e, i, ch.toString(), estilo, densidad) }
        Spacer(Modifier.width(6.dp))
    }
}

/** La insignia en movimiento: el cuadrado blanco con una cuña por boca, sin lengua ni garganta. */
@Composable
private fun InsigniaConBoca(e: EstadoComelon, d: Float) {
    val formas = remember(d) { FormasBoca(d) }
    val pincel = remember(d) {
        Paint().apply { color = Color.White; isAntiAlias = true; pathEffect = PathEffect.cornerPathEffect(formas.radioLabio) }
    }
    Box(
        Modifier
            .size(36.dp)
            .graphicsLayer {
                val ln = e.linea
                val activo = ln != null && e.animando
                // Compuesta siempre; sin escena casi transparente
                alpha = if (activo) 1f else .004f
                if (activo && ln != null) {
                    val b = ln.base(e.ahora); val c = e.geo.logo.center
                    translationX = ln.datos[b + LOGO] - c.x; translationY = ln.datos[b + LOGO + 1] - c.y
                    rotationZ = ln.datos[b + LOGO + 2]
                    scaleX = ln.datos[b + LOGO + 3]; scaleY = ln.datos[b + LOGO + 4]
                    shape = formas.forma(ln.datos[b + LOGO + 5])
                } else shape = formas.forma(0f)
                // La sombra sigue el recorte de la boca
                shadowElevation = 8.dp.toPx()
                ambientShadowColor = BosqueProfundo.copy(alpha = .3f); spotShadowColor = BosqueProfundo.copy(alpha = .45f)
                clip = false
            }
    ) {
        Canvas(Modifier.fillMaxSize()) {
            val ln = e.linea
            val p = if (ln != null && e.animando) ln.datos[ln.base(e.ahora) + LOGO + 5] else 0f
            drawIntoCanvas { it.drawPath(formas.trazo(p), pincel) }
        }
        // El eslabón se achica un poco y se corre hacia atrás al abrir, para que la cuña no lo corte
        Image(
            painterResource(R.drawable.logo_nexo), contentDescription = null,
            modifier = Modifier.size(24.dp).align(Alignment.Center).graphicsLayer {
                val ln = e.linea
                val p = if (ln != null && e.animando) ln.datos[ln.base(e.ahora) + LOGO + 5] else 0f
                translationX = -6.dp.toPx() * p
                scaleX = 1f - .22f * p; scaleY = 1f - .22f * p
            }
        )
    }
}

/** Contorno de la falda de fantasma: borde inferior ondulado que se mueve con el tiempo. */
private fun faldaOndulada(ancho: Float, alto: Float, d: Float, t: Float): Path {
    val base = alto * .5f + 5.5f * d
    val p = Path()
    p.moveTo(-4f * d, -4f * d); p.lineTo(ancho + 4f * d, -4f * d)
    val tramos = 10
    for (k in 0..tramos) {
        val x = (ancho + 3f * d) - (ancho + 6f * d) * k / tramos
        p.lineTo(x, base - 1.6f * d + 2f * d * sin(x / d * 1.1f + t * 16f))
    }
    p.close()
    return p
}

@Composable
private fun LetraComelon(e: EstadoComelon, i: Int, texto: String, estilo: TextStyle, d: Float) {
    Text(
        texto,
        style = estilo,
        maxLines = 1,
        modifier = Modifier
            .onGloballyPositioned { e.geo.letras[i] = e.geo.rel(it) }
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                val ln = e.linea
                if (ln == null || !e.animando) { alpha = 1f; return@graphicsLayer }
                val o = ln.base(e.ahora) + LETRAS + i * POR_LETRA; val c = e.geo.letras[i].center
                translationX = ln.datos[o] - c.x; translationY = ln.datos[o + 1] - c.y
                rotationZ = ln.datos[o + 2]
                scaleX = ln.datos[o + 3]; scaleY = ln.datos[o + 3]
                alpha = ln.datos[o + 4]
            }
            .drawWithContent {
                val ln = e.linea
                if (ln == null || !e.animando) {
                    drawContent()
                    // Precalentado: operaciones imperceptibles para que el primer toque no compile nada nuevo
                    drawCircle(Color.Black, 1f, Offset(-4f, -4f), blendMode = BlendMode.Clear)
                    drawRect(LogoAzulPower.copy(alpha = .003f), size = Size(1f, 1f), blendMode = BlendMode.SrcAtop)
                    return@drawWithContent
                }
                val t = e.ahora; val o = ln.base(t) + LETRAS + i * POR_LETRA
                // Falda de fantasma: recorta el borde de abajo de la letra en una onda
                if (ln.datos[o + 6] > .5f) clipPath(faldaOndulada(size.width, size.height, d, t)) { this@drawWithContent.drawContent() }
                else drawContent()
                // Tinte azul del poder
                val az = ln.datos[o + 5]
                if (az > 0f) drawRect(LogoAzulPower.copy(alpha = az), blendMode = BlendMode.SrcAtop)
            }
    )
}

/* ==================== Capa de poder, migas y puntos ==================== */

@Composable
fun CapaMigasComelon(e: EstadoComelon, modifier: Modifier = Modifier) {
    val medidor = rememberTextMeasurer()
    val estiloPop = EtiquetaMiga.copy(color = TextoMigaClaro)
    remember(medidor) { PUNTOS.forEach { medidor.measure(it, estiloPop) } }
    Canvas(modifier) {
        val ln = e.linea ?: return@Canvas
        if (!e.animando) return@Canvas
        val t = e.ahora
        val o = e.geo.cabecera
        val b = ln.base(t)
        translate(-o.x, -o.y) {
            // El poder: una bolita menta con halo, entre la insignia y el nombre
            val pe = ln.datos[b + PODER + 2]
            if (pe > .01f) {
                val c = Offset(ln.datos[b + PODER], ln.datos[b + PODER + 1])
                drawCircle(TextoMigaClaro.copy(alpha = .22f), 7.dp.toPx() * pe, c)
                drawCircle(TextoMigaClaro, 3.6.dp.toPx() * pe, c)
            }
            // Onda al comérselo
            val ea = t - ln.anilloT
            if (ln.anilloT >= 0f && ea >= 0f && ea < .35f) {
                val u = ea / .35f
                drawCircle(
                    TextoMigaClaro.copy(alpha = .8f * (1 - u)), (18 + 20 * u).dp.toPx(),
                    Offset(ln.datos[b + LOGO], ln.datos[b + LOGO + 1]), style = Stroke(1.6.dp.toPx())
                )
            }
            for (m in ln.migas) {
                if (t < m.t0) continue
                val vida = (t - m.t0) / 2f; if (vida > 1f) continue
                val k = minOf(239, (vida * 239).toInt())
                val x = m.puntos[k * 2]; val y = m.puntos[k * 2 + 1]
                val a = if (vida > .85f) (1 - vida) / .15f else 1f
                // Miga de galleta: esquinas suaves y un borde tostado para que tenga volumen, no un píxel cuadrado
                val esquina = androidx.compose.ui.geometry.CornerRadius(m.tam * .35f)
                val arriba = Offset(x - m.tam / 2, y - m.tam); val lado = Size(m.tam, m.tam)
                drawRoundRect(LogoMigaSombra.copy(alpha = a), arriba + Offset(0f, m.tam * .18f), lado, esquina)
                drawRoundRect(LogoMiga.copy(alpha = a), arriba, lado, esquina)
            }
            for ((n, p) in ln.pops.withIndex()) {
                // Vida corta y alturas alternas: el siguiente puntaje nunca cae encima del anterior
                val dt = t - p.t0; if (dt < 0f || dt > .5f) continue
                val sube = if (dt < .18f) salidaCubica(dt / .18f) else 1f
                val a = if (dt < .32f) sube else 1 - (dt - .32f) / .18f
                val y = p.y - 6.dp.toPx() - (if (n % 2 == 0) 10 else 20).dp.toPx() * sube - (if (dt > .32f) 6.dp.toPx() * ((dt - .32f) / .18f) else 0f)
                drawText(medidor, p.texto, Offset(p.x, y), estiloPop.copy(color = estiloPop.color.copy(alpha = a.coerceIn(0f, 1f))))
            }
        }
    }
}
