package com.grupo3.nexo.auth;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

import jakarta.persistence.EntityManager;

/** Utilidades comunes para probar los endpoints de /api/auth contra la base de pruebas. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(ConfiguracionPruebas.class)
@Transactional
abstract class PruebaAuthBase {

    static final String CLAVE = "Clave2026seg";

    @Autowired MockMvc mvc;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired EnviadorCorreoEnMemoria correos;

    ResultActions enviar(String ruta, String cuerpo) throws Exception {
        return mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(cuerpo));
    }

    ResultActions enviarConToken(String ruta, String cuerpo, String tokenAcceso) throws Exception {
        return mvc.perform(post(ruta).contentType(MediaType.APPLICATION_JSON).content(cuerpo)
            .header("Authorization", "Bearer " + tokenAcceso));
    }

    /** Registra un estudiante y devuelve el código que se "envió" a su correo. */
    String registrar(String correo) throws Exception {
        enviar("/api/auth/registro", """
            {"nombre":"Valeria Ríos","correo":"%s","contrasena":"%s","rol":"ESTUDIANTE",
             "documentoIdentidad":"1098765432","aceptaTerminos":true,"versionTerminos":"2026-10-18"}
            """.formatted(correo, CLAVE)).andReturn();
        return correos.ultimoCodigo.get(correo);
    }

    ResultActions verificar(String correo, String codigo) throws Exception {
        return enviar("/api/auth/verificar-correo", "{\"correo\":\"%s\",\"codigo\":\"%s\"}".formatted(correo, codigo));
    }

    ResultActions reenviar(String correo) throws Exception {
        return enviar("/api/auth/reenviar-codigo", "{\"correo\":\"%s\"}".formatted(correo));
    }

    ResultActions login(String correo, String contrasena) throws Exception {
        return enviar("/api/auth/login", "{\"correo\":\"%s\",\"contrasena\":\"%s\"}".formatted(correo, contrasena));
    }

    /** Registra y verifica un estudiante; devuelve la respuesta de la sesión abierta. */
    String cuentaActiva(String correo) throws Exception {
        String codigo = registrar(correo);
        return verificar(correo, codigo).andReturn().getResponse().getContentAsString();
    }

    static String campo(String json, String ruta) {
        return JsonPath.read(json, ruta);
    }

    /** Ejecuta SQL directo (para mover fechas) y limpia el contexto de JPA para que vea el cambio. */
    void sql(String instruccion, Object... argumentos) {
        em.flush();
        jdbc.update(instruccion, argumentos);
        em.clear();
    }
}
