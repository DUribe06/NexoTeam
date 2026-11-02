package com.grupo3.nexo.ui.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.nexo.domain.model.Resena
import com.grupo3.nexo.domain.model.ResumenActividad
import com.grupo3.nexo.domain.model.Rol
import com.grupo3.nexo.domain.model.Usuario
import com.grupo3.nexo.domain.usecase.CambiarModoUseCase
import com.grupo3.nexo.domain.usecase.CerrarSesionUseCase
import com.grupo3.nexo.domain.usecase.ObtenerMisResenasUseCase
import com.grupo3.nexo.domain.usecase.ObtenerResumenActividadUseCase
import com.grupo3.nexo.domain.usecase.ObtenerUsuarioUseCase
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
    private val cerrarSesion: CerrarSesionUseCase
) : ViewModel() {

    data class Estado(
        val usuario: Usuario,
        /** null mientras llegan las sesiones: las cifras muestran guiones en vez de ceros falsos. */
        val resumen: ResumenActividad? = null,
        val resenas: List<Resena> = emptyList()
    ) {
        val esTutor get() = usuario.rol == Rol.TUTOR
    }

    private val usuario = obtenerUsuario()

    val estado = combine(usuario, obtenerResumen(), obtenerMisResenas()) { u, resumen, resenas ->
        Estado(u, resumen, resenas)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado(usuario.value))

    fun elegirModo(rol: Rol) = cambiarModo(rol)
    fun salir() = cerrarSesion()
}
