package com.grupo3.nexo.ui

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.grupo3.nexo.ui.auth.PantallaConfirmarCorreo
import com.grupo3.nexo.ui.auth.PantallaLogin
import com.grupo3.nexo.ui.auth.PantallaRecuperarContrasena
import com.grupo3.nexo.ui.busqueda.PantallaBusqueda
import com.grupo3.nexo.ui.chat.PantallaChats
import com.grupo3.nexo.ui.chat.PantallaConversacion
import com.grupo3.nexo.ui.components.BarraNavegacion
import com.grupo3.nexo.ui.components.LocalEntradaActual
import com.grupo3.nexo.ui.disponibilidad.PantallaDisponibilidad
import com.grupo3.nexo.ui.historial.PantallaHistorial
import com.grupo3.nexo.ui.inicio.PantallaInicio
import com.grupo3.nexo.ui.navigation.Rutas
import com.grupo3.nexo.ui.navigation.rutasDeAcceso
import com.grupo3.nexo.ui.navigation.rutasDeBarra
import com.grupo3.nexo.ui.perfil.PantallaDatosTutor
import com.grupo3.nexo.ui.perfil.PantallaMisMaterias
import com.grupo3.nexo.ui.perfil.PantallaMisResenas
import com.grupo3.nexo.ui.perfil.PantallaPerfil
import com.grupo3.nexo.ui.solicitudes.PantallaSolicitudes
import com.grupo3.nexo.ui.theme.CurvaEnfatizada
import com.grupo3.nexo.ui.theme.CurvaEntrada
import com.grupo3.nexo.ui.theme.CurvaSalida
import com.grupo3.nexo.ui.theme.DuracionCorta
import com.grupo3.nexo.ui.theme.DuracionLarga
import com.grupo3.nexo.ui.theme.DuracionMedia
import com.grupo3.nexo.ui.theme.DuracionNormal
import com.grupo3.nexo.ui.theme.DuracionPestana
import com.grupo3.nexo.ui.theme.Fondo
import com.grupo3.nexo.ui.theme.LocalAnimacionesReducidas
import com.grupo3.nexo.ui.theme.RetrasoInicial
import com.grupo3.nexo.ui.theme.especColor
import com.grupo3.nexo.ui.theme.especFundidoEntrada
import com.grupo3.nexo.ui.theme.especFundidoSalida
import com.grupo3.nexo.ui.tutor.PantallaTutorPerfil
import kotlinx.coroutines.delay

/* Movimiento de navegación (Material 3, curvas enfatizadas: arrancan rápido y frenan suave):
 * - entre pestañas: el contenido nuevo entra deslizándose un poco desde el lado hacia el que viajan los
 *   eslabones de la barra, y el anterior se va hacia el otro; así pantalla y barra cuentan el mismo gesto;
 * - hacia un detalle: eje compartido horizontal (entra desde la derecha y vuelve hacia ella);
 * - del acceso al Inicio: fundido breve; la entrada la hacen los bloques del Inicio, escalonados.
 * Con el gesto de atrás predictivo, estas mismas transiciones se recorren al arrastrar. */

/** Espera de la barra tras el acceso, para que entre cuando ya asentó el Inicio. */
private const val RetrasoBarraTrasAcceso = 320
/** Espera antes de dar por terminada la llegada desde el acceso. */
private const val RetrasoReinicioTrasAcceso = 800L

/** Clave con la que Recuperar contraseña le deja al acceso el correo ya restablecido. */
private const val ClaveCorreoRestablecido = "correoRestablecido"

private val ordenPestanas = listOf(Rutas.Inicio.ruta, Rutas.Solicitudes.ruta, Rutas.Chat.ruta, Rutas.Perfil.ruta)

private fun AnimatedContentTransitionScope<NavBackStackEntry>.esPestana(): Boolean =
    initialState.destination.route in rutasDeBarra && targetState.destination.route in rutasDeBarra

/** 1 si la pestaña de destino está a la derecha de la de origen, -1 si está a la izquierda. */
private fun AnimatedContentTransitionScope<NavBackStackEntry>.sentido(): Int =
    if (ordenPestanas.indexOf(targetState.destination.route) >= ordenPestanas.indexOf(initialState.destination.route)) 1 else -1

private val entrada: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    if (esPestana()) {
        val s = sentido()
        fadeIn(especFundidoEntrada()) +
            slideInHorizontally(tween(DuracionPestana, delayMillis = RetrasoInicial, easing = CurvaEntrada)) { s * it / 14 }
    } else {
        slideInHorizontally(tween(DuracionLarga, easing = CurvaEntrada)) { it / 4 } + fadeIn(tween(DuracionMedia))
    }
}
private val salida: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    if (esPestana()) {
        val s = sentido()
        fadeOut(especFundidoSalida()) + slideOutHorizontally(tween(DuracionMedia, easing = CurvaSalida)) { -s * it / 20 }
    } else {
        slideOutHorizontally(tween(DuracionLarga, easing = CurvaEntrada)) { -it / 10 } + fadeOut(tween(DuracionMedia))
    }
}
private val entradaAtras: AnimatedContentTransitionScope<NavBackStackEntry>.() -> EnterTransition = {
    if (esPestana()) {
        val s = sentido()
        fadeIn(especFundidoEntrada()) +
            slideInHorizontally(tween(DuracionPestana, delayMillis = RetrasoInicial, easing = CurvaEntrada)) { s * it / 14 }
    } else {
        slideInHorizontally(tween(DuracionLarga, easing = CurvaEntrada)) { -it / 10 } + fadeIn(tween(DuracionNormal, delayMillis = RetrasoInicial))
    }
}
private val salidaAtras: AnimatedContentTransitionScope<NavBackStackEntry>.() -> ExitTransition = {
    if (esPestana()) {
        val s = sentido()
        fadeOut(especFundidoSalida()) + slideOutHorizontally(tween(DuracionMedia, easing = CurvaSalida)) { -s * it / 20 }
    } else {
        slideOutHorizontally(tween(DuracionNormal, easing = CurvaEnfatizada)) { it / 4 } + fadeOut(tween(DuracionMedia))
    }
}

/**
 * Raíz de la interfaz: grafo de navegación + barra inferior en las pestañas principales.
 */
@Composable
fun NexoApp() {
    val controlador = rememberNavController()
    val reducidas = LocalAnimacionesReducidas.current
    val barraViewModel: BarraViewModel = hiltViewModel()
    val pendientes by barraViewModel.pendientes.collectAsStateWithLifecycle()
    val noLeidos by barraViewModel.noLeidos.collectAsStateWithLifecycle()

    val entradaActual by controlador.currentBackStackEntryAsState()
    val rutaActual = entradaActual?.destination?.route
    val mostrarBarra = rutaActual in rutasDeBarra

    // Al llegar desde el acceso, la barra espera a que los bloques del Inicio empiecen a asentarse
    var vieneDelAcceso by remember { mutableStateOf(false) }
    LaunchedEffect(mostrarBarra) {
        if (mostrarBarra && vieneDelAcceso) {
            delay(RetrasoReinicioTrasAcceso)
            vieneDelAcceso = false
        }
    }

    // Sesión que cambia sola: vence con la app abierta o estaba guardada al abrirla. Se lee la ruta del controlador,
    // no la de la composición: al cerrar sesión desde el Perfil la navegación ya ocurrió y aquí no hay nada que hacer
    val cuentaViewModel: CuentaViewModel = hiltViewModel()
    val autenticado by cuentaViewModel.autenticado.collectAsStateWithLifecycle()
    PedirPermisoAvisos(autenticado)
    LaunchedEffect(autenticado) {
        val ruta = controlador.currentDestination?.route ?: return@LaunchedEffect
        if (autenticado && ruta in rutasDeAcceso) {
            vieneDelAcceso = true
            controlador.irAInicio()
        } else if (!autenticado && ruta !in rutasDeAcceso) {
            controlador.irAInicioSesion()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Fondo)
    ) {
        CompositionLocalProvider(LocalEntradaActual provides entradaActual?.id) {
        NavHost(
            navController = controlador,
            startDestination = Rutas.Login.ruta,
            enterTransition = { if (reducidas) EnterTransition.None else entrada() },
            exitTransition = { if (reducidas) ExitTransition.None else salida() },
            popEnterTransition = { if (reducidas) EnterTransition.None else entradaAtras() },
            popExitTransition = { if (reducidas) ExitTransition.None else salidaAtras() },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            composable(
                Rutas.Login.ruta,
                exitTransition = { if (reducidas) ExitTransition.None else fadeOut(tween(DuracionCorta, easing = CurvaSalida)) }
            ) { pila ->
                val correoRestablecido by pila.savedStateHandle
                    .getStateFlow<String?>(ClaveCorreoRestablecido, null)
                    .collectAsStateWithLifecycle()
                PantallaLogin(
                    alEntrar = {
                        vieneDelAcceso = true
                        controlador.irAInicio()
                    },
                    alVerificarCorreo = { correo, reenviar ->
                        controlador.navigate(Rutas.ConfirmarCorreo.crear(correo, reenviar)) { launchSingleTop = true }
                    },
                    alOlvidarContrasena = { correo ->
                        controlador.navigate(Rutas.RecuperarContrasena.crear(correo)) { launchSingleTop = true }
                    },
                    correoRestablecido = correoRestablecido,
                    alConsumirRestablecido = { pila.savedStateHandle[ClaveCorreoRestablecido] = null }
                )
            }
            composable(
                Rutas.ConfirmarCorreo.ruta,
                arguments = listOf(
                    navArgument(Rutas.ConfirmarCorreo.ARG_CORREO) { type = NavType.StringType },
                    navArgument(Rutas.ConfirmarCorreo.ARG_REENVIAR) { type = NavType.BoolType }
                ),
                exitTransition = { if (reducidas) ExitTransition.None else fadeOut(tween(DuracionCorta, easing = CurvaSalida)) }
            ) {
                PantallaConfirmarCorreo(
                    alEntrar = {
                        vieneDelAcceso = true
                        controlador.irAInicio()
                    },
                    alVolver = { controlador.popBackStack() }
                )
            }
            composable(
                Rutas.RecuperarContrasena.ruta,
                arguments = listOf(
                    navArgument(Rutas.RecuperarContrasena.ARG_CORREO) { type = NavType.StringType }
                )
            ) {
                PantallaRecuperarContrasena(
                    alTerminar = { correo ->
                        controlador.previousBackStackEntry?.savedStateHandle?.set(ClaveCorreoRestablecido, correo)
                        controlador.popBackStack()
                    },
                    alVolver = { controlador.popBackStack() }
                )
            }
            composable(
                Rutas.Inicio.ruta,
                enterTransition = {
                    if (reducidas) EnterTransition.None
                    else if (initialState.destination.route in rutasDeAcceso) {
                        fadeIn(especFundidoEntrada())
                    } else entrada()
                }
            ) {
                PantallaInicio(
                    alAbrirChat = { id -> controlador.navigate(Rutas.Conversacion.crear(id)) },
                    alAbrirPerfil = { controlador.irAPestana(Rutas.Perfil.ruta) },
                    alVerSolicitudes = { controlador.irAPestana(Rutas.Solicitudes.ruta) },
                    alBuscar = { area -> controlador.irABusqueda(area) },
                    alAbrirDisponibilidad = { controlador.navigate(Rutas.Disponibilidad.ruta) },
                    alAbrirHistorial = { controlador.navigate(Rutas.Historial.ruta) },
                    alAbrirResenas = { controlador.navigate(Rutas.MisResenas.ruta) },
                    alAbrirMaterias = { controlador.navigate(Rutas.MisMaterias.ruta) }
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
                    alAbrirChat = { id -> controlador.navigate(Rutas.Conversacion.crear(id)) },
                    alAbrirDisponibilidad = { controlador.navigate(Rutas.Disponibilidad.ruta) }
                )
            }
            composable(Rutas.Chat.ruta) {
                PantallaChats(
                    alAbrirChat = { id -> controlador.navigate(Rutas.Conversacion.crear(id)) },
                    alBuscar = { controlador.irABusqueda(null) },
                    alVerSolicitudes = { controlador.irAPestana(Rutas.Solicitudes.ruta) }
                )
            }
            composable(Rutas.Perfil.ruta) {
                PantallaPerfil(
                    alCerrarSesion = { controlador.irAInicioSesion() },
                    alAbrirHistorial = { controlador.navigate(Rutas.Historial.ruta) },
                    alAbrirDisponibilidad = { controlador.navigate(Rutas.Disponibilidad.ruta) },
                    alAbrirResenas = { controlador.navigate(Rutas.MisResenas.ruta) },
                    alAbrirMaterias = { controlador.navigate(Rutas.MisMaterias.ruta) },
                    alEditarPerfilTutor = { controlador.navigate(Rutas.DatosTutor.ruta) }
                )
            }
            composable(Rutas.DatosTutor.ruta) {
                PantallaDatosTutor(alVolver = { controlador.popBackStack() })
            }
            composable(Rutas.MisResenas.ruta) {
                PantallaMisResenas(alVolver = { controlador.popBackStack() })
            }
            composable(Rutas.MisMaterias.ruta) {
                PantallaMisMaterias(alVolver = { controlador.popBackStack() })
            }
            composable(Rutas.Disponibilidad.ruta) {
                PantallaDisponibilidad(alVolver = { controlador.popBackStack() })
            }
            composable(Rutas.Historial.ruta) {
                PantallaHistorial(
                    alVolver = { controlador.popBackStack() },
                    alBuscar = { controlador.irABusqueda(null) },
                    alAbrirDisponibilidad = { controlador.navigate(Rutas.Disponibilidad.ruta) }
                )
            }
            composable(Rutas.TutorPerfil.ruta) {
                PantallaTutorPerfil(
                    alVolver = { controlador.popBackStack() }
                )
            }
            composable(Rutas.Conversacion.ruta) {
                PantallaConversacion(
                    alVolver = { controlador.popBackStack() }
                )
            }
        }

        }

        AnimatedVisibility(
            visible = mostrarBarra,
            enter = if (reducidas) EnterTransition.None else slideInVertically(
                tween(DuracionLarga, delayMillis = if (vieneDelAcceso) RetrasoBarraTrasAcceso else 0, easing = CurvaEntrada)
            ) { it } + fadeIn(tween(DuracionMedia, delayMillis = if (vieneDelAcceso) RetrasoBarraTrasAcceso else 0)),
            exit = if (reducidas) ExitTransition.None else slideOutVertically(tween(DuracionMedia, easing = CurvaSalida)) { it } + fadeOut(especColor())
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
    // launchSingleTop: el acceso y la observación de la sesión pueden pedirlo a la vez
    navigate(Rutas.Inicio.ruta) {
        popUpTo(Rutas.Login.ruta) { inclusive = true }
        launchSingleTop = true
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
