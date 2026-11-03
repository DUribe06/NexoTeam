package com.grupo3.nexo.ui.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.nexo.domain.model.Resena
import com.grupo3.nexo.domain.model.ResumenActividad
import com.grupo3.nexo.domain.model.Rol
import com.grupo3.nexo.domain.model.Usuario
import com.grupo3.nexo.domain.usecase.CambiarModoUseCase
import com.grupo3.nexo.domain.usecase.CerrarSesionUseCase
import com.grupo3.nexo.domain.usecase.EliminarCuentaUseCase
import com.grupo3.nexo.domain.usecase.ObtenerMisResenasUseCase
import com.grupo3.nexo.domain.usecase.ObtenerResumenActividadUseCase
import com.grupo3.nexo.domain.usecase.ObtenerUsuarioUseCase
import com.grupo3.nexo.ui.AccionesEnCurso
import com.grupo3.nexo.ui.Recarga
import com.grupo3.nexo.ui.falloDe
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class PerfilViewModel @Inject constructor(
    obtenerUsuario: ObtenerUsuarioUseCase,
    obtenerResumen: ObtenerResumenActividadUseCase,
    obtenerMisResenas: ObtenerMisResenasUseCase,
    private val cambiarModo: CambiarModoUseCase,
    private val cerrarSesion: CerrarSesionUseCase,
    private val eliminarCuentaPropia: EliminarCuentaUseCase
) : ViewModel() {

    data class Estado(
        val usuario: Usuario,
        /** null mientras llegan las sesiones: las cifras muestran guiones en vez de ceros falsos. */
        val resumen: ResumenActividad? = null,
        val resenas: List<Resena> = emptyList(),
        /** Falló la lectura: el bloque de reseñas muestra el error con "Reintentar". */
        val error: Boolean = false
    ) {
        val esTutor get() = usuario.rol == Rol.TUTOR
    }

    private val usuario = obtenerUsuario()

    private val recarga = Recarga()

    fun reintentar() = recarga.reintentar()

    val estado = recarga.proteger(alCargar = { Estado(usuario.value) }, alFallar = { Estado(usuario.value, error = true) }) {
        combine(usuario, obtenerResumen(), obtenerMisResenas()) { u, resumen, resenas ->
            Estado(u, resumen, resenas)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado(usuario.value))

    private val enCurso = AccionesEnCurso()

    /** Si falla el modo sigue igual y `alFallar` muestra el aviso. */
    fun elegirModo(rol: Rol, alFallar: (Throwable) -> Unit) {
        enCurso.lanzar(viewModelScope, "modo") { falloDe { cambiarModo(rol) }?.let(alFallar) }
    }

    fun salir() = cerrarSesion()

    /** Sin fallo, la sesión ya se cerró. */
    fun eliminarCuenta(contrasena: String, alTerminar: (Throwable?) -> Unit) {
        enCurso.lanzar(viewModelScope, "eliminar") { alTerminar(falloDe { eliminarCuentaPropia(contrasena) }) }
    }
}
