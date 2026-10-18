package com.grupo3.nexo.di

import com.grupo3.nexo.data.repository.AuthRepositoryImpl
import com.grupo3.nexo.data.repository.ChatRepositoryImpl
import com.grupo3.nexo.data.repository.DisponibilidadRepositoryImpl
import com.grupo3.nexo.data.repository.SesionRepositoryImpl
import com.grupo3.nexo.data.repository.SolicitudRepositoryImpl
import com.grupo3.nexo.data.repository.TutorRepositoryImpl
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
import javax.inject.Singleton

/**
 * Módulo de inyección de dependencias.
 * La UI y los casos de uso dependen de las INTERFACES de dominio;
 * aquí se decide la implementación concreta (mock ahora, remota después).
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun proveerTutorRepository(impl: TutorRepositoryImpl): TutorRepository = impl

    @Provides
    @Singleton
    fun proveerSolicitudRepository(impl: SolicitudRepositoryImpl): SolicitudRepository = impl

    @Provides
    @Singleton
    fun proveerChatRepository(impl: ChatRepositoryImpl): ChatRepository = impl

    @Provides
    @Singleton
    fun proveerSesionRepository(impl: SesionRepositoryImpl): SesionRepository = impl

    @Provides
    @Singleton
    fun proveerDisponibilidadRepository(impl: DisponibilidadRepositoryImpl): DisponibilidadRepository = impl

    @Provides
    @Singleton
    fun proveerAuthRepository(impl: AuthRepositoryImpl): AuthRepository = impl
}
