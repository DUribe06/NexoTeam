package com.grupo3.nexo.ui.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.grupo3.nexo.domain.model.Chat
import com.grupo3.nexo.domain.model.Rol
import com.grupo3.nexo.domain.usecase.ObtenerChatsUseCase
import com.grupo3.nexo.domain.usecase.ObtenerUsuarioUseCase
import com.grupo3.nexo.ui.Recarga
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class ChatsViewModel @Inject constructor(
    obtenerChats: ObtenerChatsUseCase,
    obtenerUsuario: ObtenerUsuarioUseCase
) : ViewModel() {
    data class Estado(
        val cargando: Boolean = true,
        val chats: List<Chat> = emptyList(),
        val esTutor: Boolean = false,
        /** Falló la lectura: la pantalla muestra el error con "Reintentar". */
        val error: Boolean = false
    )

    private val recarga = Recarga()

    fun reintentar() = recarga.reintentar()

    val estado = recarga.proteger(alCargar = { Estado() }, alFallar = { Estado(cargando = false, error = true) }) {
        combine(obtenerChats(), obtenerUsuario()) { chats, usuario ->
            Estado(cargando = false, chats = chats, esTutor = usuario.rol == Rol.TUTOR)
        }
    }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), Estado())
}
