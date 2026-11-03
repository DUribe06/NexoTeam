package com.grupo3.nexo.di

import com.grupo3.nexo.data.repository.AuthRepositoryRemoto
import com.grupo3.nexo.data.repository.ChatRepositoryRemoto
import com.grupo3.nexo.data.repository.DisponibilidadRepositoryRemoto
import com.grupo3.nexo.data.repository.SesionRepositoryRemoto
import com.grupo3.nexo.data.repository.SolicitudRepositoryRemoto
import com.grupo3.nexo.data.repository.TutorRepositoryRemoto
import com.grupo3.nexo.domain.repository.AuthRepository
import com.grupo3.nexo.domain.repository.ChatRepository
import com.grupo3.nexo.domain.repository.DisponibilidadRepository
import com.grupo3.nexo.domain.repository.SesionRepository
import com.grupo3.nexo.domain.repository.SolicitudRepository
import com.grupo3.nexo.domain.repository.TutorRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/**
 * La UI y los casos de uso dependen de las INTERFACES de dominio; aquí se elige su implementación contra la API.
 * Los repositorios remotos se limpian solos al cerrar sesión o cambiar de modo (ver `ContextoRemoto`).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    abstract fun tutores(remoto: TutorRepositoryRemoto): TutorRepository

    @Binds
    abstract fun solicitudes(remoto: SolicitudRepositoryRemoto): SolicitudRepository

    @Binds
    abstract fun chats(remoto: ChatRepositoryRemoto): ChatRepository

    @Binds
    abstract fun sesiones(remoto: SesionRepositoryRemoto): SesionRepository

    @Binds
    abstract fun disponibilidad(remoto: DisponibilidadRepositoryRemoto): DisponibilidadRepository

    @Binds
    abstract fun cuentas(remoto: AuthRepositoryRemoto): AuthRepository
}
