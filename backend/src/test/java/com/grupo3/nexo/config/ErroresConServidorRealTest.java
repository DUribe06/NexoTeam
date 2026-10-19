package com.grupo3.nexo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/**
 * Con un servidor de verdad (MockMvc no hace el reenvío a /error), un error del cliente debe llegar con su
 * código real y no disfrazado de 401.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ErroresConServidorRealTest {

    @LocalServerPort
    int puerto;

    private int estado(String metodo, String ruta, String tipo, String cuerpo) throws Exception {
        HttpRequest.Builder peticion = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta))
            .method(metodo, HttpRequest.BodyPublishers.ofString(cuerpo));
        if (tipo != null) {
            peticion.header("Content-Type", tipo);
        }
        return HttpClient.newHttpClient().send(peticion.build(), HttpResponse.BodyHandlers.ofString()).statusCode();
    }

    @Test
    void unTipoDeContenidoNoSoportadoDa415() throws Exception {
        assertEquals(415, estado("POST", "/api/auth/registro", "text/plain", "hola"));
        assertEquals(415, estado("POST", "/api/auth/login", null, "{}"));
    }

    @Test
    void unMetodoNoPermitidoDa405() throws Exception {
        assertEquals(405, estado("POST", "/api/salud", "application/json", "{}"));
    }

    @Test
    void sinTokenSigueDando401EnRutasProtegidas() throws Exception {
        assertEquals(401, estado("POST", "/api/auth/cerrar-sesion", "application/json", "{}"));
    }
}
