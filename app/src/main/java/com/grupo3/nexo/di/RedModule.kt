package com.grupo3.nexo.di

import com.grupo3.nexo.BuildConfig
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
import javax.inject.Singleton

/** Cliente HTTP hacia el backend. La URL base sale de BuildConfig (debug: emulador → localhost). */
@Module
@InstallIn(SingletonComponent::class)
object RedModule {

    @Provides
    @Singleton
    fun provideJson(): Json = Json { ignoreUnknownKeys = true }

    @Provides
    @Singleton
    fun provideOkHttp(): OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    @Provides
    @Singleton
    fun provideApi(cliente: OkHttpClient, json: Json): NexoApi = Retrofit.Builder()
        .baseUrl(BuildConfig.API_URL)
        .client(cliente)
        .addConverterFactory(json.asConverterFactory("application/json".toMediaType()))
        .build()
        .create(NexoApi::class.java)
}
