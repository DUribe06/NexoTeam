package com.grupo3.nexo

import android.app.Application
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import com.grupo3.nexo.data.avisos.RevisarNovedadesWorker
import com.grupo3.nexo.data.local.AlmacenSesion
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

@HiltAndroidApp
class NexoApplication : Application(), Configuration.Provider {

    @Inject lateinit var fabricaWorkers: HiltWorkerFactory
    @Inject lateinit var almacenSesion: AlmacenSesion

    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().setWorkerFactory(fabricaWorkers).build()

    override fun onCreate() {
        super.onCreate()
        if (!BuildConfig.USAR_API) return
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            almacenSesion.esperarCarga()
            almacenSesion.sesion.map { it?.usuario?.id }.distinctUntilChanged().collect { usuario ->
                if (usuario == null) RevisarNovedadesWorker.detener(this@NexoApplication)
                else RevisarNovedadesWorker.programar(this@NexoApplication)
            }
        }
    }
}
