package com.grupo3.nexo.ui.inicio

import android.provider.Settings
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.compose.ui.unit.sp
import com.grupo3.nexo.R
import com.grupo3.nexo.ui.theme.BosqueProfundo
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

/*
 * "El logo se come el nombre": al tocar el logo de la cabecera, la insignia se abre como Pac-Man, da un
 * mordisco al aire, las letras de "Nexo" se ponen azules y tiemblan, y se las come una a una a mordiscos
 * (tres bocados a la N y a la o, dos a la e y la x). Las migas caen sobre el saludo y resbalan por sus
 * letras. Luego sale por la derecha y reaparece por la izquierda. Al tocarlo otra vez, escupe las letras.
 *
 * Todo se calcula con una sola línea de tiempo (como la maqueta en GSAP): cada propiedad es una "pista"
 * con tramos, y un único reloj recorre la línea; las lecturas ocurren en las fases de dibujo, así que
 * la animación no recompone la pantalla en cada cuadro.
 */

/* ==================== Curvas ==================== */

internal typealias Curva = (Float) -> Float

private val lineal: Curva = { it }
private val p1In: Curva = { it * it }
private val p2In: Curva = { it * it * it }
private val p2Out: Curva = { 1 - (1 - it).pow(3) }
private val p3Out: Curva = { 1 - (1 - it).pow(4) }
private val p4In: Curva = { it.pow(5) }
private val senoInOut: Curva = { -(cos(PI.toFloat() * it) - 1) / 2 }
private fun backOut(s: Float = 1.7f): Curva = { t -> val u = t - 1; 1 + (s + 1) * u * u * u + s * u * u }
private fun elasticOut(a: Float = 1f, p: Float = .4f): Curva = { t ->
    if (t == 0f || t == 1f) t else {
        val s = p / (2 * PI.toFloat()) * asin(1 / a)
        a * 2f.pow(-10 * t) * sin((t - s) * 2 * PI.toFloat() / p) + 1
    }
}

/* ==================== Motor de pistas ==================== */

internal class Tramo(val t0: Float, val dur: Float, val desde: Float, val hasta: Float, val curva: Curva)

internal class Pista(private val base: Float) {
    private val tramos = mutableListOf<Tramo>()
    fun valor(t: Float): Float {
        var v = base
        for (s in tramos) {
            if (t < s.t0) break
            val p = if (s.dur <= 0f) 1f else ((t - s.t0) / s.dur).coerceIn(0f, 1f)
            v = s.desde + (s.hasta - s.desde) * s.curva(p)
        }
        return v
    }
    fun a(t0: Float, dur: Float, hasta: Float, curva: Curva = p2Out): Pista {
        tramos += Tramo(t0, dur, valor(t0), hasta, curva); tramos.sortBy { it.t0 }; return this
    }
    fun mas(t0: Float, dur: Float, delta: Float, curva: Curva = p2Out) = a(t0, dur, valor(t0) + delta, curva)
    fun fija(t0: Float, v: Float) = a(t0, 0f, v, lineal)
    fun ida(t0: Float, dur: Float, hasta: Float, curva: Curva = senoInOut): Pista {
        val v0 = valor(t0); a(t0, dur, hasta, curva); return a(t0 + dur, dur, v0, curva)
    }
}

internal class Letra {
    val x = Pista(0f); val y = Pista(0f); val giro = Pista(0f); val ex = Pista(1f); val ey = Pista(1f)
    val esc = Pista(1f); val alfa = Pista(1f); val azul = Pista(0f)
    val mordida = Array(3) { Pista(0f) }
    var vuelo: Vuelo? = null
}

/** Trayecto curvo de una letra al ser escupida (de la boca a su sitio). */
internal class Vuelo(val t0: Float, val dur: Float, val desde: Offset, val control: Offset, val curva: Curva)

internal class Miga(val t0: Float, val tam: Float, val puntos: FloatArray, val giro: Float, val galleta: Boolean = true)
internal class Pop(val t0: Float, val texto: String, val x: Float, val y: Float)

internal class Linea {
    val logoX = Pista(0f); val logoY = Pista(0f); val logoEx = Pista(1f); val logoEy = Pista(1f)
    val logoEsc = Pista(1f); val logoGiro = Pista(0f); val sacudon = Pista(0f); val abre = Pista(0f)
    val letras = List(4) { Letra() }
    @Volatile var migas: List<Miga> = emptyList()
    /** Migas por simular: se calculan en segundo plano para no frenar el primer cuadro. */
    val porSimular = mutableListOf<() -> Miga>()
    val pops = mutableListOf<Pop>()
    var chompIni = -1f; var chompFin = -1f; var chompPer = .13f
    /** Instantes de cada bocado: ahí cierra la mandíbula y vibra el teléfono. */
    val golpes = mutableListOf<Float>()
    var final = 1f
}

/* ==================== Geometría ==================== */

/** Posiciones medidas en pantalla, relativas a la cabecera completa. */
class GeometriaComelon {
    internal var cabecera = Offset.Zero
    internal var altoCabecera = 0f
    internal var logo = Rect.Zero
    internal val letras = Array(4) { Rect.Zero }
    internal val saludo = mutableListOf<Pair<Offset, TextLayoutResult>>()
    internal fun rel(c: LayoutCoordinates): Rect {
        val p = c.positionInRoot()
        return Rect(p, androidx.compose.ui.geometry.Size(c.size.width.toFloat(), c.size.height.toFloat()))
    }
}

/** Estado compartido entre la marca (logo + letras) y la capa de migas de la cabecera. */
class EstadoComelon internal constructor() {
    internal val geo = GeometriaComelon()
    internal val reloj = Animatable(0f)
    internal var linea by mutableStateOf<Linea?>(null)
    internal var oculto by mutableStateOf(false)
    internal var animando by mutableStateOf(false)
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

/* ==================== Construcción de la coreografía ==================== */

private val PUNTOS = listOf("200", "400", "800", "1600")
private val BOCADOS = intArrayOf(3, 2, 2, 3)   // N y o con tres bocados; e y x con dos

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
                    if (vy > 160 * d) vy = -vy * .22f else { estado = 1; s = hit; vx = (if (vx >= 0) 1 else -1) * maxOf(kotlin.math.abs(vx) * .6f, 40f * d); vy = 0f }
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

private fun construirComer(geo: GeometriaComelon, d: Float, rnd: Random): Linea {
    val ln = Linea()
    val ci = geo.logo.center
    val c0 = geo.letras[0].center
    val sup = superficies(geo)
    val piso = geo.cabecera.y + geo.altoCabecera - 36 * d
    fun r(a: Float, b: Float) = a + rnd.nextFloat() * (b - a)
    fun paso(t: Float, x: Float, dur: Float, curva: Curva = senoInOut) { ln.logoX.a(t, dur, x, curva); ln.logoY.ida(t, dur / 2, -3 * d) }
    fun migas(letra: Rect, t: Float, n: Int) = repeat(n) {
        val tam = r(2f, 3.4f) * d
        val x0 = letra.center.x - 12 * d + r(-3f, 3f) * d; val y0 = letra.center.y + r(-3f, 3f) * d
        val vx = r(-45f, 35f) * d; val vy = r(-70f, -20f) * d; val giro = r(0f, 90f)
        ln.porSimular += { Miga(t, tam, simulaMiga(x0, y0, vx, vy, sup, piso - tam, d), giro) }
    }
    fun miedo(le: Letra, t0: Float, dur: Float) { var t = t0; while (t < t0 + dur) { le.x.a(t, .04f, r(-1.4f, 1.4f) * d + 2 * d, lineal); le.y.a(t, .04f, r(-1f, 1f) * d, lineal); le.giro.a(t, .04f, r(-6f, 6f) + 5, lineal); t += .04f }
        le.x.a(t, .1f, 0f); le.y.a(t, .1f, 0f); le.giro.a(t, .1f, 0f) }

    // Anticipación y mordisco al aire
    ln.logoX.a(0f, .12f, -5 * d, p2In); ln.logoEx.a(0f, .12f, .88f, p2In).a(.12f, .1f, 1f)
    paso(.2f, c0.x - ci.x - 34 * d, .18f)
    ln.logoEsc.ida(.38f, .08f, 1.12f, p2Out)
    for (k in 0 until 4) ln.sacudon.a(.38f + k * .03f, .03f, if (k % 2 == 0) 2 * d else 0f, senoInOut)
    ln.letras.forEachIndexed { i, le -> le.azul.a(.42f + i * .03f, .08f, 1f); miedo(le, .45f, .22f) }

    var t = .62f; var x = 0f
    ln.letras.forEachIndexed { i, le ->
        val rect = geo.letras[i]; val xl = rect.center.x - ci.x - 14 * d
        for (k in 0 until 4) le.azul.a(t - .12f + k * .05f, .04f, if (k % 2 == 0) 0f else 1f, lineal)
        val nb = BOCADOS[i]; val dur = .26f / nb
        val marcas = if (nb == 3) listOf(0 to .45f, 2 to .5f, 1 to 1.3f) else listOf(0 to .55f, 1 to 1.3f)
        marcas.forEachIndexed { j, (k, rad) ->
            val tb = t + j * (dur + .03f)
            paso(tb, xl - 6 * d + j * (10 * d / (nb - 1)), dur)
            val tm = tb + dur * .8f
            // La boca se abre al acercarse y cierra justo en el bocado
            ln.abre.a(tb, dur * .5f, 1f, p2Out).a(tm - .05f, .05f, 0f, p2In)
            ln.golpes += tm
            le.mordida[k].a(tm, .05f, rad, p4In)
            le.giro.mas(tm + .04f, .06f, 9f).a(tm + .1f, .32f, 0f, elasticOut(1f, .4f))
            le.y.mas(tm + .04f, .06f, -2 * d).a(tm + .1f, .32f, 0f, elasticOut(1f, .4f))
            ln.letras.getOrNull(i + 1)?.let { v -> v.x.ida(tm + .05f, .05f, 1.5f * d) }
            ln.logoEy.ida(tm, .05f, .9f, p2Out); ln.logoEx.ida(tm, .05f, 1.08f, p2Out)
            migas(rect, tb + dur * .85f, 3)
        }
        val fin = t + nb * (dur + .03f)
        // Tragada, no borrada: la letra se encoge hacia la boca en vez de desaparecer de golpe
        le.esc.a(fin - .08f, .08f, .2f, p2In)
        le.x.a(fin - .08f, .08f, -10 * d, p2In)
        le.alfa.a(fin - .08f, .08f, 0f, p2In)
        ln.pops += Pop(fin - .03f, PUNTOS[i], rect.left, rect.top)
        x = xl + 4 * d; t = fin
    }
    // Sale por la derecha y reaparece por la izquierda, como en el juego
    // Se encoge al irse (sin cortarse en pleno aire) y reentra justo desde el borde de la pantalla
    val borde = -(ci.x + 4 * d)
    ln.logoX.a(t, .22f, x + 40 * d, p1In).fija(t + .22f, borde).a(t + .23f, .3f, 0f, p2Out)
    ln.logoEsc.a(t + .08f, .14f, 0f, p1In).fija(t + .22f, 1f)
    // Mastica solo en la anticipación y en el trayecto; durante los bocados manda `abre`
    ln.chompIni = .12f; ln.chompFin = .6f
    ln.abre.a(t, .08f, 1f).a(t + .08f, .08f, 0f).a(t + .23f, .1f, 1f).a(t + .33f, .1f, 0f).a(t + .43f, .1f, 1f).a(t + .53f, .08f, 0f)
    ln.logoEy.ida(t + .56f, .06f, .85f, p2Out); ln.logoEx.ida(t + .56f, .06f, 1.12f, p2Out)
    val ts = t + .64f
    ln.logoGiro.a(ts, .1f, -9f, senoInOut).a(ts + .1f, .1f, 5f, senoInOut).a(ts + .2f, .22f, 0f, elasticOut(1f, .45f))
    ln.letras.forEach { it.azul.fija(ts, 0f) }
    // Al volver, la boca se entreabre un instante antes de cerrarse
    val te = t + .6f
    ln.abre.a(te, .06f, .55f, p2Out).a(te + .1f, .12f, 0f, p2In)
    ln.golpes += te
    ln.final = ts + .42f
    return ln
}

private fun construirEscupir(geo: GeometriaComelon, d: Float): Linea {
    val ln = Linea(); val ci = geo.logo.center
    ln.logoEy.a(0f, .1f, 1.15f).a(.7f, .5f, 1f, elasticOut(1f, .35f))
    ln.logoEx.a(0f, .1f, .9f).a(.7f, .5f, 1f, elasticOut(1f, .35f))
    ln.logoGiro.a(0f, .1f, -10f).a(.7f, .5f, 0f, elasticOut(1f, .35f))
    // La boca se abre casi al tocar y la primera letra sale enseguida: la reacción se siente inmediata
    ln.abre.a(.02f, .1f, 1f, backOut(2.2f)).a(.63f, .08f, 0f, p2In)
    ln.letras.forEachIndexed { i, le ->
        val cl = geo.letras[i].center
        val at = .09f + i * .1f
        le.alfa.fija(0f, 0f).fija(at, 1f)
        val desde = Offset(ci.x - cl.x + 10 * d, ci.y - cl.y)
        le.vuelo = Vuelo(at, .4f, desde, Offset(desde.x * .5f, (-30f - 5 * i) * d), backOut(1.4f))
        le.esc.fija(0f, .25f).a(at, .4f, 1f, backOut(1.4f))
        le.giro.fija(0f, -50f).a(at, .4f, 0f, backOut(1.4f))
        le.ey.a(at + .34f, .06f, .7f).a(at + .4f, .4f, 1f, elasticOut(1f, .35f))
        le.ex.a(at + .34f, .06f, 1.25f).a(at + .4f, .4f, 1f, elasticOut(1f, .35f))
        ln.logoX.ida(at, .05f, -3 * d, p2Out)
    }
    ln.final = .09f + .3f + .4f + .4f
    return ln
}


/* ==================== Lecturas por cuadro ==================== */

private fun chomp(ln: Linea, t: Float): Float {
    if (ln.chompIni < 0 || t < ln.chompIni || t > ln.chompFin) return 0f
    val fase = (t - ln.chompIni) / ln.chompPer
    return (1 - cos(2 * PI.toFloat() * fase)) / 2
}

private fun aperturaDe(ln: Linea, t: Float) = maxOf(chomp(ln, t), ln.abre.valor(t))

/* ==================== Marca: logo + letras ==================== */

private val AzulPower = Color(0xFF9CC4FF)

@Composable
fun MarcaComelon(e: EstadoComelon, modifier: Modifier = Modifier) {
    val contexto = LocalContext.current
    val sinAnimaciones = remember { Settings.Global.getFloat(contexto.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f }
    val densidad = LocalDensity.current.density
    val alcance = rememberCoroutineScope()
    val haptica = LocalHapticFeedback.current
    val forma = RoundedCornerShape(11.dp)
    val estilo = MaterialTheme.typography.titleLarge.copy(fontSize = 19.sp, letterSpacing = (-0.4).sp, color = Color.White)

    fun alternar() {
        if (e.animando) return
        val ocultar = !e.oculto
        e.oculto = ocultar
        haptica.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        if (sinAnimaciones) { e.linea = null; return }
        val ln = if (ocultar) construirComer(e.geo, densidad, Random(7)) else construirEscupir(e.geo, densidad)
        e.linea = ln; e.animando = true
        if (ln.porSimular.isNotEmpty()) alcance.launch(kotlinx.coroutines.Dispatchers.Default) {
            ln.migas = ln.porSimular.map { it() }.sortedBy { it.t0 }
        }
        if (ln.golpes.isNotEmpty()) alcance.launch {
            var antes = 0f
            for (g in ln.golpes.sorted()) {
                kotlinx.coroutines.delay(((g - antes) * 1000).toLong()); antes = g
                haptica.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
        alcance.launch {
            e.reloj.snapTo(0f)
            e.reloj.animateTo(ln.final, tween((ln.final * 1000).toInt(), easing = LinearEasing))
            e.animando = false
        }
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .graphicsLayer { translationX = e.linea?.sacudon?.valor(e.ahora) ?: 0f }
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                role = Role.Button,
                onClickLabel = if (e.oculto) "Mostrar el nombre" else "Esconder el nombre"
            ) { alternar() }
    ) {
        // Insignia: en reposo el logo entero; en movimiento, dos mandíbulas con la garganta detrás
        Box(
            Modifier
                .zIndex(1f)
                .size(36.dp)
                .onGloballyPositioned { e.geo.logo = e.geo.rel(it) }
                .graphicsLayer {
                    val ln = e.linea; val t = e.ahora
                    if (ln != null && e.animando) {
                        translationX = ln.logoX.valor(t); translationY = ln.logoY.valor(t)
                        val esc = ln.logoEsc.valor(t)
                        scaleX = esc * ln.logoEx.valor(t); scaleY = esc * ln.logoEy.valor(t)
                        rotationZ = ln.logoGiro.valor(t)
                    }
                    transformOrigin = TransformOrigin(.5f, .55f)
                }
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
            Box(Modifier.size(36.dp).graphicsLayer { alpha = if (e.animando) 1f else .004f }) {
                // Sombra difusa como la de la insignia en reposo: sin ella las mandíbulas se ven planas
                Canvas(Modifier.size(36.dp)) {
                    drawOval(
                        Brush.radialGradient(listOf(BosqueProfundo.copy(alpha = .42f), Color.Transparent), center = Offset(size.width / 2, size.height * .62f), radius = size.width * .62f),
                        topLeft = Offset(-size.width * .1f, size.height * .05f), size = androidx.compose.ui.geometry.Size(size.width * 1.2f, size.height * 1.15f)
                    )
                }
                // Garganta recortada dentro de la insignia: el fondo oscuro nunca asoma fuera de la silueta
                Canvas(Modifier.size(36.dp).graphicsLayer {
                    val ln = e.linea; val a = if (ln != null) aperturaDe(ln, e.ahora) else 0f
                    alpha = if (a > .02f) 1f else .004f
                    // Óvalo, no la forma cuadrada: el fondo de la boca se lee curvo, como una garganta
                    shape = androidx.compose.foundation.shape.CircleShape; clip = true
                }) {
                    val w = size.width; val h = size.height
                    // Profundidad: más oscura al fondo (izquierda), con un leve brillo bosque hacia los labios
                    drawRect(Brush.horizontalGradient(listOf(Color(0xFF020B07), Color(0xFF0B3324), Color(0xFF17573C)), startX = w * .15f, endX = w))
                    // Lengua definida: contorno, surco central y brillo. Gira con la mandíbula de abajo
                    // (misma bisagra y ángulo), así siempre asoma apoyada sobre el labio inferior.
                    val ap = e.linea?.let { aperturaDe(it, e.ahora) } ?: 0f
                    rotate(24f * ap, pivot = Offset(w * .1f, h * .5f)) {
                        val lengua = androidx.compose.ui.geometry.Rect(w * .3f, h * .3f, w * .96f, h * .7f)
                        val curva = androidx.compose.ui.geometry.CornerRadius(lengua.height * .5f)
                        // Contorno más oscuro: separa la lengua del fondo de la garganta
                        drawRoundRect(Color(0xFF1F8A5B), lengua.topLeft - Offset(w * .02f, h * .02f),
                            androidx.compose.ui.geometry.Size(lengua.width + w * .04f, lengua.height + h * .04f), curva)
                        // Cuerpo con luz desde la punta (los labios) hacia el fondo
                        drawRoundRect(
                            Brush.horizontalGradient(listOf(Color(0xFF3CCB8B), Color(0xFF7FDDB0), Color(0xFFA8EBCB)), startX = lengua.left, endX = lengua.right),
                            lengua.topLeft, lengua.size, curva
                        )
                        // Surco central
                        drawLine(Color(0xFF2A9E6A).copy(alpha = .8f), Offset(lengua.left + w * .1f, lengua.center.y - h * .02f),
                            Offset(lengua.right - w * .12f, lengua.center.y - h * .03f), strokeWidth = w * .03f, cap = androidx.compose.ui.graphics.StrokeCap.Round)
                        // Brillo húmedo cerca de la punta
                        drawOval(Color.White.copy(alpha = .45f), Offset(lengua.right - w * .26f, lengua.top + h * .04f),
                            androidx.compose.ui.geometry.Size(w * .14f, h * .06f))
                    }
                    // Sombra interior bajo el labio de arriba para que la boca tenga volumen
                    drawRect(Brush.verticalGradient(listOf(Color.Black.copy(alpha = .45f), Color.Transparent), startY = 0f, endY = h * .45f))
                }
                listOf(true, false).forEach { arriba -> Mandibula(e, arriba, forma) }
            }
        }
        Spacer(Modifier.width(10.dp))
        "Nexo".forEachIndexed { i, ch -> LetraComelon(e, i, ch.toString(), estilo, densidad) }
        Spacer(Modifier.width(6.dp))
    }
}

@Composable
private fun Mandibula(e: EstadoComelon, arriba: Boolean, forma: RoundedCornerShape) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .size(36.dp)
            .graphicsLayer {
                val ln = e.linea; val a = if (ln != null) aperturaDe(ln, e.ahora) else 0f
                // Menos giro y bisagra más atrás: la nuca no se abulta al abrir
                rotationZ = if (arriba) -30f * a else 24f * a
                transformOrigin = TransformOrigin(.1f, .5f)
                val labio = 9.dp.toPx() * minOf(1f, a * 3f)
                shape = if (arriba) RoundedCornerShape(11.dp.toPx(), 11.dp.toPx(), labio, labio) else RoundedCornerShape(labio, labio, 11.dp.toPx(), 11.dp.toPx())
                clip = true
            }
            .drawWithContent {
                // Solo la mitad que le corresponde a esta mandíbula
                // Las mitades se solapan medio dp: al cerrar no queda la raya del corte
                val h = size.height / 2; val s = .5f.dp.toPx()
                val p = Path().apply { addRect(if (arriba) Rect(0f, 0f, size.width, h + s) else Rect(0f, h - s, size.width, size.height)) }
                clipPath(p) { this@drawWithContent.drawContent() }
            }
            .background(Color.White, forma)
    ) { Image(painterResource(R.drawable.logo_nexo), contentDescription = null, modifier = Modifier.size(24.dp)) }
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
                val ln = e.linea; val t = e.ahora
                compositingStrategy = CompositingStrategy.Offscreen
                if (ln == null || !e.animando) { alpha = if (e.oculto) .004f else 1f; return@graphicsLayer }
                val le = ln.letras[i]
                val v = le.vuelo
                if (v != null && t >= v.t0) {
                    val p = v.curva(((t - v.t0) / v.dur).coerceIn(0f, 1f)); val u = 1 - p
                    translationX = u * u * v.desde.x + 2 * u * p * v.control.x
                    translationY = u * u * v.desde.y + 2 * u * p * v.control.y
                } else { translationX = le.x.valor(t); translationY = le.y.valor(t) }
                rotationZ = le.giro.valor(t)
                val esc = le.esc.valor(t)
                scaleX = esc * le.ex.valor(t); scaleY = esc * le.ey.valor(t)
                alpha = le.alfa.valor(t)
            }
            .drawWithContent {
                drawContent()
                // Precalentado: operaciones imperceptibles para que el primer mordisco no compile nada nuevo
                drawCircle(Color.Black, 1f, Offset(-4f, -4f), blendMode = BlendMode.Clear)
                drawRect(AzulPower.copy(alpha = .003f), size = androidx.compose.ui.geometry.Size(1f, 1f), blendMode = BlendMode.SrcAtop)
                val ln = e.linea ?: return@drawWithContent
                if (!e.animando) return@drawWithContent
                val le = ln.letras[i]; val t = e.ahora
                // Mordidas: círculos que borran un pedazo desde el lado por donde llega el logo
                val base = sqrt((size.width * size.width + size.height * size.height) / 2)
                val centros = listOf(Offset(-.18f, .38f), Offset(-.08f, .92f), Offset(-.04f, .02f))
                for (k in 0 until 3) {
                    val rr = le.mordida[k].valor(t)
                    if (rr > 0f) drawCircle(Color.Black, rr * base, Offset(centros[k].x * size.width, centros[k].y * size.height), blendMode = BlendMode.Clear)
                }
                // Tinte azul del power-up
                val az = le.azul.valor(t)
                if (az > 0f) drawRect(AzulPower.copy(alpha = az), blendMode = BlendMode.SrcAtop)
            }
    )
}

/* ==================== Capa de migas y puntos ==================== */

@Composable
fun CapaMigasComelon(e: EstadoComelon, modifier: Modifier = Modifier) {
    val medidor = rememberTextMeasurer()
    val estiloPop = TextStyle(color = Color(0xFF8FE0B8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
    remember(medidor) { PUNTOS.forEach { medidor.measure(it, estiloPop) } }
    Canvas(modifier) {
        val ln = e.linea ?: return@Canvas
        if (!e.animando) return@Canvas
        val t = e.ahora
        val o = e.geo.cabecera
        translate(-o.x, -o.y) {
        for (m in ln.migas) {
            if (t < m.t0) continue
            val vida = (t - m.t0) / 2f; if (vida > 1f) continue
            val k = minOf(239, (vida * 239).toInt())
            val x = m.puntos[k * 2]; val y = m.puntos[k * 2 + 1]
            val a = if (vida > .85f) (1 - vida) / .15f else 1f
            // Miga de galleta: esquinas suaves y un borde tostado para que tenga volumen, no un píxel cuadrado
            val esquina = androidx.compose.ui.geometry.CornerRadius(m.tam * .35f)
            val arriba = Offset(x - m.tam / 2, y - m.tam); val lado = androidx.compose.ui.geometry.Size(m.tam, m.tam)
            drawRoundRect(Color(0xFFC9A46A).copy(alpha = a), arriba + Offset(0f, m.tam * .18f), lado, esquina)
            drawRoundRect(Color(0xFFF3E3C3).copy(alpha = a), arriba, lado, esquina)
        }
        for ((n, p) in ln.pops.withIndex()) {
            // Vida corta y alturas alternas: el siguiente puntaje nunca cae encima del anterior
            val dt = t - p.t0; if (dt < 0f || dt > .34f) continue
            val sube = if (dt < .16f) p2Out(dt / .16f) else 1f
            val a = if (dt < .22f) sube else 1 - (dt - .22f) / .12f
            val y = p.y - 6.dp.toPx() - (if (n % 2 == 0) 10 else 20).dp.toPx() * sube - (if (dt > .22f) 6.dp.toPx() * ((dt - .22f) / .12f) else 0f)
            drawText(medidor, p.texto, Offset(p.x, y), estiloPop.copy(color = estiloPop.color.copy(alpha = a.coerceIn(0f, 1f))))
        }
        }
    }
}
