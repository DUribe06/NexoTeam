package com.grupo3.nexo.ui

import androidx.lifecycle.ViewModel
import com.grupo3.nexo.domain.usecase.ObtenerAutenticadoUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject

/** Le dice a la raíz de la app si hay sesión, para llevar al acceso cuando vence o al Inicio si sigue abierta. */
@HiltViewModel
class CuentaViewModel @Inject constructor(obtenerAutenticado: ObtenerAutenticadoUseCase) : ViewModel() {
    val autenticado = obtenerAutenticado()
}
