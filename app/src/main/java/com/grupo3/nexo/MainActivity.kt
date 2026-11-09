package com.grupo3.nexo

import android.content.Context
import android.content.res.Configuration
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.grupo3.nexo.ui.NexoApp
import com.grupo3.nexo.ui.theme.NexoTema
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    // La app está escrita en español: los textos de las librerías (p. ej. "Cerrar hoja" del lector
    // de pantalla) también deben salir en español aunque el teléfono esté en otro idioma.
    override fun attachBaseContext(base: Context) {
        val config = Configuration(base.resources.configuration).apply { setLocale(Locale("es", "CO")) }
        super.attachBaseContext(base.createConfigurationContext(config))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // App clara: íconos oscuros en las barras del sistema, aunque el teléfono esté en modo oscuro.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
        )
        setContent {
            NexoTema {
                NexoApp()
            }
        }
    }
}
