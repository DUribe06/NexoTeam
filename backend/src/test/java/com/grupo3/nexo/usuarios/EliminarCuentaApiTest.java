package com.grupo3.nexo.usuarios;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import com.grupo3.nexo.tutorias.PruebaTutoriasBase;

/** Eliminar la cuenta borra lo que identifica a la persona y deja el historial de los demás. */
class EliminarCuentaApiTest extends PruebaTutoriasBase {

    private static final String RUTA = "/api/usuarios/yo/eliminar";

    private LocalDate dia;
    private Cuenta ana;
    private Cuenta vale;

    @BeforeEach
    void preparar() throws Exception {
        dia = manana();
        ana = tutor("Ana Torres", dia, "10:00");
        vale = cuenta("Valeria Ríos");
    }

    private String clave() {
        return "{\"contrasena\":\"" + CLAVE + "\"}";
    }

    @Test
    void conLaContrasenaMalaNoPasaNada() throws Exception {
        enviarComo(vale.token(), RUTA, "{\"contrasena\":\"otra-clave-mala\"}").andExpect(status().isUnprocessableEntity())
            .andExpect(jsonPath("$.codigo").value("CONTRASENA_INCORRECTA"));
        pedir("/api/usuarios/yo", vale.token()).andExpect(status().isOk());
    }

    @Test
    void conTutoriasAgendadasNoSePuede() throws Exception {
        tutoriaAceptada(vale, ana, dia, "10:00");
        enviarComo(vale.token(), RUTA, clave()).andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("CUENTA_CON_SESIONES"));
    }

    @Test
    void anonimizaCierraLasPendientesYElTokenDejaDeServir() throws Exception {
        String pendiente = enviarSolicitud(vale, ana, dia, "10:00");
        sesionTerminada(vale, ana);
        String correo = jdbc.queryForObject("SELECT correo FROM usuarios WHERE id = ?", String.class, java.util.UUID.fromString(vale.id()));

        enviarComo(vale.token(), RUTA, clave()).andExpect(status().isNoContent());
        em.clear();

        pedir("/api/usuarios/yo", vale.token()).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"correo\":\"" + correo + "\",\"contrasena\":\"" + CLAVE + "\"}")).andExpect(status().isUnauthorized());
        pedir("/api/solicitudes/" + pendiente, ana.token()).andExpect(jsonPath("$.estado").value("CANCELADA"))
            .andExpect(jsonPath("$.nombreEstudiante").value("Cuenta eliminada"));
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuarios WHERE correo = ? OR nombre = 'Valeria Ríos'", Integer.class, correo)).isZero();
        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM sesiones WHERE estudiante_id = ?", Integer.class, java.util.UUID.fromString(vale.id()))).isEqualTo(1);
        // El correo queda libre para volver a registrarse
        cuentaActiva(correo);
    }

    @Test
    void unTutorSaleDelCatalogoYSusSolicitudesSeRechazan() throws Exception {
        String pendiente = enviarSolicitud(vale, ana, dia, "10:00");
        enviarComo(ana.token(), RUTA, clave()).andExpect(status().isNoContent());
        em.clear();
        pedir("/api/solicitudes/" + pendiente, vale.token()).andExpect(jsonPath("$.estado").value("RECHAZADA"));
        pedir("/api/tutores/" + ana.id(), vale.token()).andExpect(status().isNotFound());
    }
}
