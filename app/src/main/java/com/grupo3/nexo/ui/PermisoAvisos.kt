package com.grupo3.nexo.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat

/** Pide una sola vez, al entrar a una cuenta, el permiso de notificaciones de Android 13 en adelante. */
@Composable
fun PedirPermisoAvisos(autenticado: Boolean) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
    val contexto = LocalContext.current
    val pedir = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }
    LaunchedEffect(autenticado) {
        if (!autenticado) return@LaunchedEffect
        val guardado = contexto.getSharedPreferences("permisos", Context.MODE_PRIVATE)
        val concedido = ContextCompat.checkSelfPermission(contexto, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (concedido || guardado.getBoolean(CLAVE_PEDIDO, false)) return@LaunchedEffect
        guardado.edit().putBoolean(CLAVE_PEDIDO, true).apply()
        pedir.launch(Manifest.permission.POST_NOTIFICATIONS)
    }
}

private const val CLAVE_PEDIDO = "avisos_pedido"
