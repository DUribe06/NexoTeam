package com.grupo3.nexo.data.repository

import com.grupo3.nexo.data.local.AlmacenSesion
import com.grupo3.nexo.data.remote.SesionDto
import com.grupo3.nexo.data.remote.aDominio
import com.grupo3.nexo.domain.model.Rol
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/** De quién son los datos en memoria: toda lectura depende de la cuenta y del modo. */
data class ClaveDatos(val usuarioId: String, val modo: Rol)

/**
 * Dice para qué cuenta y modo son los datos de los repositorios remotos. Sale de la sesión guardada, que `cambiarModo`
 * mantiene al día. Al cambiar de usuario o de modo, al cerrar sesión o al vencerse, limpia todos los [EstadoRemoto].
 */
@Singleton
class ContextoRemoto @Inject constructor(almacen: AlmacenSesion) {

    // Unconfined: la clave y la limpieza ocurren en el mismo paso en que cambia la sesión
    private val alcance = CoroutineScope(SupervisorJob() + Dispatchers.Unconfined)
    private val limpiezas = CopyOnWriteArrayList<() -> Unit>()

    /** null sin sesión o si la cuenta no usa modos: entonces no se pide nada al servidor. */
    val clave: StateFlow<ClaveDatos?> = almacen.sesion
        .map { it?.claveDatos() }
        .stateIn(alcance, SharingStarted.Eagerly, almacen.sesion.value?.claveDatos())

    init {
        alcance.launch { clave.drop(1).collect { limpiezas.forEach { limpiar -> limpiar() } } }
    }

    internal fun alCambiar(limpiar: () -> Unit) {
        limpiezas += limpiar
    }

    private fun SesionDto.claveDatos(): ClaveDatos? = usuario.rol.aDominio()?.let { ClaveDatos(usuario.id, it) }
}
