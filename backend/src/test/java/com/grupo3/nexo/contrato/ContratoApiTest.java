package com.grupo3.nexo.contrato;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.web.servlet.MockMvc;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.ParseOptions;
import io.swagger.v3.parser.core.models.SwaggerParseResult;

/** Verifica que el contrato de la API es válido y dice lo acordado (RF1, RF2, RF16 y decisión D2). */
@SpringBootTest
@AutoConfigureMockMvc
class ContratoApiTest {

    private static final String RUTA = "static/openapi/nexo-api.yaml";
    private static SwaggerParseResult resultado;
    private static OpenAPI api;

    @Autowired
    MockMvc mvc;

    @BeforeAll
    static void leerContrato() throws IOException {
        String yaml = new String(new ClassPathResource(RUTA).getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        ParseOptions opciones = new ParseOptions();
        opciones.setResolve(true);
        resultado = new OpenAPIV3Parser().readContents(yaml, null, opciones);
        api = resultado.getOpenAPI();
    }

    private static Operation post(String ruta) {
        assertNotNull(api.getPaths().get(ruta), "falta " + ruta);
        Operation op = api.getPaths().get(ruta).getPost();
        assertNotNull(op, "falta POST " + ruta);
        return op;
    }

    @Test
    void elContratoEsOpenApiValidoYSusReferenciasResuelven() {
        assertNotNull(api, "no se pudo leer el contrato: " + resultado.getMessages());
        assertTrue(resultado.getMessages().isEmpty(), "problemas en el contrato: " + resultado.getMessages());
    }

    @Test
    void definePorCompletoLosEndpointsDeLaFase1() {
        List<String> esperados = List.of(
            "/api/auth/registro", "/api/auth/verificar-correo", "/api/auth/reenviar-codigo",
            "/api/auth/login", "/api/auth/refrescar", "/api/auth/cerrar-sesion",
            "/api/auth/recuperar", "/api/auth/restablecer");
        esperados.forEach(ruta -> post(ruta));
        assertNotNull(api.getPaths().get("/api/salud").getGet());
    }

    @Test
    void soloCerrarSesionExigeToken() {
        for (String ruta : api.getPaths().keySet()) {
            if (!ruta.startsWith("/api/auth/")) continue;
            Operation op = post(ruta);
            boolean exigeToken = op.getSecurity() != null && !op.getSecurity().isEmpty();
            assertEquals(ruta.equals("/api/auth/cerrar-sesion"), exigeToken, "seguridad de " + ruta);
        }
    }

    @Test
    void loginDocumentaTodosSusFallos() {
        var respuestas = post("/api/auth/login").getResponses().keySet();
        // 401 credenciales, 403 correo sin verificar, 423 bloqueo tras 5 intentos
        assertTrue(respuestas.containsAll(List.of("200", "400", "401", "403", "423")), "respuestas: " + respuestas);
    }

    @Test
    void elRegistroExigeConsentimientoYDocumento() {
        Schema<?> registro = api.getComponents().getSchemas().get("SolicitudRegistro");
        assertTrue(registro.getRequired().containsAll(
            List.of("correo", "contrasena", "rol", "documentoIdentidad", "aceptaTerminos", "versionTerminos")));
        assertTrue(post("/api/auth/registro").getResponses().keySet().containsAll(List.of("201", "409", "422", "429")));
    }

    @Test
    void elCodigoDeVerificacionTieneSeisDigitos() {
        Map<String, Schema> esquemas = api.getComponents().getSchemas();
        for (String nombre : List.of("SolicitudVerificarCorreo", "SolicitudRestablecer")) {
            Schema<?> codigo = (Schema<?>) esquemas.get(nombre).getProperties().get("codigo");
            assertEquals("^[0-9]{6}$", codigo.getPattern(), nombre);
        }
    }

    @Test
    void recuperarContrasenaNoRevelaSiLaCuentaExiste() {
        assertTrue(post("/api/auth/recuperar").getResponses().containsKey("204"));
        assertTrue(!post("/api/auth/recuperar").getResponses().containsKey("404"));
    }

    @Test
    void todoErrorUsaElEsquemaComun() {
        Schema<?> error = api.getComponents().getSchemas().get("Error");
        assertTrue(error.getRequired().containsAll(List.of("codigo", "mensaje")));
    }

    @Test
    void elContratoSeSirvePublicamenteYElRestoSigueProtegido() throws Exception {
        mvc.perform(get("/openapi/nexo-api.yaml")).andExpect(status().isOk());
        mvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
    }
}
