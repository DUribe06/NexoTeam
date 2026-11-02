package com.grupo3.nexo.di

import com.grupo3.nexo.BuildConfig
import com.grupo3.nexo.data.local.AlmacenSesion
import com.grupo3.nexo.data.local.AlmacenSesionDataStore
import com.grupo3.nexo.data.remote.AutenticadorSesion
import com.grupo3.nexo.data.remote.InterceptorSesion
import com.grupo3.nexo.data.remote.NexoApi
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

/** Cliente sin token ni renovación: lo usa el propio autenticador para pedir tokens nuevos. */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SinSesion

/** Cliente HTTP hacia el backend. La URL base sale de BuildConfig (debug: emulador → localhost). */
@Module
@InstallIn(SingletonComponent::class)
object RedModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideAlmacenSesion(impl: AlmacenSesionDataStore): AlmacenSesion = impl

    @Provides
    @Singleton
    @SinSesion
    fun provideOkHttpSinSesion(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    @SinSesion
    fun provideApiSinSesion(@SinSesion cliente: OkHttpClient, json: Json): NexoApi = crearApi(cliente, json)

    @Provides
    @Singleton
    fun provideOkHttp(
        @SinSesion base: OkHttpClient,
        @SinSesion apiSinSesion: NexoApi,
        almacen: AlmacenSesion
    ): OkHttpClient = base.newBuilder()
        .addInterceptor(InterceptorSesion(almacen))
        .authenticator(AutenticadorSesion(almacen, apiSinSesion))
        .build()

    @Provides
    @Singleton
    fun provideApi(cliente: OkHttpClient, json: Json): NexoApi = crearApi(cliente, json)

    private fun crearApi(cliente: OkHttpClient, json: Json): NexoApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_URL)
        .client(cliente)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(NexoApi::class.java)
}
