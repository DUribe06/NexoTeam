package com.grupo3.nexo.ui.navigation

import android.net.Uri

/**
 * Rutas del grafo de navegación.
 * Las cuatro de [rutasDeBarra] son las pestañas de la barra inferior;
 * el resto son pantallas apiladas (detalle). Buscar es apilada: se abre desde el Inicio.
 */
sealed class Rutas(val ruta: String) {
    data object Login : Rutas("login")
    data object Inicio : Rutas("inicio")
    data object Busqueda : Rutas("busqueda?area={area}") {
        const val ARG_AREA = "area"
        val base = "busqueda"
        fun crear(area: String? = null) = if (area == null) base else "busqueda?area=${Uri.encode(area)}"
    }
    data object Solicitudes : Rutas("solicitudes")
    data object Chat : Rutas("chat")
    data object Perfil : Rutas("perfil")
    data object Disponibilidad : Rutas("disponibilidad")
    data object Historial : Rutas("historial")
    data object TutorPerfil : Rutas("tutor/{tutorId}") {
        fun crear(tutorId: String) = "tutor/$tutorId"
    }
    data object Conversacion : Rutas("conversacion/{chatId}") {
        fun crear(chatId: String) = "conversacion/$chatId"
    }
}

val rutasDeBarra = setOf(
    Rutas.Inicio.ruta,
    Rutas.Solicitudes.ruta,
    Rutas.Chat.ruta,
    Rutas.Perfil.ruta
)
