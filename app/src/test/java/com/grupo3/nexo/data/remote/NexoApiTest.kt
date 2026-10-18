package com.grupo3.nexo.data.remote

import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory

class NexoApiTest {
    private lateinit var servidor: MockWebServer
    private lateinit var api: NexoApi

    @Before
    fun preparar() {
        servidor = MockWebServer()
        servidor.start()
        api = Retrofit.Builder()
            .baseUrl(servidor.url("/"))
            .addConverterFactory(Json { ignoreUnknownKeys = true }.asConverterFactory("application/json".toMediaType()))
            .build()
            .create(NexoApi::class.java)
    }

    @After
    fun cerrar() = servidor.shutdown()

    @Test
    fun `salud interpreta la respuesta del backend`() = runTest {
        servidor.enqueue(MockResponse().setBody("""{"estado":"ok","servicio":"nexo-backend","extra":1}"""))

        val r = api.salud()

        assertEquals(RespuestaSalud("ok", "nexo-backend"), r)
        assertEquals("/api/salud", servidor.takeRequest().path)
    }
}
