package com.grupo3.nexo.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

/** La página de presentación y sus archivos se ven sin iniciar sesión; Google la usa como página principal. */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PaginaWebTest {

    @LocalServerPort
    int puerto;

    private HttpResponse<String> pedir(String ruta) throws Exception {
        HttpRequest peticion = HttpRequest.newBuilder(URI.create("http://localhost:" + puerto + ruta)).build();
        return HttpClient.newHttpClient().send(peticion, HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void laRaizMuestraLaPaginaDePresentacion() throws Exception {
        HttpResponse<String> respuesta = pedir("/");

        assertEquals(200, respuesta.statusCode());
        assertTrue(respuesta.body().contains("Descargar para Android"));
    }

    @Test
    void susArchivosSonPublicos() throws Exception {
        assertEquals(200, pedir("/web/logo.png").statusCode());
        assertEquals(200, pedir("/web/instrument-sans.ttf").statusCode());
        assertEquals(200, pedir("/privacidad.html").statusCode());
    }

    @Test
    void loDemasSigueProtegido() throws Exception {
        assertEquals(401, pedir("/api/usuarios/yo").statusCode());
    }
}
