package com.grupo3.nexo.di

import com.grupo3.nexo.BuildConfig
import com.grupo3.nexo.data.repository.AuthRepositoryImpl
import com.grupo3.nexo.data.repository.AuthRepositoryRemoto
import com.grupo3.nexo.data.repository.ChatRepositoryImpl
import com.grupo3.nexo.data.repository.ChatRepositoryRemoto
import com.grupo3.nexo.data.repository.DisponibilidadRepositoryImpl
import com.grupo3.nexo.data.repository.DisponibilidadRepositoryRemoto
import com.grupo3.nexo.data.repository.SesionRepositoryImpl
import com.grupo3.nexo.data.repository.SesionRepositoryRemoto
import com.grupo3.nexo.data.repository.SolicitudRepositoryImpl
import com.grupo3.nexo.data.repository.SolicitudRepositoryRemoto
import com.grupo3.nexo.data.repository.TutorRepositoryImpl
import com.grupo3.nexo.data.repository.TutorRepositoryRemoto
import com.grupo3.nexo.domain.repository.AuthRepository
import com.grupo3.nexo.domain.repository.ChatRepository
import com.grupo3.nexo.domain.repository.DisponibilidadRepository
import com.grupo3.nexo.domain.repository.SesionRepository
import com.grupo3.nexo.domain.repository.SolicitudRepository
import com.grupo3.nexo.domain.repository.TutorRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Provider
import javax.inject.Singleton

/**
 * Módulo de inyección de dependencias.
 * La UI y los casos de uso dependen de las INTERFACES de dominio; aquí se decide la implementación concreta según
 * `BuildConfig.USAR_API`: los datos simulados o el servidor. Los `Provider` evitan crear la que no se usa.
 * Los repositorios remotos se limpian solos al cerrar sesión o cambiar de modo (ver `ContextoRemoto`).
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun proveerTutorRepository(
        simulado: Provider<TutorRepositoryImpl>,
        remoto: Provider<TutorRepositoryRemoto>
    ): TutorRepository = if (BuildConfig.USAR_API) remoto.get() else simulado.get()

    @Provides
    @Singleton
    fun proveerSolicitudRepository(
        simulado: Provider<SolicitudRepositoryImpl>,
        remoto: Provider<SolicitudRepositoryRemoto>
    ): SolicitudRepository = if (BuildConfig.USAR_API) remoto.get() else simulado.get()

    @Provides
    @Singleton
    fun proveerChatRepository(
        simulado: Provider<ChatRepositoryImpl>,
        remoto: Provider<ChatRepositoryRemoto>
    ): ChatRepository = if (BuildConfig.USAR_API) remoto.get() else simulado.get()

    @Provides
    @Singleton
    fun proveerSesionRepository(
        simulado: Provider<SesionRepositoryImpl>,
        remoto: Provider<SesionRepositoryRemoto>
    ): SesionRepository = if (BuildConfig.USAR_API) remoto.get() else simulado.get()

    @Provides
    @Singleton
    fun proveerDisponibilidadRepository(
        simulado: Provider<DisponibilidadRepositoryImpl>,
        remoto: Provider<DisponibilidadRepositoryRemoto>
    ): DisponibilidadRepository = if (BuildConfig.USAR_API) remoto.get() else simulado.get()

    @Provides
    @Singleton
    fun proveerAuthRepository(
        simulado: Provider<AuthRepositoryImpl>,
        remoto: Provider<AuthRepositoryRemoto>
    ): AuthRepository = if (BuildConfig.USAR_API) remoto.get() else simulado.get()
}
