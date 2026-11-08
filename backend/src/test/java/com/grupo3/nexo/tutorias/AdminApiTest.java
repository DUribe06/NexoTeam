package com.grupo3.nexo.tutorias;

import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.grupo3.nexo.config.HoraColombia;

/** El panel de administración (RF12): revisiones, reportes y suspensión de cuentas. */
class AdminApiTest extends PruebaTutoriasBase {

    private Cuenta admin;
    private Cuenta ana;
    private Cuenta vale;

    @BeforeEach
    void preparar() throws Exception {
        admin = cuenta("Administración");
        jdbc.update("UPDATE usuarios SET rol = 'ADMIN' WHERE id = ?", admin.uuid());
        ana = tutor("Ana Torres", manana(), "10:00");
        vale = cuenta("Valeria Ríos");
    }

    private UUID graveDeAna() {
        Instant hora = LocalDate.now(HoraColombia.ZONA).minusDays(3).atTime(10, 0).atZone(HoraColombia.ZONA).toInstant();
        return sesionPresencial(vale, ana, hora, null, hora.plus(30, ChronoUnit.MINUTES), null);
    }

    private String revisionAbierta() throws Exception {
        UUID s = graveDeAna();
        enviarComo(ana.token(), "/api/sesiones/" + s + "/revision", "{\"motivo\":\"Sí llegué y la app no me dejó\"}").andExpect(status().isCreated());
        return campo(pedir("/api/admin/revisiones", admin.token()).andExpect(status().isOk())
            .andExpect(jsonPath("$[0].persona").value("Ana Torres")).andExpect(jsonPath("$[0].nivel").value("GRAVE"))
            .andExpect(jsonPath("$[0].cierre").value("TUTOR_NO_LLEGO"))
            .andReturn().getResponse().getContentAsString(), "$[0].id");
    }

    @Test
    void soloUnAdministradorEntra() throws Exception {
        for (String ruta : new String[] {"/api/admin/reportes", "/api/admin/revisiones", "/api/admin/usuarios"}) {
            pedir(ruta, ana.token()).andExpect(status().isForbidden());
            pedir(ruta, admin.token()).andExpect(status().isOk());
        }
        enviarComo(ana.token(), "/api/admin/usuarios/" + ana.id() + "/reactivar", "").andExpect(status().isForbidden());
    }

    @Test
    void retirarLaPenalizacionTerminaLaPausaYLaVeQuienLaPidio() throws Exception {
        String id = revisionAbierta();
        pedir("/api/usuarios/yo/resumen?modo=TUTOR", ana.token()).andExpect(jsonPath("$.suspendidaHasta").exists());

        enviarComo(admin.token(), "/api/admin/revisiones/" + id + "/aceptar", "").andExpect(status().isNoContent());

        pedir("/api/usuarios/yo/resumen?modo=TUTOR", ana.token()).andExpect(jsonPath("$.suspendidaHasta").doesNotExist())
            .andExpect(jsonPath("$.penalizaciones").value(0));
        pedir("/api/admin/revisiones", admin.token()).andExpect(jsonPath("$").isEmpty());
        enviarComo(admin.token(), "/api/admin/revisiones/" + id + "/rechazar", "").andExpect(status().isConflict())
            .andExpect(jsonPath("$.codigo").value("REVISION_NO_ABIERTA"));
    }

    @Test
    void mantenerLaPenalizacionNoCambiaLaPausa() throws Exception {
        String id = revisionAbierta();
        enviarComo(admin.token(), "/api/admin/revisiones/" + id + "/rechazar", "").andExpect(status().isNoContent());
        pedir("/api/usuarios/yo/resumen?modo=TUTOR", ana.token()).andExpect(jsonPath("$.suspendidaHasta").exists())
            .andExpect(jsonPath("$.penalizaciones").value(1));
    }

    @Test
    void unReporteSeVeYSeCierraUnaVez() throws Exception {
        enviarComo(vale.token(), "/api/tutores/" + ana.id() + "/reportes", "{\"motivo\":\"No llegó y no avisó\"}").andExpect(status().isCreated());
        String lista = pedir("/api/admin/reportes", admin.token()).andExpect(jsonPath("$[0].tutor").value("Ana Torres"))
            .andExpect(jsonPath("$[0].reportante").value("Valeria Ríos")).andExpect(jsonPath("$[0].cerradoEn").doesNotExist())
            .andReturn().getResponse().getContentAsString();
        String id = campo(lista, "$[0].id");

        enviarComo(admin.token(), "/api/admin/reportes/" + id + "/cerrar", "").andExpect(status().isNoContent());
        pedir("/api/admin/reportes", admin.token()).andExpect(jsonPath("$[0].cerradoEn").exists());
        enviarComo(admin.token(), "/api/admin/reportes/" + id + "/cerrar", "").andExpect(status().isConflict());
        // Cerrado el anterior, la misma persona puede volver a reportar
        enviarComo(vale.token(), "/api/tutores/" + ana.id() + "/reportes", "{\"motivo\":\"Volvió a pasar\"}").andExpect(status().isCreated());
    }

    @Test
    void suspenderDejaSinAccesoYSaleDelCatalogoReactivarLoDevuelve() throws Exception {
        enviarComo(admin.token(), "/api/admin/usuarios/" + ana.id() + "/suspender", "").andExpect(status().isNoContent());
        em.clear();
        pedir("/api/usuarios/yo", ana.token()).andExpect(status().isUnauthorized());
        pedir("/api/tutores?tamano=50", vale.token()).andExpect(jsonPath("$.elementos[*].id", not(hasItem(ana.id()))));
        pedir("/api/admin/usuarios?q=ana", admin.token()).andExpect(jsonPath("$[0].estadoCuenta").value("SUSPENDIDA"));
        enviarComo(admin.token(), "/api/admin/usuarios/" + ana.id() + "/suspender", "").andExpect(status().isConflict());

        enviarComo(admin.token(), "/api/admin/usuarios/" + ana.id() + "/reactivar", "").andExpect(status().isNoContent());
        em.clear();
        pedir("/api/usuarios/yo", ana.token()).andExpect(status().isOk());
    }

    @Test
    void unaCuentaDeAdministradorNoSeSuspendeNiApareceYLaBusquedaNoUsaComodines() throws Exception {
        enviarComo(admin.token(), "/api/admin/usuarios/" + admin.id() + "/suspender", "").andExpect(status().isConflict());
        pedir("/api/admin/usuarios?q=Administraci", admin.token()).andExpect(jsonPath("$").isEmpty());
        pedir("/api/admin/usuarios?q=%25", admin.token()).andExpect(jsonPath("$").isEmpty());
    }
}
