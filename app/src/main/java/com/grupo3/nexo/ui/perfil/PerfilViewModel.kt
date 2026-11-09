package com.grupo3.nexo.ui.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.nexo.domain.model.Resena
import com.grupo3.nexo.domain.model.ResumenActividad
import com.grupo3.nexo.domain.model.Rol
import com.grupo3.nexo.domain.model.Usuario
import com.grupo3.nexo.domain.usecase.CambiarFotoPerfilUseCase
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/** Lo que un perfil de tutor necesita para verse completo. */
enum class PasoPerfil { FOTO, TARIFA, MATERIAS, PRESENTACION }

/** Tarifa fijada: un precio en cero o ausente cuenta como "a convenir". */
val Usuario.tarifaFijada: Double? get() = precioHora?.takeIf { it > 0 }

fun pasosListosDe(usuario: Usuario): Set<PasoPerfil> = buildSet {
    if (!usuario.fotoUrl.isNullOrBlank()) add(PasoPerfil.FOTO)
    if (usuario.tarifaFijada != null) add(PasoPerfil.TARIFA)
    if (usuario.materias.isNotEmpty()) add(PasoPerfil.MATERIAS)
    if (usuario.bio.isNotBlank() && usuario.universidad.isNotBlank()) add(PasoPerfil.PRESENTACION)
}

@HiltViewModel
class PerfilViewModel @Inject constructor(
    obtenerUsuario: ObtenerUsuarioUseCase,
    obtenerResumen: ObtenerResumenActividadUseCase,
    obtenerMisResenas: ObtenerMisResenasUseCase,
    private val cambiarModo: CambiarModoUseCase,
    private val cerrarSesion: CerrarSesionUseCase,
    private val eliminarCuentaPropia: EliminarCuentaUseCase,
    private val cambiarFotoPerfil: CambiarFotoPerfilUseCase
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
        /** Pasos de "Completa tu perfil" ya hechos. */
        val pasosListos get() = pasosListosDe(usuario)
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

    private val _subiendoFoto = MutableStateFlow(false)
    /** Mientras sube, el avatar muestra un indicador en vez de la cámara. */
    val subiendoFoto: StateFlow<Boolean> = _subiendoFoto.asStateFlow()

    /** `imagen` es el content:// del selector; null quita la foto propia. */
    fun cambiarFoto(imagen: String?, alFallar: (Throwable) -> Unit) {
        enCurso.lanzar(viewModelScope, "foto") {
            _subiendoFoto.value = true
            val fallo = falloDe { cambiarFotoPerfil(imagen) }
            _subiendoFoto.value = false
            fallo?.let(alFallar)
        }
    }

    fun salir() = cerrarSesion()

    private val _eliminandoCuenta = MutableStateFlow(false)
    /** Mientras corre la eliminación, la hoja deshabilita el botón y muestra carga. */
    val eliminandoCuenta: StateFlow<Boolean> = _eliminandoCuenta.asStateFlow()

    /** Sin fallo, la sesión ya se cerró. */
    fun eliminarCuenta(contrasena: String, alTerminar: (Throwable?) -> Unit) = eliminar(alTerminar) { eliminarCuentaPropia(contrasena) }

    fun eliminarCuentaConGoogle(tokenGoogle: String, alTerminar: (Throwable?) -> Unit) =
        eliminar(alTerminar) { eliminarCuentaPropia.conGoogle(tokenGoogle) }

    private fun eliminar(alTerminar: (Throwable?) -> Unit, accion: suspend () -> Unit) {
        enCurso.lanzar(viewModelScope, "eliminar") {
            _eliminandoCuenta.value = true
            val fallo = falloDe(accion)
            _eliminandoCuenta.value = false
            alTerminar(fallo)
        }
    }
}
