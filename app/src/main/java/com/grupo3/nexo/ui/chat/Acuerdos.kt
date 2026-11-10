package com.grupo3.nexo.ui.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.unit.dp
import com.grupo3.nexo.domain.model.Modalidad
import com.grupo3.nexo.domain.model.Sesion
import com.grupo3.nexo.domain.model.TipoAcuerdo
import com.grupo3.nexo.domain.model.inicio
import com.grupo3.nexo.ui.components.BotonPrimario
import com.grupo3.nexo.ui.components.BotonSecundario
import com.grupo3.nexo.ui.components.HojaNexo
import com.grupo3.nexo.ui.components.formatoHora
import com.grupo3.nexo.ui.theme.AreaTactilMinima
import com.grupo3.nexo.ui.theme.Divisor
import com.grupo3.nexo.ui.theme.EspacioAjustado
import com.grupo3.nexo.ui.theme.EspacioCorto
import com.grupo3.nexo.ui.theme.EspacioMargen
import com.grupo3.nexo.ui.theme.EspacioMedio
import com.grupo3.nexo.ui.theme.EspacioNormal
import com.grupo3.nexo.ui.theme.Superficie
import com.grupo3.nexo.ui.theme.TextoPrimario
import com.grupo3.nexo.ui.theme.TextoTercero
import java.time.LocalDate
import java.time.LocalDateTime

/** Cuánto después de la hora acordada se puede proponer empezar. El servidor admite hasta 60 minutos. */
private val MINUTOS_DE_RETARDO = listOf(15, 30, 45, 60)

/** Minutos que quedan para llegar después de la nueva hora: también debe caber en la sesión. */
private const val TOLERANCIA_MINUTOS = 10

/** Cualquier sesión programada que no ha empezado se puede cancelar de común acuerdo: presencial o virtual, hoy o después. */
internal fun Sesion.admiteAcuerdo(): Boolean = proxima && !iniciada

/** Solo una presencial de hoy: es lo único que el retardo puede salvar. */
internal fun Sesion.admiteRetardo(hoy: LocalDate): Boolean =
    admiteAcuerdo() && modalidad == Modalidad.PRESENCIAL && fecha == hoy

/** Las horas que aún sirven para un retardo: en el futuro y con tiempo antes de que termine la sesión. */
internal fun Sesion.minutosDeRetardo(ahora: LocalDateTime): List<Int> =
    MINUTOS_DE_RETARDO.filter { inicio.plusMinutes(it.toLong()) > ahora && it + TOLERANCIA_MINUTOS <= duracionHoras * 60 }

/** Encima de la caja de texto: en qué va el acuerdo de esta sesión y qué puede hacer cada quien. */
@Composable
internal fun TiraAcuerdo(sesion: Sesion, alAcordar: () -> Unit, alAceptar: () -> Unit, alRechazar: () -> Unit) {
    val nombre = sesion.conQuien.substringBefore(' ')
    val acuerdo = sesion.acuerdo
    val nuevaHora = acuerdo?.nuevaHora?.let(::formatoHora)
    val (titulo, detalle) = when {
        acuerdo == null && sesion.horaPactada != null -> "Nueva hora: ${formatoHora(sesion.horaPactada)}" to
            (if (sesion.retardoMio == true) "Pactaste un retardo: no llegar a esa hora penaliza más." else "$nombre pactó un retardo: no llegar a esa hora penaliza más.")
        acuerdo == null && sesion.admiteRetardo(LocalDate.now()) -> "¿Alguien va tarde?" to "Acuérdenlo aquí para evitar la penalización."
        acuerdo == null -> "¿No pueden asistir?" to "Si los dos están de acuerdo, se cancela sin penalización."
        acuerdo.propuestoPorMi && acuerdo.tipo == TipoAcuerdo.RETARDO -> "Propusiste empezar a las $nuevaHora" to "Esperando la respuesta de $nombre."
        acuerdo.propuestoPorMi -> "Propusiste cancelar la sesión" to "Esperando la respuesta de $nombre."
        acuerdo.tipo == TipoAcuerdo.RETARDO -> "$nombre propone empezar a las $nuevaHora" to "Si aceptas, no llegar a esa hora penaliza más."
        else -> "$nombre propone cancelar la sesión" to "Si aceptas, se cancela sin penalización."
    }
    Column(
        Modifier
            .fillMaxWidth()
            .background(Superficie)
            .drawBehind { drawLine(Divisor, Offset(0f, 0f), Offset(size.width, 0f), 1.dp.toPx()) }
            .padding(start = EspacioMargen, end = EspacioNormal, top = EspacioAjustado, bottom = EspacioAjustado)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(titulo, color = TextoPrimario, style = MaterialTheme.typography.titleSmall)
                Text(detalle, color = TextoTercero, style = MaterialTheme.typography.bodySmall)
            }
            // Con un retardo ya pactado solo queda poder cancelar, y eso sigue aquí
            if (acuerdo == null) {
                Spacer(Modifier.width(EspacioMedio))
                BotonSecundario("Acordar", alAcordar, compacto = true, alto = AreaTactilMinima, descripcion = "Acordar un retardo o cancelar con $nombre")
            } else if (acuerdo.propuestoPorMi) {
                Spacer(Modifier.width(EspacioMedio))
                BotonSecundario("Retirar", alRechazar, compacto = true, alto = AreaTactilMinima, descripcion = "Retirar mi propuesta")
            }
        }
        if (acuerdo != null && !acuerdo.propuestoPorMi) {
            Row(horizontalArrangement = Arrangement.spacedBy(EspacioCorto), modifier = Modifier.padding(top = EspacioAjustado)) {
                BotonSecundario("Rechazar", alRechazar, Modifier.weight(1f), compacto = true, alto = AreaTactilMinima)
                BotonPrimario("Aceptar", alAceptar, Modifier.weight(1f), compacto = true)
            }
        }
    }
}

/** @param alElegir los minutos de retardo, o null para proponer cancelar. */
@Composable
internal fun HojaAcuerdo(sesion: Sesion, alCerrar: () -> Unit, alElegir: (Int?) -> Unit) {
    val nombre = sesion.conQuien.substringBefore(' ')
    HojaNexo(titulo = "Acordar con $nombre", alCerrar = alCerrar, subtitulo = "Solo cuenta si $nombre acepta. Quedará escrito en el chat.") {
        val opciones = if (sesion.horaPactada == null && sesion.admiteRetardo(LocalDate.now())) sesion.minutosDeRetardo(LocalDateTime.now()) else emptyList()
        if (opciones.isNotEmpty()) {
            Text(
                "Voy tarde, empezamos a las",
                color = TextoPrimario,
                style = MaterialTheme.typography.titleSmall,
                modifier = Modifier.padding(top = EspacioAjustado)
            )
            Row(horizontalArrangement = Arrangement.spacedBy(EspacioCorto), modifier = Modifier.padding(top = EspacioAjustado, bottom = EspacioNormal)) {
                opciones.forEach { minutos ->
                    BotonSecundario(
                        formatoHora(sesion.hora.plusMinutes(minutos.toLong())),
                        { alElegir(minutos) },
                        Modifier.weight(1f),
                        compacto = true,
                        alto = AreaTactilMinima
                    )
                }
            }
        }
        BotonSecundario(
            "Proponer cancelar la sesión",
            { alElegir(null) },
            Modifier.fillMaxWidth().padding(top = if (opciones.isEmpty()) EspacioNormal else 0.dp),
            peligro = true
        )
        Text(
            "Si $nombre acepta, nadie recibe penalización.",
            color = TextoTercero,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(top = EspacioAjustado)
        )
    }
}
