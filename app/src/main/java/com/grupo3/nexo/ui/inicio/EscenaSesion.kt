package com.grupo3.nexo.ui.inicio

import android.content.pm.ApplicationInfo
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.currentStateAsState
import com.grupo3.nexo.ui.theme.BordeSutil
import com.grupo3.nexo.ui.theme.CalCelda
import com.grupo3.nexo.ui.theme.CalCuerpo
import com.grupo3.nexo.ui.theme.CalDia
import com.grupo3.nexo.ui.theme.CalLinea
import com.grupo3.nexo.ui.theme.CalOscuro
import com.grupo3.nexo.ui.theme.CalSalvia
import com.grupo3.nexo.ui.theme.CodigoEspaciado
import com.grupo3.nexo.ui.theme.CurvaEstandar
import com.grupo3.nexo.ui.theme.Divisor
import com.grupo3.nexo.ui.theme.DuracionNormal
import com.grupo3.nexo.ui.theme.EspacioAjustado
import com.grupo3.nexo.ui.theme.EspacioMargen
import com.grupo3.nexo.ui.theme.EspacioMicro
import com.grupo3.nexo.ui.theme.LocalAnimacionesReducidas
import com.grupo3.nexo.ui.theme.Menta
import com.grupo3.nexo.ui.theme.Miel
import com.grupo3.nexo.ui.theme.Salvia
import com.grupo3.nexo.ui.theme.SombraSuelo
import com.grupo3.nexo.ui.theme.Superficie
import com.grupo3.nexo.ui.theme.SuperficieClara
import com.grupo3.nexo.ui.theme.TextoPrimario
import com.grupo3.nexo.ui.theme.TextoTercero
import com.grupo3.nexo.ui.theme.VerdeMarca
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Escena animada de la próxima sesión. Los personajes son títeres armados con las piezas de las hojas de
 * personajes (cabeza, torso, brazos, piernas...): cada pieza gira sobre su articulación y cuelga de su padre,
 * así el cuerpo se mueve como una sola cosa. Los datos de cada escena están en EscenaDatos.kt.
 */

/**
 * Etapas del seguimiento de una sesión, como el de un pedido. Siguen los estados del producto: solicitud
 * pendiente (RF5), aceptada (RF6), el día, el arranque, la sesión y la calificación mutua (RF9).
 * "Ya casi" y "En sesión" cambian según la modalidad: presencial o virtual. En presencial, como al pedir un
 * carro: primero el tutor va en camino y, cuando llega, ella le muestra el PIN para iniciar la sesión.
 */
enum class FaseSesion(
    val titulo: String,
    val detalle: String,
    val tituloTutor: String = titulo,
    val detalleTutor: String = detalle
) {
    SOLA("Sin sesiones por ahora", "Cuando agendes una, la seguirás aquí paso a paso."),
    ENVIADA("Solicitud enviada", "Esperando la respuesta del tutor.", "Solicitud recibida", "Un estudiante espera tu respuesta."),
    AGENDADA("Sesión agendada", "El tutor aceptó: ya está en tu calendario.", detalleTutor = "Aceptaste la solicitud: ya está en tu calendario."),
    HOY("Hoy es tu sesión", "Revisa la hora y deja listo lo que necesitas."),
    YA_CASI("Ya casi", "Faltan 30 minutos: tu tutor va en camino.", detalleTutor = "Faltan 30 minutos: tu estudiante te espera."),
    LLEGO("Tu tutor llegó", "Faltan 2 minutos: muéstrale el PIN para empezar.", "Ya llegaste", "Faltan 2 minutos: pídele el PIN al estudiante para empezar."),
    EN_SESION("Sesión en curso", "Concéntrate: el chat sigue abierto si lo necesitas."),
    YA_CASI_VIRTUAL("Ya casi", "Faltan 30 minutos: la videollamada estará lista."),
    EN_SESION_VIRTUAL("Sesión en curso", "Videollamada con tu tutor en curso.", detalleTutor = "Videollamada con tu estudiante en curso."),
    CALIFICAR("¿Cómo te fue?", "Califica la sesión para ayudar a otros estudiantes.", detalleTutor = "Califica la sesión: tu opinión también cuenta.");

    fun siguiente() = entries[(ordinal + 1) % entries.size]
}

/* ==================== Curvas ==================== */

private const val DOS_PI = 2f * PI.toFloat()

private fun suave(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return t * t * (3f - 2f * t)
}

/** Sale rápido y frena al llegar, como quien camina hacia alguien y reduce el paso. */
private fun frenada(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    return 1f - (1f - t) * (1f - t) * (1f - t)
}

/** Llega un poco más allá y vuelve: para lo que aparece con energía (la marca, las estrellas). */
private fun rebote(x: Float): Float {
    val t = x.coerceIn(0f, 1f)
    val s = 2.2f
    val u = t - 1f
    return 1f + (s + 1f) * u * u * u + s * u * u
}

private fun onda(t: Float, periodo: Float, fase: Float = 0f) = sin(DOS_PI * (t / periodo + fase))

/** Va de 0 a 1 entre a y b (en segundos) con arranque y frenada suaves. */
private fun tramo(t: Float, a: Float, b: Float) = suave((t - a) / (b - a))

/** Sube entre a y b, se mantiene y baja entre c y d. */
private fun meseta(t: Float, a: Float, b: Float, c: Float, d: Float) = tramo(t, a, b) * (1f - tramo(t, c, d))

/* ==================== Motor de títeres ==================== */

/** Movimiento de una pieza: giro en grados (horario) sobre su articulación, desplazamiento, escala y opacidad. */
private class Mov(
    val grados: Float = 0f, val dx: Float = 0f, val dy: Float = 0f,
    val escala: Float = 1f, val alfa: Float = 1f, val escalaY: Float = escala,
    /** Ancho solo de la imagen de la pieza (no lo heredan sus hijas): adelgaza un torso de perfil sin deformar cabeza ni brazos. */
    val ancho: Float = 1f,
    /** Borde donde ancla el adelgazado: 0 izquierda, 1 derecha (la espalda queda pegada a la silla). */
    val ancla: Float = 0.5f
)

private val QUIETO = Mov()

/** Lo que una escena sabe en cada cuadro: el tiempo y la mitad del ancho visible (para entrar desde el borde). */
private class Instante(val t: Float, val mitad: Float)

private typealias Movimiento = (String, Instante) -> Mov

private const val ALTO = 270f

/**
 * Dibuja una escena: el suelo coincide con el borde inferior y todas las escenas comparten escala, así los
 * personajes miden lo mismo en todas las fases. Cada pieza hereda el giro y la opacidad de su padre.
 */
private fun DrawScope.escena(r: Rig, bmps: List<ImageBitmap?>, t: Float, unDp: Float, mueve: Movimiento, pin: List<TextLayoutResult> = emptyList()) {
    val sc = size.height / ALTO
    val x0 = (size.width - r.ancho * sc) / 2f
    val instante = Instante(t, size.width / sc / 2f)
    val movs = r.capas.map { mueve(it.n, instante) }
    // El padre puede estar después en la lista (las piernas se dibujan antes que la pelvis de la que cuelgan)
    fun alfaDe(i: Int): Float = movs[i].alfa * (r.capas[i].padre.let { if (it >= 0) alfaDe(it) else 1f })
    val alfas = FloatArray(r.capas.size) { alfaDe(it) }
    // Raíz de cada pieza: un personaje que aparece o se va se desvanece entero en una capa, no pieza por pieza
    // (si no, al volverse transparente se verían las piezas de abajo a través de las de arriba)
    fun raizDe(i: Int): Int = r.capas[i].padre.let { if (it >= 0) raizDe(it) else i }
    val raices = IntArray(r.capas.size) { raizDe(it) }
    val dp = unDp / sc
    // Donde se juntan los dos fondos de "Ya casi": la calle a la izquierda, el parque a la derecha
    val costura = r.capas.firstOrNull { it.n == "costura" }?.x

    fun DrawScope.enCadena(i: Int, bloque: DrawScope.() -> Unit) {
        val c = r.capas[i]
        val m = movs[i]
        val propio: DrawScope.() -> Unit = {
            if (m === QUIETO) bloque()
            else {
                val pivote = Offset(c.px, c.py)
                withTransform({
                    translate(m.dx, m.dy)
                    rotate(m.grados, pivote)
                    scale(m.escala, m.escalaY, pivote)
                }) { bloque() }
            }
        }
        if (c.padre >= 0) enCadena(c.padre, propio) else propio()
    }

    withTransform({
        translate(x0, 0f)
        scale(sc, sc, Offset.Zero)
    }) {
        var capa = -1 // raíz cuya capa está abierta
        r.capas.forEachIndexed { i, c ->
            val raiz = raices[i]
            val grupo = movs[raiz].alfa
            if (capa >= 0 && capa != raiz) {
                drawContext.canvas.restore()
                capa = -1
            }
            if (alfas[i] <= 0.01f) return@forEachIndexed
            val img = bmps[i]
            if (img == null && c.w <= 0f) return@forEachIndexed
            if (c.n.startsWith("fondo_") && img != null) {
                fondo(c, img, movs[i], alfas[i], costura, -x0 / sc, (size.width - x0) / sc)
                return@forEachIndexed
            }
            if (grupo < 0.99f && capa != raiz) {
                drawContext.canvas.saveLayer(Rect(-ALTO, -ALTO, r.ancho + ALTO, 2 * ALTO), Paint().apply { alpha = grupo })
                capa = raiz
            }
            // Dentro de la capa del grupo, cada pieza solo lleva su propia transparencia
            val a = if (capa == raiz) alfas[i] / grupo else alfas[i]
            enCadena(i) {
                if (img != null) translate(c.x, c.y) {
                    val ancho = movs[i].ancho
                    if (ancho == 1f) drawImage(img, alpha = a, filterQuality = FilterQuality.Medium)
                    else scale(ancho, 1f, Offset(img.width * movs[i].ancla, 0f)) { drawImage(img, alpha = a, filterQuality = FilterQuality.Medium) }
                }
                else vector(c, a, dp, pin)
            }
        }
        if (capa >= 0) drawContext.canvas.restore()
    }
}

/**
 * Fondo que pasa: la tira (c.w = largo de lo que se repite) se dibuja una y otra vez hasta cubrir lo visible,
 * corrida por m.dx. Con costura, la calle solo ocupa la mitad izquierda y el parque la derecha.
 */
private fun DrawScope.fondo(c: Capa, img: ImageBitmap, m: Mov, a: Float, costura: Float?, desde: Float, hasta: Float) {
    val izq = if (costura != null && c.n == "fondo_parque") costura else desde
    val der = if (costura != null && c.n == "fondo_calle") costura else hasta
    val borde = 30f // el fondo se desvanece en sus extremos: no queda un corte seco contra el otro
    clipRect(izq, -ALTO, der, 2 * ALTO) {
        drawContext.canvas.saveLayer(Rect(izq, -ALTO, der, 2 * ALTO), Paint())
        var x = c.x + m.dx
        x -= ceil((x - izq) / c.w) * c.w
        while (x < der) {
            translate(x, c.y) { drawImage(img, alpha = a, filterQuality = FilterQuality.Medium) }
            x += c.w
        }
        drawRect(
            Brush.horizontalGradient(listOf(Color.Transparent, Color.Black), startX = izq, endX = izq + borde),
            Offset(izq, -ALTO), Size(borde, 3 * ALTO), blendMode = BlendMode.DstIn
        )
        drawRect(
            Brush.horizontalGradient(listOf(Color.Black, Color.Transparent), startX = der - borde, endX = der),
            Offset(der - borde, -ALTO), Size(borde, 3 * ALTO), blendMode = BlendMode.DstIn
        )
        drawContext.canvas.restore()
    }
}

/** El PIN de muestra que ella le enseña al tutor en "Llegó". */
private const val PIN_MUESTRA = "4821"

/* Cambio de escena: entra por la derecha y la anterior sale por la izquierda. */
private const val ESCENA_RETRASO_MS = 120
private const val ESCENA_PASO_MS = 460
private const val ESCENA_SE_APAGA_MS = 200

/** Elementos de interfaz de la escena: se dibujan en código con los colores de la app. */
private fun DrawScope.vector(c: Capa, a: Float, dp: Float, pin: List<TextLayoutResult>) {
    val centro = Offset(c.x + c.w / 2f, c.y + c.h / 2f)
    when {
        c.n == "burbuja" -> {
            // Globo de chat con la colita hacia la estudiante
            val cola = Path().apply {
                moveTo(c.x + c.h * 0.22f, c.y + c.h * 0.7f)
                lineTo(c.x - c.h * 0.16f, c.y + c.h * 0.9f)
                lineTo(c.x + c.h * 0.42f, c.y + c.h * 0.92f)
                close()
            }
            val radio = CornerRadius(c.h * 0.42f)
            drawPath(cola, Superficie, alpha = a)
            drawRoundRect(Superficie, Offset(c.x, c.y), Size(c.w, c.h), radio, alpha = a)
            drawRoundRect(BordeSutil, Offset(c.x, c.y), Size(c.w, c.h), radio, alpha = a, style = Stroke(dp))
        }
        c.n == "pin" -> {
            // Nube de lo que ella le dice al tutor, con la colita hacia su boca, y el PIN en el centro
            val cola = Path().apply {
                moveTo(c.x + c.h * 0.3f, c.y + c.h * 0.62f)
                lineTo(c.x - c.h * 0.3f, c.y + c.h * 0.84f)
                lineTo(c.x + c.h * 0.55f, c.y + c.h * 0.9f)
                close()
            }
            val radio = CornerRadius(c.h * 0.42f)
            drawPath(cola, Superficie, alpha = a)
            drawRoundRect(Superficie, Offset(c.x, c.y), Size(c.w, c.h), radio, alpha = a)
            drawRoundRect(BordeSutil, Offset(c.x, c.y), Size(c.w, c.h), radio, alpha = a, style = Stroke(dp))
        }
        c.n.startsWith("pin") -> {
            // Una cifra del PIN, centrada en su casilla
            val cifra = pin.getOrNull(c.n.last().digitToInt()) ?: return
            // La cifra se ajusta al alto de su casilla (el texto se mide en píxeles, la escena va en unidades)
            val f = c.h / cifra.size.height
            withTransform({
                translate(centro.x, centro.y)
                scale(f, f, Offset.Zero)
            }) {
                drawText(cifra, topLeft = Offset(-cifra.size.width / 2f, -cifra.size.height / 2f), alpha = a)
            }
        }
        c.n == "tarjeta" -> {
            val radio = CornerRadius(c.h * 0.13f)
            drawRoundRect(SuperficieClara, Offset(c.x, c.y), Size(c.w, c.h), radio, alpha = a)
            drawRoundRect(BordeSutil, Offset(c.x, c.y), Size(c.w, c.h), radio, alpha = a, style = Stroke(dp))
        }
        c.n == "calendario" -> calendario(c, a)
        c.n == "sombra" -> drawOval(SombraSuelo, Offset(c.x, c.y), Size(c.w, c.h), alpha = a)
        c.n == "marca" -> {
            // Chulo verde sobre el día, como en la referencia (sin círculo)
            val chulo = Path().apply {
                moveTo(c.x + c.w * 0.1f, c.y + c.h * 0.55f)
                lineTo(c.x + c.w * 0.38f, c.y + c.h * 0.8f)
                lineTo(c.x + c.w * 0.92f, c.y + c.h * 0.22f)
            }
            drawPath(chulo, VerdeMarca, alpha = a, style = Stroke(c.w * 0.17f, cap = StrokeCap.Round, join = StrokeJoin.Round))
        }
        c.n == "enlinea" -> drawCircle(Menta, c.w / 2f, centro, alpha = a)
        c.n.startsWith("estrella") -> drawPath(estrella(centro, c.w / 2f), Miel, alpha = a)
        else -> drawCircle(Salvia, c.w / 2f, centro, alpha = a) // puntos de "escribiendo" y "conectando"
    }
}

// Colores del calendario de la referencia

/** Sombra suave del suelo bajo los muebles (como en las láminas de referencia). */

/** Tarjeta de calendario: encabezado verde con su barra, nombres de los días y la cuadrícula con el fin de semana. */
private fun DrawScope.calendario(c: Capa, a: Float) {
    val (x, y, w, h) = listOf(c.x, c.y, c.w, c.h)
    val radio = CornerRadius(w * 0.05f)
    drawRoundRect(Color.Black, Offset(x, y + h * 0.02f), Size(w, h), radio, alpha = a * 0.06f) // sombra
    drawRoundRect(CalCuerpo, Offset(x, y), Size(w, h), radio, alpha = a)
    val alto = h * 0.17f
    drawPath(Path().apply { addRoundRect(RoundRect(x, y, x + w, y + alto, topLeftCornerRadius = radio, topRightCornerRadius = radio)) }, CalOscuro, alpha = a)
    drawRoundRect(CalSalvia, Offset(x + w * 0.27f, y + alto * 0.32f), Size(w * 0.46f, alto * 0.36f), CornerRadius(alto * 0.18f), alpha = a)
    val gx = x + w * 0.07f
    val gw = w * 0.86f
    val col = gw / 7f
    val dh = h * 0.045f
    for (i in 0 until 7) {
        drawRoundRect(if (i == 6) CalOscuro else CalDia, Offset(gx + i * col + col * 0.1f, y + h * 0.225f), Size(col * 0.8f, dh), CornerRadius(dh / 2f), alpha = a)
    }
    val gy = y + h * 0.31f
    val gh = h * 0.63f
    val fila = gh / 5f
    drawRect(CalCelda, Offset(gx, gy), Size(gw, gh), alpha = a)
    drawRect(CalSalvia, Offset(gx + 5 * col, gy), Size(col, gh), alpha = a)
    drawRect(CalOscuro, Offset(gx + 6 * col, gy), Size(col, gh), alpha = a)
    val trazo = col * 0.07f
    for (i in 0..7) drawLine(CalLinea, Offset(gx + i * col, gy), Offset(gx + i * col, gy + gh), trazo, alpha = a)
    for (j in 0..5) drawLine(CalLinea, Offset(gx, gy + j * fila), Offset(gx + gw, gy + j * fila), trazo, alpha = a)
}

private fun estrella(centro: Offset, radio: Float) = Path().apply {
    for (i in 0 until 10) {
        val r = if (i % 2 == 0) radio else radio * 0.47f
        val ang = -PI / 2 + i * PI / 5
        val p = Offset(centro.x + r * cos(ang).toFloat(), centro.y + r * sin(ang).toFloat())
        if (i == 0) moveTo(p.x, p.y) else lineTo(p.x, p.y)
    }
    close()
}

/* ==================== Gestos reutilizables ==================== */

/** Torso que respira desde la cintura (sube un poco al inhalar, sin deformarse) y se mece apenas. */
private fun respira(t: Float, fase: Float = 0f, inclina: Float = 0f, ancho: Float = 1f, ancla: Float = 0.5f) = Mov(
    grados = inclina + 0.6f * onda(t, 5.4f, fase),
    dy = -0.5f * onda(t, 3.7f, fase),
    ancho = ancho, ancla = ancla
)

/** Cabeza viva: dos ritmos que no coinciden para que el bucle no se note, más lo que pida la escena. */
private fun cabeza(t: Float, amp: Float, periodo: Float, fase: Float = 0f, extra: Float = 0f) = Mov(
    grados = extra + amp * onda(t, periodo, fase) + amp * 0.35f * onda(t, periodo * 0.43f, fase + 0.3f)
)

/** Lado de una pieza de frente por su nombre ("e_mangaI"): 1 = izquierda de la pantalla, -1 = derecha. */
private fun String.lado() = if (endsWith("I")) 1f else -1f

/**
 * Brazo de frente colgando, en tres tramos (manga con brazo, antebrazo, mano) que se siguen con retraso como un
 * brazo suelto: el hombro lo lleva la respiración, el codo llega un poco después y la mano al final.
 */
private fun brazoSuelto(n: String, t: Float, fase: Float): Mov? {
    val l = n.lado()
    return when (n.substringAfter('_').dropLast(1)) {
        "manga" -> Mov(grados = l * (2.2f + 1.3f * onda(t, 4.6f, fase) + 0.5f * onda(t, 7.3f, fase + 0.3f)))
        "brazo" -> Mov(grados = -l * (5f + 1.6f * onda(t - 0.15f, 4.6f, fase) + 0.6f * onda(t - 0.15f, 7.3f, fase + 0.3f)))
        "mano" -> Mov(grados = -l * (2f + 1.2f * onda(t - 0.3f, 4.6f, fase)))
        else -> null
    }
}

/** Peso que pasa de un pie al otro: la cadera se desliza y se inclina muy poco. */
private fun apoyo(t: Float, fase: Float = 0f) = Mov(dx = 1.6f * onda(t, 6.2f, fase), grados = 0.35f * onda(t, 6.2f, fase + 0.25f))

/** Largo de la pierna de frente (de la cadera al tobillo), en unidades de la escena. */
private const val PIERNA = 115f

/** Giro de la pierna que deja el tobillo en su sitio mientras la cadera se desliza. */
private fun inclinaPierna(t: Float, fase: Float) = Math.toDegrees(asin(apoyo(t, fase).dx / PIERNA).toDouble()).toFloat()

/** Piernas que se inclinan con la cadera y pies planos en el suelo: el peso se mueve sin que los pies resbalen. */
private fun piernaFirme(n: String, t: Float, fase: Float = 0f): Mov = when {
    n.contains("_pierna") -> Mov(grados = inclinaPierna(t, fase) - apoyo(t, fase).grados)
    else -> Mov(grados = -inclinaPierna(t, fase))
}

/** Teclea por ráfagas: manos que golpean alternadas y pausas para pensar mirando la pantalla. */
private class Tecleo(private val t: Float, private val periodo: Float = 7f, private val fase: Float = 0f) {
    /** El mismo tecleo unos instantes después: la cabeza sigue al torso en vez de moverse con él. */
    fun retraso(s: Float) = Tecleo(t - s, periodo, fase)
    private val u = ((t / periodo + fase) % 1f) * periodo
    /** 1 mientras escribe, 0 en la pausa (con transiciones suaves). */
    val escribe = meseta(u, 0.2f, 0.5f, periodo * 0.55f, periodo * 0.55f + 0.4f) +
        meseta(u, periodo * 0.72f, periodo * 0.72f + 0.3f, periodo - 0.5f, periodo - 0.2f)
    private val golpe = t * DOS_PI / 0.26f
    val manoCerca = escribe * 2.6f * max(0f, sin(golpe))
    val manoLejos = escribe * 2.6f * max(0f, sin(golpe + PI.toFloat()))
    /** En la pausa levanta un poco las manos y la mirada, y se echa atrás. */
    val piensa = 1f - escribe
}

/**
 * Estudiante sentada frente al portátil (tres cuartos, mira a la derecha): los antebrazos giran en el codo con cada
 * golpe de tecla y en la pausa se levantan un poco. [quieta] (0 a 1) deja de teclear para atender al tutor.
 */
private fun estudianteEscribe(n: String, t: Float, tecleo: Tecleo, cabezaExtra: Float = 0f, quieta: Float = 0f): Mov? {
    val sigue = 1f - quieta
    return when (n) {
        "e_torso" -> respira(t, inclina = (0.8f * tecleo.escribe - 1.2f * tecleo.piensa) * sigue)
        "e_cabeza" -> tecleo.retraso(0.25f).let { atras ->
            Mov(grados = cabezaExtra + (2f * atras.escribe - 3.5f * atras.piensa) * sigue + 1f * onda(t, 5.3f) + 0.4f * onda(t, 2.3f, 0.2f))
        }
        "e_ante" -> Mov(grados = (0.6f * tecleo.manoCerca - 2.5f * tecleo.piensa) * sigue - 1.5f * quieta)
        "e_ante2" -> Mov(grados = (0.6f * tecleo.manoLejos - 2f * tecleo.piensa) * sigue - 1.2f * quieta)
        else -> null
    }
}

/* ==================== Movimientos de cada fase ==================== */

private val movSola: Movimiento = { n, i -> estudianteEscribe(n, i.t, Tecleo(i.t)) ?: QUIETO }

/** Espera la respuesta: cambia el peso de pie, mira el globo de vez en cuando y los puntos saltan en ola. */
private val movEnviada: Movimiento = { n, i ->
    val t = i.t
    val mira = meseta((t % 6.5f), 1.2f, 1.8f, 3.4f, 4.1f)
    when {
        n == "e_pelvis" -> apoyo(t)
        n.startsWith("e_pie") -> piernaFirme(n, t)
        n == "e_torso" -> respira(t)
        n == "e_cabeza" -> cabeza(t, 1.4f, 5.8f, extra = 3.5f * mira)
        n.startsWith("e_manga") || n.startsWith("e_brazo") || n.startsWith("e_mano") -> brazoSuelto(n, t, if (n.lado() > 0) 0f else 0.5f) ?: QUIETO
        n == "burbuja" -> Mov(escala = 1f + 0.015f * onda(t, 3.1f), dy = -1.2f * onda(t, 3.1f, 0.2f))
        n.startsWith("punto") -> {
            val k = n.last().digitToInt()
            val s = ((t / 1.25f) - k * 0.16f).mod(1f)
            val salto = if (s < 0.36f) sin(PI.toFloat() * s / 0.36f) else 0f
            Mov(dy = -6f * salto, alfa = 0.45f + 0.55f * salto)
        }
        else -> QUIETO
    }
}

/**
 * Levanta el brazo derecho tramo a tramo (primero el hombro, luego el codo y al final la mano, que se cambia por la
 * que señala), toca el día en el calendario y aparece la marca de confirmación.
 */
private val movAgendada: Movimiento = { n, i ->
    val t = i.t % 7.5f
    fun sube(retraso: Float) = tramo(t, 0.6f + retraso, 1.3f + retraso) * (1f - tramo(t, 5f + retraso, 5.7f + retraso))
    val impulso = meseta(t, 0.35f, 0.6f, 0.6f, 0.95f) // el brazo se echa un poco atrás antes de levantarse
    val toque = meseta(t, 1.75f, 1.9f, 1.9f, 2.15f) + meseta(t, 2.35f, 2.5f, 2.5f, 2.75f)
    val manoGesto = suave(sube(0.24f) * 6f)
    when (n) {
        "e_pelvis" -> apoyo(i.t)
        "e_pieI", "e_pieD", "e_piernaI", "e_piernaD" -> piernaFirme(n, i.t)
        "e_torso" -> respira(i.t, inclina = 1.5f * sube(0f) - 1f * impulso)
        "e_cabeza" -> cabeza(i.t, 1.2f, 6.1f, extra = 4f * sube(0.1f))
        "e_mangaD" -> Mov(grados = 2.2f * (1f - sube(0f)) - 64f * sube(0f) + 6f * impulso + 1.4f * onda(i.t, 4.6f, 0.5f) * (1f - sube(0f)))
        "e_brazoD" -> Mov(grados = -5f * (1f - sube(0.12f)) - 30f * sube(0.12f) - 3f * toque)
        "e_manoD" -> Mov(alfa = 1f - manoGesto)
        "e_senala" -> Mov(grados = 14f * (1f - sube(0.24f)) - 5f * toque, alfa = manoGesto)
        "marca" -> {
            val visible = 1f - tramo(t, 6.6f, 7.2f)
            Mov(escala = rebote((t - 1.95f) / 0.45f).coerceAtLeast(0f) * visible, alfa = suave((t - 1.95f) / 0.15f) * visible)
        }
        else -> if (n.startsWith("e_manga") || n.startsWith("e_brazo") || n.startsWith("e_mano")) brazoSuelto(n, i.t, 0f) ?: QUIETO else QUIETO
    }
}

/**
 * Como en la referencia: sube la muñeca hasta el pecho y la gira para ver el reloj; mientras mira, la otra mano va a la
 * cadera (impaciencia); luego baja los dos brazos, primero el del reloj.
 */
private val movHoy: Movimiento = { n, i ->
    val t = i.t % 7.5f
    fun sube(retraso: Float) = tramo(t, 0.7f + retraso, 1.45f + retraso) * (1f - tramo(t, 5f + retraso, 5.7f + retraso))
    fun jarra(retraso: Float) = tramo(t, 1.6f + retraso, 2.3f + retraso) * (1f - tramo(t, 4.7f + retraso, 5.4f + retraso))
    val revisa = meseta(t, 1.5f, 1.75f, 1.75f, 2.1f)
    val manoGesto = suave(sube(0.3f) * 6f)
    when (n) {
        "e_pelvis" -> apoyo(i.t)
        "e_pieI", "e_pieD", "e_piernaI", "e_piernaD" -> piernaFirme(n, i.t)
        "e_torso" -> respira(i.t, inclina = -1.2f * sube(0f))
        "e_cabeza" -> cabeza(i.t, 1.2f, 5.6f, extra = -6f * sube(0.1f) - 1.5f * revisa)
        "e_mangaI" -> Mov(grados = 2.2f * (1f - sube(0f)) + 18f * sube(0f) + 1.3f * onda(i.t, 4.6f) * (1f - sube(0f)))
        "e_brazoI" -> Mov(grados = -5f * (1f - sube(0.12f)) - 132f * sube(0.12f) + 2f * revisa)
        "e_manoI" -> Mov(alfa = 1f - manoGesto)
        "e_reloj" -> Mov(grados = 6f * (1f - sube(0.3f)) - 4f * revisa, alfa = manoGesto)
        "e_mangaD" -> Mov(grados = -2.2f * (1f - jarra(0f)) - 28f * jarra(0f) - 1.3f * onda(i.t, 4.6f, 0.5f) * (1f - jarra(0f)))
        "e_brazoD" -> Mov(grados = 5f * (1f - jarra(0.1f)) + 52f * jarra(0.1f))
        "e_manoD" -> Mov(grados = 2f + 10f * jarra(0.2f))
        else -> QUIETO
    }
}

/** Ritmo de cada uno: duración de un paso completo y lo que avanza el cuerpo en una pisada, en unidades de escena. */
private class Ritmo(val paso: Float, val zancada: Float) {
    /** Lo que pasa su fondo por segundo: lo mismo que avanzan los pies, así no patinan. */
    val velocidad = 2f * zancada / paso
}

private val ritmoEstudiante = Ritmo(1.15f, 86f)
private val ritmoTutor = Ritmo(1.28f, 94f) // más largo y pausado, como alguien que no tiene prisa

/** Los fondos se ven tenues (son el lugar, no el tema) y pasan algo más lento que los pies: el fondo queda lejos. */
private const val ALFA_FONDO = 0.55f
private const val PROFUNDIDAD = 0.88f

/**
 * Una pierna y un brazo de perfil al caminar. fase avanza 2π por paso completo; vel (0 a 1) calma el gesto al
 * frenar; s = -1 para el personaje en espejo; inclina = cuánto se echa hacia adelante al caminar. La pierna que
 * avanza dobla la rodilla y el brazo de su lado va al revés, con el codo doblándose al adelantarlo; el cuerpo
 * sube cuando las piernas se cruzan y rebota un poco en cada pisada.
 */
private fun caminar(parte: String, fase: Float, vel: Float, s: Float, t: Float, inclina: Float = 2f): Mov? = when (parte) {
    "raiz" -> Mov(dy = (-2.6f * cos(fase) * cos(fase) + 0.6f * abs(sin(fase))) * vel)
    "torso" -> respira(t, inclina = s * inclina * vel)
    "cabeza" -> Mov(grados = s * (-1.5f * vel + 0.8f * onda(t, 4.8f)))
    "muslo" -> Mov(grados = s * -22f * sin(fase) * vel)
    "muslo2" -> Mov(grados = s * 22f * sin(fase) * vel)
    "pierna" -> Mov(grados = s * (4f + 38f * max(0f, cos(fase - 0.3f))) * vel)
    "pierna2" -> Mov(grados = s * (4f + 38f * max(0f, -cos(fase - 0.3f))) * vel)
    // El brazo se balancea ±30° (la mano pasa delante y detrás de la pierna) y el codo se dobla al adelantarlo
    "brazo" -> Mov(grados = s * (30f * sin(fase) * vel - 3f * (1f - vel)))
    "brazo2" -> Mov(grados = s * (-30f * sin(fase) * vel - 3f * (1f - vel)))
    "ante" -> Mov(grados = s * -(10f + 30f * max(0f, -sin(fase)) * vel))
    "ante2" -> Mov(grados = s * -(10f + 30f * max(0f, sin(fase)) * vel))
    else -> null
}

/**
 * Ya casi (faltan 30 minutos): cada uno camina sin parar en su mitad, ella por la calle y él por el parque, y
 * su fondo pasa detrás al ritmo de sus pasos.
 */
private val movYaCasi: Movimiento = { n, i ->
    when (n) {
        "fondo_calle" -> Mov(dx = -(ritmoEstudiante.velocidad * PROFUNDIDAD * i.t) % 100_000f, alfa = ALFA_FONDO)
        "fondo_parque" -> Mov(dx = (ritmoTutor.velocidad * PROFUNDIDAD * i.t) % 100_000f, alfa = ALFA_FONDO)
        else -> {
            val tutor = n.startsWith("t_")
            val r = if (tutor) ritmoTutor else ritmoEstudiante
            val fase = DOS_PI * i.t / r.paso + if (tutor) 1.1f else 0f
            caminar(n.substring(2), fase, 1f, if (tutor) -1f else 1f, i.t, if (tutor) 3.5f else 2f) ?: QUIETO
        }
    }
}

private const val LLEGA = 2.6f
private const val ACERCA = 110f

/**
 * Llegó (faltan 2 minutos): los dos terminan de acercarse y frenan con un último paso corto, cara a cara. Ella
 * sube la mirada hacia él y entonces le sale la nube con el PIN, cifra a cifra. Corre con el tiempo de la fase:
 * la llegada pasa una vez y luego quedan quietos, respirando, con la nube flotando y el tutor asintiendo.
 */
private val movLlego: Movimiento = { n, i ->
    val t = i.t
    val avance = frenada(t / LLEGA)
    val resto = 1f - (t / LLEGA).coerceIn(0f, 1f)
    val vel = (3f * resto * resto).coerceAtMost(1f)
    val nube = (t - LLEGA - 0.45f) / 0.45f
    val mira = tramo(t, LLEGA - 0.5f, LLEGA + 0.2f)
    val asiente = sin((t - 3.9f) * DOS_PI / 0.55f) * meseta((t - 3.7f) % 7f, 0f, 0.2f, 1.1f, 1.4f) * tramo(t, 3.6f, 3.8f)
    when {
        n == "fondo_calle" -> Mov(alfa = ALFA_FONDO)
        n == "pin" -> Mov(
            escala = 0.6f + 0.4f * rebote(nube).coerceAtLeast(0f),
            dy = -1f * onda(t, 3.1f) * tramo(t, LLEGA + 0.9f, LLEGA + 1.4f),
            alfa = suave(nube * 1.6f)
        )
        // Cada cifra entra con su propio retraso y un saltito
        n.startsWith("pin") -> {
            val k = n.last().digitToInt()
            val e = (t - LLEGA - 0.7f - k * 0.16f) / 0.3f
            Mov(escala = 0.7f + 0.3f * rebote(e).coerceAtLeast(0f), dy = 2f * (1f - suave(e)), alfa = suave(e * 1.5f))
        }
        else -> {
            val tutor = n.startsWith("t_")
            val s = if (tutor) -1f else 1f
            val r = if (tutor) ritmoTutor else ritmoEstudiante
            val fase = ACERCA * avance / r.zancada * PI.toFloat() + if (tutor) 1.1f else 0f
            val parte = n.substring(2)
            when {
                parte == "raiz" -> Mov(dx = -s * ACERCA * (1f - avance), dy = (-2.6f * cos(fase) * cos(fase) + 0.6f * abs(sin(fase))) * vel)
                tutor && parte == "cabeza" -> Mov(grados = s * (-1.5f * vel + 3f * asiente + 0.8f * onda(t, 4.8f)))
                // Ella mira al tutor mientras le muestra el código
                !tutor && parte == "cabeza" -> Mov(grados = -1.5f * vel - 3.5f * mira + 0.8f * onda(t, 5.2f))
                else -> caminar(parte, fase, vel, s, t, if (tutor) 3.5f else 2f) ?: QUIETO
            }
        }
    }
}

private const val CICLO_SESION = 10f

/**
 * Sesión presencial, como en la lámina de la escena: el tutor pasa la página del libro, luego señala un párrafo
 * (ella deja de teclear y mira el libro) y al final le explica con la mano abierta mientras ella asiente. La mano
 * de cada gesto entra con un fundido corto mientras el antebrazo se mueve, así el cambio no se nota.
 */
private val movSesion: Movimiento = { n, i ->
    val t = i.t
    val u = t % CICLO_SESION
    val pagina = meseta(u, 0.3f, 0.6f, 2.8f, 3.1f)
    val senala = meseta(u, 2.8f, 3.1f, 5.6f, 5.9f)
    val abierta = meseta(u, 5.6f, 5.9f, 9f, 9.4f)
    val levanta = meseta(u, 5.6f, 6.4f, 8.6f, 9.3f)
    val ademan = sin((u - 6.4f) * DOS_PI / 1.4f) * meseta(u, 6.3f, 6.6f, 8.3f, 8.7f)
    val toque = max(0f, sin((u - 3.4f) * DOS_PI / 0.6f)) * meseta(u, 3.3f, 3.5f, 5f, 5.3f)
    val atiende = meseta(u, 3.1f, 3.6f, 8.5f, 9f)
    val asiente = sin((u - 7f) * DOS_PI / 0.6f) * meseta(u, 6.9f, 7.1f, 8f, 8.3f)
    val tecleo = Tecleo(t, CICLO_SESION, fase = 0.55f)
    estudianteEscribe(n, t, tecleo, cabezaExtra = atiende * (3f * senala - 2f * abierta) + 3f * asiente, quieta = atiende) ?: when (n) {
        "t_torso" -> respira(t, 0.4f, inclina = -1.2f * senala - 0.6f * pagina)
        "t_cabeza" -> Mov(grados = -3f * (pagina + senala) + 3f * levanta + 1.2f * ademan + 1f * onda(t, 4.4f, 0.55f))
        "t_ante" -> Mov(
            grados = 5f * meseta(u, 0.6f, 1.1f, 1.5f, 2f) + 4f * meseta(u, 1.9f, 2.3f, 2.4f, 2.8f) +
                (3f - 1.5f * toque) * senala + 26f * levanta + 4f * ademan
        )
        "t_mano" -> Mov(alfa = (1f - pagina - senala - abierta).coerceIn(0f, 1f))
        "t_pagina" -> Mov(alfa = pagina)
        "t_senala" -> Mov(alfa = senala)
        "t_abierta" -> Mov(grados = -3f * ademan, alfa = abierta)
        else -> QUIETO
    }
}

/**
 * Sesión virtual: la estudiante en su portátil y el tutor en una ventana de videollamada. En "ya casi" la
 * ventana muestra que se está conectando hasta que el tutor aparece y saluda; en la sesión, conversan.
 */
private fun movVirtual(conectando: Boolean): Movimiento = { n, i ->
    val t = i.t
    val u = t % 8f
    val presente = if (conectando) tramo(u, 3.4f, 3.8f) * (1f - tramo(u, 7.2f, 7.7f)) else 1f
    val habla = meseta(u, 0.5f, 0.9f, 3.5f, 4f)
    val saludo = if (conectando) sin((u - 3.9f) * DOS_PI / 0.6f) * meseta(u, 3.8f, 4f, 4.9f, 5.2f) else 0f
    val tecleo = Tecleo(t, 8f, fase = 0.35f)
    estudianteEscribe(n, t, tecleo, cabezaExtra = if (conectando) 0f else -2f * habla) ?: when {
        n == "t_busto" -> Mov(alfa = presente, escalaY = 1f + 0.01f * onda(t, 3.7f, 0.4f))
        n == "t_cabeza" -> Mov(
            grados = 3f * saludo + 2f * habla * onda(t, 1.4f) + 1.4f * onda(t, 4.4f, 0.55f),
            dy = -0.6f * onda(t, 3.7f, 0.4f)
        )
        n == "enlinea" -> Mov(alfa = presente, escala = 1f + 0.18f * presente * max(0f, onda(t, 1.6f)))
        n.startsWith("conecta") -> {
            val k = n.last().digitToInt()
            val s = ((t / 1.2f) - k * 0.18f).mod(1f)
            val pulso = if (s < 0.4f) sin(PI.toFloat() * s / 0.4f) else 0f
            Mov(alfa = (1f - presente) * (0.35f + 0.65f * pulso), escala = 0.85f + 0.3f * pulso)
        }
        else -> QUIETO
    }
}

private val movConectando = movVirtual(conectando = true)
private val movLlamada = movVirtual(conectando = false)

/**
 * Se despiden: el tutor levanta el brazo izquierdo (hombro, codo y mano en ese orden) y saluda moviendo el antebrazo
 * y la mano con retraso; las estrellas se encienden una a una y la estudiante asiente.
 */
private val movCalificar: Movimiento = { n, i ->
    val t = i.t
    val u = t % 7f
    fun sube(retraso: Float) = tramo(u, 0.3f + retraso, 0.9f + retraso) * (1f - tramo(u, 6.1f + retraso, 6.7f + retraso))
    val manoGesto = suave(sube(0.24f) * 6f)
    val saludo = onda(t, 0.78f)
    when {
        n == "e_pelvis" -> apoyo(t)
        n == "t_pelvis" -> apoyo(t, 0.45f)
        n.startsWith("e_pie") -> piernaFirme(n, t)
        n.startsWith("t_pie") -> piernaFirme(n, t, 0.45f)
        n == "e_torso" -> respira(t)
        n == "t_torso" -> respira(t, 0.4f, inclina = -1f * sube(0f))
        n == "e_cabeza" -> cabeza(t, 1.4f, 5.6f, 0.2f, extra = 3f * sin((u - 3.1f) * DOS_PI / 0.6f) * meseta(u, 3f, 3.2f, 4.1f, 4.4f))
        n == "t_cabeza" -> cabeza(t, 1.6f, 4.3f, 0.6f, extra = -2.5f * sube(0.1f))
        n == "t_mangaI" -> Mov(grados = 2.2f * (1f - sube(0f)) + 65f * sube(0f) + 1.3f * onda(t, 4.6f, 0.2f) * (1f - sube(0f)))
        n == "t_brazoI" -> Mov(grados = -5f * (1f - sube(0.12f)) + 90f * sube(0.12f) + 7f * saludo * sube(0.12f))
        n == "t_manoI" -> Mov(alfa = 1f - manoGesto)
        n == "t_saludo" -> Mov(grados = -8f * (1f - sube(0.24f)) + 13f * onda(t, 0.78f, -0.18f) * sube(0.24f), alfa = manoGesto)
        n.startsWith("e_manga") || n.startsWith("e_brazo") || n.startsWith("e_mano") -> brazoSuelto(n, t, if (n.lado() > 0) 0f else 0.5f) ?: QUIETO
        n.startsWith("t_manga") || n.startsWith("t_brazo") || n.startsWith("t_mano") -> brazoSuelto(n, t, if (n.lado() > 0) 0.2f else 0.7f) ?: QUIETO
        n.startsWith("estrella") -> {
            val k = n.last().digitToInt()
            val enciende = (u - 1.2f - k * 0.32f) / 0.4f
            val apaga = tramo(u, 6.0f, 6.6f)
            val lleno = suave(enciende) * (1f - apaga)
            Mov(
                escala = 0.82f + 0.18f * rebote(enciende).coerceAtLeast(0f) * (1f - apaga) + 0.03f * lleno * onda(t, 1.9f, k * 0.2f),
                alfa = 0.3f + 0.7f * lleno
            )
        }
        else -> QUIETO
    }
}

/* ==================== Composable ==================== */

@Composable
private fun rememberBitmaps(ids: List<Int>): List<ImageBitmap?>? {
    val recursos = LocalContext.current.resources
    // Se decodifican fuera del hilo principal; null mientras cargan (la escena no se dibuja)
    return produceState<List<ImageBitmap?>?>(initialValue = null, ids) {
        value = withContext(Dispatchers.IO) { ids.map { if (it == 0) null else ImageBitmap.imageResource(recursos, it) } }
    }.value
}

/** Escena de cada fase y el instante que la muestra mejor cuando no hay animación. */
private fun escenaDe(f: FaseSesion): Triple<Rig, Movimiento, Float> = when (f) {
    FaseSesion.SOLA -> Triple(rigSola, movSola, 1.5f)
    FaseSesion.ENVIADA -> Triple(rigEnviada, movEnviada, 2.5f)
    FaseSesion.AGENDADA -> Triple(rigAgendada, movAgendada, 3.5f)
    FaseSesion.HOY -> Triple(rigHoy, movHoy, 3f)
    FaseSesion.YA_CASI -> Triple(rigYaCasi, movYaCasi, 0.3f)
    FaseSesion.LLEGO -> Triple(rigLlego, movLlego, 6f)
    FaseSesion.EN_SESION -> Triple(rigSesion, movSesion, 4.2f)
    FaseSesion.YA_CASI_VIRTUAL -> Triple(rigConectando, movConectando, 5f)
    FaseSesion.EN_SESION_VIRTUAL -> Triple(rigLlamada, movLlamada, 2f)
    FaseSesion.CALIFICAR -> Triple(rigCalificar, movCalificar, 4.5f)
}

/**
 * Escena animada de la próxima sesión. Arranca en "agendada" si hay sesiones y en "sola" si no, y se
 * ajusta si el dato cambia. Doble toque rápido: salta a la siguiente fase (control de revisión del
 * flujo mientras no hay horas ni estados reales de la sesión); solo en compilaciones de depuración.
 */
@Composable
fun BarraSesion(haySesiones: Boolean, esTutor: Boolean = false, modifier: Modifier = Modifier) {
    var fase by remember(haySesiones) { mutableStateOf(if (haySesiones) FaseSesion.AGENDADA else FaseSesion.SOLA) }
    val movimiento = !LocalAnimacionesReducidas.current
    val contexto = LocalContext.current
    val revision = remember(contexto) { contexto.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0 }
    // Con letra grande el texto necesita más líneas y la franja crece; la escena conserva su alto
    val letraGrande = LocalDensity.current.fontScale > 1f
    // Un reloj de 20 minutos (en segundos): el reinicio llega tan tarde que no se nota. Solo corre con
    // animaciones activas y la app a la vista; si se detiene, conserva el último instante
    val enPantalla = LocalLifecycleOwner.current.lifecycle.currentStateAsState().value.isAtLeast(Lifecycle.State.STARTED)
    val corre = movimiento && enPantalla
    val tiempo = produceState(0f, corre) {
        if (!corre) return@produceState
        var anterior = withFrameNanos { it }
        while (true) {
            withFrameNanos { ahora ->
                value = (value + (ahora - anterior) / 1_000_000_000f) % 1200f
                anterior = ahora
            }
        }
    }
    // El PIN de la nube (de muestra: aún no hay datos reales)
    val medidor = rememberTextMeasurer()
    val estiloPin = MaterialTheme.typography.titleSmall.merge(CodigoEspaciado).copy(color = TextoPrimario)
    val pin = remember(estiloPin, revision) { if (revision) PIN_MUESTRA.map { medidor.measure(it.toString(), estiloPin) } else emptyList() }
    Column(
        modifier
            .fillMaxWidth()
            .then(if (revision) Modifier.pointerInput(Unit) { detectTapGestures(onDoubleTap = { fase = fase.siguiente() }) } else Modifier)
    ) {
        AnimatedContent(
            targetState = fase,
            // La escena nueva entra por la derecha y la anterior sale por la izquierda, como un paso más
            transitionSpec = {
                (fadeIn(tween(DuracionNormal, delayMillis = ESCENA_RETRASO_MS)) + slideInHorizontally(tween(ESCENA_PASO_MS, easing = CurvaEstandar)) { it / 3 }) togetherWith
                    (fadeOut(tween(ESCENA_SE_APAGA_MS)) + slideOutHorizontally(tween(ESCENA_PASO_MS, easing = CurvaEstandar)) { -it / 3 })
            },
            label = "escena"
        ) { f ->
            val (rig, mueve, pose) = escenaDe(f)
            // "Llegó" cuenta desde que aparece: la llegada pasa una sola vez
            val inicio = remember { if (f == FaseSesion.LLEGO) tiempo.value else 0f }
            // Solo se decodifican las imágenes de la fase visible (y, al cambiar, las de la siguiente)
            val bmps = rememberBitmaps(rig.capas.map { it.id })
            // A la izquierda, en qué paso va la sesión; a la derecha, la escena que lo cuenta
            Row(
                Modifier.fillMaxWidth().padding(horizontal = EspacioMargen).heightIn(min = 62.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(0.44f).padding(end = EspacioAjustado)) {
                    Text(
                        if (esTutor) f.tituloTutor else f.titulo,
                        style = MaterialTheme.typography.titleSmall,
                        color = TextoPrimario,
                        maxLines = if (letraGrande) 2 else 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        if (esTutor) f.detalleTutor else f.detalle,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextoTercero,
                        maxLines = if (letraGrande) 4 else 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = EspacioMicro)
                    )
                }
                Canvas(Modifier.weight(0.56f).height(62.dp).align(Alignment.Bottom).clipToBounds()) {
                    val t = if (movimiento) tiempo.value - inicio else pose
                    if (bmps != null) escena(rig, bmps, t, 1.dp.toPx(), mueve, pin)
                }
            }
        }
        // La misma línea fina que cierra la tira de materias: los personajes pisan sobre ella
        Box(Modifier.fillMaxWidth().height(1.dp).background(Divisor))
    }
}
