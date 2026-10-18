package com.grupo3.nexo.ui

import com.grupo3.nexo.ui.components.LocalEntradaActual
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.grupo3.nexo.ui.auth.PantallaLogin
import com.grupo3.nexo.ui.busqueda.PantallaBusqueda
import com.grupo3.nexo.ui.chat.PantallaChats
import com.grupo3.nexo.ui.chat.PantallaConversacion
import com.grupo3.nexo.ui.components.BarraNavegacion
import com.grupo3.nexo.ui.disponibilidad.PantallaDisponibilidad
import com.grupo3.nexo.ui.historial.PantallaHistorial
import com.grupo3.nexo.ui.inicio.PantallaInicio
import com.grupo3.nexo.ui.navigation.Rutas
import com.grupo3.nexo.ui.navigation.rutasDeBarra
import com.grupo3.nexo.ui.perfil.PantallaPerfil
import com.grupo3.nexo.ui.solicitudes.PantallaSolicitudes
import com.grupo3.nexo.ui.theme.Fondo
import com.grupo3.nexo.ui.tutor.PantallaTutorPerfil

/* Movimiento de navegación (Material 3, curvas enfatizadas: arrancan rápido y frenan suave):
 * - entre pestañas: el contenido nuevo entra deslizándose un poco desde el lado hacia el que viajan los
 *   eslabones de la barra, y el anterior se va hacia el otro; así pantalla y barra cuentan el mismo gesto;
 * - hacia un detalle: eje compartido horizontal (entra desde la derecha y vuelve hacia ella);
 * - del acceso al Inicio: fundido con un leve acercamiento, la foto continúa de una pantalla a la otra.
 * Con el gesto de atrás predictivo, estas mismas transiciones se recorren al arrastrar. */
private const val DURACION = 340
private val EntradaEnfatizada = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
private val SalidaEnfatizada = CubicBezierEasing(0.3f, 0f, 0.8f, 0.15f)
private val Enfatizada = CubicBezierEasing(0.2f, 0f, 0f, 1f)

private val ordenPestanas = listOf(Rutas.Inicio.ruta, Rutas.Solicitudes.ruta, Rutas.Chat.ruta, Rutas.Perfil.ruta)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.esPestana(): Boolean =
    initialState.destination.route in rutasDeBarra && targetState.destination.route in rutasDeBarra

/** 1 si la pestaña de destino está a la derecha de la de origen, -1 si está a la izquierda. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.sentido(): Int =
    if (ordenPestanas.indexOf(targetState.destination.route) >= ordenPestanas.indexOf(initialState.destination.route)) 1 else -1

private val entrada: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    if (esPestana()) {
        val s = sentido()
        fadeIn(tween(240, delayMillis = 60)) +
            slideInHorizontally(tween(DURACION, delayMillis = 30, easing = EntradaEnfatizada)) { s * it / 14 }
    } else {
        slideInHorizontally(tween(380, easing = EntradaEnfatizada)) { it / 4 } + fadeIn(tween(220))
    }
}
private val salida: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    if (esPestana()) {
        val s = sentido()
        fadeOut(tween(120)) + slideOutHorizontally(tween(200, easing = SalidaEnfatizada)) { -s * it / 20 }
    } else {
        slideOutHorizontally(tween(380, easing = EntradaEnfatizada)) { -it / 10 } + fadeOut(tween(240))
    }
}
private val entradaAtras: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    if (esPestana()) {
        val s = sentido()
        fadeIn(tween(240, delayMillis = 60)) +
            slideInHorizontally(tween(DURACION, delayMillis = 30, easing = EntradaEnfatizada)) { s * it / 14 }
    } else {
        slideInHorizontally(tween(380, easing = EntradaEnfatizada)) { -it / 10 } + fadeIn(tween(300, delayMillis = 40))
    }
}
private val salidaAtras: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    if (esPestana()) {
        val s = sentido()
        fadeOut(tween(120)) + slideOutHorizontally(tween(200, easing = SalidaEnfatizada)) { -s * it / 20 }
    } else {
        slideOutHorizontally(tween(300, easing = Enfatizada)) { it / 4 } + fadeOut(tween(200))
    }
}

/**
 * Raíz de la interfaz: grafo de navegación + barra inferior en las pestañas principales.
 */
@Composable
fun NexoApp() {
    val controlador = rememberNavController()
    val barraViewModel: BarraViewModel = hiltViewModel()
    val pendientes by barraViewModel.pendientes.collectAsState()
    val noLeidos by barraViewModel.noLeidos.collectAsState()

    val entradaActual by controlador.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route
    val mostrarBarra = rutaActual in rutasDeBarra

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Fondo)
    ) {
        CompositionLocalProvider(LocalEntradaActual provides entradaActual?.id) {
        NavHost(
            navController = controlador,
            startDestination = Rutas.Login.ruta,
            enterTransition = entrada,
            exitTransition = salida,
            popEnterTransition = entradaAtras,
            popExitTransition = salidaAtras,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            composable(
                Rutas.Login.ruta,
                exitTransition = { fadeOut(tween(300)) + scaleOut(tween(400, easing = Enfatizada), targetScale = 0.98f) }
            ) {
                PantallaLogin(alEntrar = { controlador.irAInicio() })
            }
            composable(
                Rutas.Inicio.ruta,
                enterTransition = {
                    if (initialState.destination.route == Rutas.Login.ruta) {
                        fadeIn(tween(500, delayMillis = 80)) + scaleIn(tween(700, easing = EntradaEnfatizada), initialScale = 1.05f)
                    } else entrada()
                }
            ) {
                PantallaInicio(
                    alAbrirChat = { id -> controlador.navigate(Rutas.Conversacion.crear(id)) },
                    alAbrirPerfil = { controlador.irAPestana(Rutas.Perfil.ruta) },
                    alVerSolicitudes = { controlador.irAPestana(Rutas.Solicitudes.ruta) },
                    alBuscar = { area -> controlador.irABusqueda(area) },
                    alAbrirDisponibilidad = { controlador.navigate(Rutas.Disponibilidad.ruta) }
                )
            }
            composable(
                Rutas.Busqueda.ruta,
                arguments = listOf(
                    navArgument(Rutas.Busqueda.ARG_AREA) {
                        type = NavType.StringType
                        nullable = true
                        defaultValue = null
                    }
                )
            ) {
                PantallaBusqueda(
                    alAbrirTutor = { id -> controlador.navigate(Rutas.TutorPerfil.crear(id)) },
                    alVolver = { controlador.popBackStack() }
                )
            }
            composable(Rutas.Solicitudes.ruta) {
                PantallaSolicitudes(
                    alBuscar = { area -> controlador.irABusqueda(area) },
                    alAbrirChat = { id -> controlador.navigate(Rutas.Conversacion.crear(id)) }
                )
            }
            composable(Rutas.Chat.ruta) {
                PantallaChats(
                    alAbrirChat = { id -> controlador.navigate(Rutas.Conversacion.crear(id)) },
                    alBuscar = { controlador.irABusqueda(null) }
                )
            }
            composable(Rutas.Perfil.ruta) {
                PantallaPerfil(
                    alCerrarSesion = { controlador.irAInicioSesion() },
                    alAbrirHistorial = { controlador.navigate(Rutas.Historial.ruta) },
                    alAbrirDisponibilidad = { controlador.navigate(Rutas.Disponibilidad.ruta) }
                )
            }
            composable(Rutas.Disponibilidad.ruta) {
                PantallaDisponibilidad(alVolver = { controlador.popBackStack() })
            }
            composable(Rutas.Historial.ruta) {
                PantallaHistorial(
                    alVolver = { controlador.popBackStack() },
                    alBuscar = { controlador.irABusqueda(null) }
                )
            }
            composable(Rutas.TutorPerfil.ruta) { entrada ->
                val tutorId = entrada.arguments?.getString("tutorId").orEmpty()
                PantallaTutorPerfil(
                    tutorId = tutorId,
                    alVolver = { controlador.popBackStack() }
                )
            }
            composable(Rutas.Conversacion.ruta) { entrada ->
                val chatId = entrada.arguments?.getString("chatId").orEmpty()
                PantallaConversacion(
                    chatId = chatId,
                    alVolver = { controlador.popBackStack() }
                )
            }
        }

        }

        AnimatedVisibility(
            visible = mostrarBarra,
            enter = slideInVertically(tween(380, easing = EntradaEnfatizada)) { it } + fadeIn(tween(240)),
            exit = slideOutVertically(tween(200, easing = SalidaEnfatizada)) { it } + fadeOut(tween(160))
        ) {
            BarraNavegacion(
                actual = rutaActual.orEmpty(),
                pendientes = pendientes,
                noLeidos = noLeidos,
                // Se ignora el toque en la pestaña activa y los que llegan en plena transición:
                // navegar con saveState/restoreState a mitad de una animación puede cerrar la app
                alSeleccionar = { ruta ->
                    if (ruta != rutaActual && entradaActual?.lifecycle?.currentState == Lifecycle.State.RESUMED) {
                        controlador.irAPestana(ruta)
                    }
                }
            )
        }
    }
}

private fun NavHostController.irAInicio() {
    navigate(Rutas.Inicio.ruta) {
        popUpTo(Rutas.Login.ruta) { inclusive = true }
    }
}

private fun NavHostController.irAInicioSesion() {
    navigate(Rutas.Login.ruta) {
        popUpTo(0) { inclusive = true }
    }
}

/** Navegación entre pestañas: conserva el estado de cada una. */
private fun NavHostController.irAPestana(ruta: String) {
    navigate(ruta) {
        popUpTo(Rutas.Inicio.ruta) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Abre Buscar como pantalla apilada (vuelve a donde estaba); con un área la aplica como filtro. */
private fun NavHostController.irABusqueda(area: String?) {
    navigate(Rutas.Busqueda.crear(area)) { launchSingleTop = true }
}
