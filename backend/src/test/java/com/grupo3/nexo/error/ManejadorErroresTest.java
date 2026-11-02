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
        assertEquals("REPORTE_DUPLICADO", codigo("Unique index violation: PUBLIC.UQ_REPORTES_ABIERTO_INDEX_6"));
    }

    @Test
    void soloElChoqueConElCorreoEsUnCorreoDuplicado() {
        assertEquals("CORREO_DUPLICADO",
            codigo("ERROR: duplicate key value violates unique constraint \"uq_usuarios_correo\""));
        assertEquals("CORREO_DUPLICADO", codigo("Unique index violation: PUBLIC.UQ_USUARIOS_CORREO_INDEX_D"));
        assertEquals("CONFLICTO", codigo("ERROR: duplicate key value violates unique constraint \"uq_usuarios_handle\""));
        assertEquals("CONFLICTO", codigo("ERROR: duplicate key value violates unique constraint \"tutor_materias_pkey\""));
    }
}
