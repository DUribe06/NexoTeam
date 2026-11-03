package com.grupo3.nexo.error;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;

class ManejadorErroresTest {

    private String codigo(String causa) {
        var e = new DataIntegrityViolationException("choque", new RuntimeException(causa));
        var respuesta = new ManejadorErrores().integridad(e);
        assertEquals(409, respuesta.getStatusCode().value());
        return respuesta.getBody().codigo();
    }

    @Test
    void elChoqueConUnReporteAbiertoEsUnReporteDuplicado() {
        assertEquals("REPORTE_DUPLICADO",
            codigo("ERROR: duplicate key value violates unique constraint \"uq_reportes_abierto\""));
    }

    @Test
    void cadaChoqueDeTutoriasEsElErrorDelContrato() {
        assertEquals("SOLICITUD_DUPLICADA", codigo("ERROR: duplicate key value violates unique constraint \"uq_solicitudes_activa\""));
        assertEquals("HORA_NO_DISPONIBLE", codigo("ERROR: conflicting key value violates exclusion constraint \"ex_sesiones_tutor_sin_solape\""));
        assertEquals("HORA_NO_DISPONIBLE", codigo("ERROR: conflicting key value violates exclusion constraint \"ex_sesiones_estudiante_sin_solape\""));
        assertEquals("SESION_YA_CALIFICADA", codigo("ERROR: duplicate key value violates unique constraint \"uq_calificaciones_sesion_sentido\""));
        assertEquals("CHAT_SOLO_LECTURA", codigo("ERROR: tg_mensajes_chat_activo: el chat está en solo lectura"));
    }

    @Test
    void soloElChoqueConElCorreoEsUnCorreoDuplicado() {
        assertEquals("CORREO_DUPLICADO",
            codigo("ERROR: duplicate key value violates unique constraint \"uq_usuarios_correo\""));
        assertEquals("CONFLICTO", codigo("ERROR: duplicate key value violates unique constraint \"uq_usuarios_handle\""));
        assertEquals("CONFLICTO", codigo("ERROR: duplicate key value violates unique constraint \"tutor_materias_pkey\""));
    }

    @Test
    void losRegistrosNoLlevanLaFilaDeLaBase() {
        String mensaje = "ERROR: new row violates check constraint \"ck_usuarios_rol\"\n  Detail: Failing row contains (a@b.co, $2a$hash)";
        assertEquals("ERROR: new row violates check constraint \"ck_usuarios_rol\"", ManejadorErrores.sinDetalle(mensaje));
    }
}
