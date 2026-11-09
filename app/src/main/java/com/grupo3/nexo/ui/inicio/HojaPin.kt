package com.grupo3.nexo.ui.inicio

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.grupo3.nexo.domain.model.Sesion
import com.grupo3.nexo.domain.usecase.LARGO_PIN
import com.grupo3.nexo.ui.auth.CampoCodigo
import com.grupo3.nexo.ui.auth.MensajeAcceso
import com.grupo3.nexo.ui.components.BotonPrimario
import com.grupo3.nexo.ui.components.HojaNexo
import com.grupo3.nexo.ui.theme.EspacioAjustado
import com.grupo3.nexo.ui.theme.EspacioNormal

/** El tutor escribe el PIN que le dice el estudiante: con él empieza la sesión. Si no coincide, la hoja sigue abierta. */
@Composable
internal fun HojaPin(sesion: Sesion, alCerrar: () -> Unit, alEnviar: (String, alFallar: (String) -> Unit) -> Unit) {
    var pin by rememberSaveable { mutableStateOf("") }
    var error by rememberSaveable { mutableStateOf<String?>(null) }
    var enviando by rememberSaveable { mutableStateOf(false) }
    val completo = pin.length == LARGO_PIN
    fun enviar() {
        if (!completo || enviando) return
        enviando = true
        error = null
        alEnviar(pin) {
            enviando = false
            error = it
            pin = ""
        }
    }
    HojaNexo(titulo = "Escribe el PIN", alCerrar = alCerrar, subtitulo = "Pídeselo a ${sesion.conQuien.substringBefore(' ')}. Con él empieza la sesión.") {
        CampoCodigo(
            valor = pin,
            alCambiar = { nuevo ->
                pin = nuevo.filter(Char::isDigit).take(LARGO_PIN)
                error = null
            },
            conError = error != null,
            alPulsarListo = ::enviar,
            modifier = Modifier.padding(top = EspacioAjustado),
            largo = LARGO_PIN,
            descripcion = "PIN de $LARGO_PIN dígitos"
        )
        MensajeAcceso(error = error)
        BotonPrimario(
            "Empezar tutoría",
            ::enviar,
            modifier = Modifier.fillMaxWidth().padding(top = EspacioNormal),
            habilitado = completo,
            cargando = enviando
        )
    }
}
