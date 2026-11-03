package com.grupo3.nexo.ui.perfil

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.nexo.domain.usecase.DefinirMateriasUseCase
import com.grupo3.nexo.domain.usecase.ObtenerUsuarioUseCase
import com.grupo3.nexo.ui.falloDe
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/** Cada cambio se ve de inmediato y se guarda solo. Si el servidor lo rechaza, la lista vuelve a lo último guardado. */
@HiltViewModel
class MisMateriasViewModel @Inject constructor(
    obtenerUsuario: ObtenerUsuarioUseCase,
    private val definirMaterias: DefinirMateriasUseCase
) : ViewModel() {

    private val usuario = obtenerUsuario()
    private val borrador = MutableStateFlow<List<String>?>(null)

    val materias = combine(usuario, borrador) { u, b -> b ?: u.materias }
        .stateIn(viewModelScope, SharingStarted.Eagerly, usuario.value.materias)

    private val _fallos = MutableSharedFlow<Throwable>(extraBufferCapacity = 1)
    val fallos: SharedFlow<Throwable> = _fallos

    /** Sobrevive a la pantalla: volver atrás justo después de agregar no pierde el cambio. */
    private val guardados = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Una petición a la vez: una lista vieja nunca llega después de una más nueva. */
    private val turno = Mutex()

    /** Devuelve lo que hay que avisar, o null si se agregó. */
    fun agregar(texto: String): String? {
        val resultado = materias.value.agregarMateria(texto)
        if (resultado.lista != materias.value) cambiar(resultado.lista)
        return resultado.aviso
    }

    /** El servidor exige al menos una materia: la última no se puede quitar. */
    fun quitar(materia: String) {
        val actuales = materias.value
        if (actuales.size > 1) cambiar(actuales - materia)
    }

    private fun cambiar(nueva: List<String>) {
        borrador.value = nueva
        guardados.launch {
            turno.withLock {
                val foto = borrador.value ?: return@withLock
                val fallo = falloDe { definirMaterias(foto) }
                if (borrador.value !== foto) return@withLock
                borrador.value = null
                fallo?.let { _fallos.tryEmit(it) }
            }
        }
    }
}
