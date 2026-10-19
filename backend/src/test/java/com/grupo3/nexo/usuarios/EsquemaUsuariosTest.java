package com.grupo3.nexo.usuarios;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** La migración V2 debe impedir en la base lo que las reglas de RF1, RF2 y D2 prohíben. */
@SpringBootTest
@Transactional
class EsquemaUsuariosTest {

    private static final String TERMINOS = "2026-10-18";

    @Autowired
    JdbcTemplate jdbc;

    private UUID usuario(String correo, String rol, String estadoTutor, String terminos) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO usuarios (id, nombre, correo, contrasena_hash, rol, estado_cuenta,
                                  estado_verificacion_tutor, terminos_version, terminos_aceptados_en)
            VALUES (?, 'Prueba', ?, 'hash', ?, 'PENDIENTE_VERIFICACION_CORREO', ?, ?,
                    CASE WHEN ? IS NULL THEN NULL ELSE CURRENT_TIMESTAMP END)
            """, id, correo, rol, estadoTutor, terminos, terminos);
        return id;
    }

    private UUID estudiante(String correo) {
        return usuario(correo, "ESTUDIANTE", null, TERMINOS);
    }

    @Test
    void guardaUnEstudianteConValoresPorDefecto() {
        UUID id = estudiante("ana@ejemplo.com");
        assertEquals(0, jdbc.queryForObject("SELECT intentos_fallidos FROM usuarios WHERE id = ?", Integer.class, id));
        assertEquals(1, jdbc.queryForObject(
            "SELECT COUNT(*) FROM usuarios WHERE id = ? AND creado_en IS NOT NULL AND bloqueado_hasta IS NULL",
            Integer.class, id));
    }

    @Test
    void noPermiteDosCuentasConElMismoCorreo() {
        estudiante("repetido@ejemplo.com");
        assertThrows(DataIntegrityViolationException.class, () -> estudiante("repetido@ejemplo.com"));
    }

    @Test
    void exigeElCorreoNormalizadoEnMinusculasYSinEspacios() {
        assertThrows(DataIntegrityViolationException.class, () -> estudiante("Ana@Ejemplo.com"));
        assertThrows(DataIntegrityViolationException.class, () -> estudiante(" ana@ejemplo.com"));
    }

    @Test
    void rechazaRolesYEstadosInventados() {
        assertThrows(DataIntegrityViolationException.class, () -> usuario("a@ejemplo.com", "SUPERUSUARIO", null, TERMINOS));
        UUID id = estudiante("b@ejemplo.com");
        assertThrows(DataIntegrityViolationException.class,
            () -> jdbc.update("UPDATE usuarios SET estado_cuenta = 'INVENTADO' WHERE id = ?", id));
    }

    @Test
    void unTutorSiempreTieneEstadoDeVerificacionYLosDemasNunca() {
        assertDoesNotThrow(() -> usuario("t@ejemplo.com", "TUTOR", "PENDIENTE", TERMINOS));
        assertThrows(DataIntegrityViolationException.class, () -> usuario("t2@ejemplo.com", "TUTOR", null, TERMINOS));
        assertThrows(DataIntegrityViolationException.class, () -> usuario("e@ejemplo.com", "ESTUDIANTE", "VERIFICADO", TERMINOS));
    }

    @Test
    void exigeElConsentimientoSalvoParaElAdministrador() {
        assertThrows(DataIntegrityViolationException.class, () -> usuario("sin@ejemplo.com", "ESTUDIANTE", null, null));
        assertDoesNotThrow(() -> usuario("admin@ejemplo.com", "ADMIN", null, null));
    }

    @Test
    void noPermiteIntentosFallidosNegativos() {
        UUID id = estudiante("c@ejemplo.com");
        assertThrows(DataIntegrityViolationException.class,
            () -> jdbc.update("UPDATE usuarios SET intentos_fallidos = -1 WHERE id = ?", id));
    }

    @Test
    void guardaMateriasYModalidadesDelTutorSinRepetirlas() {
        UUID id = usuario("tutor@ejemplo.com", "TUTOR", "PENDIENTE", TERMINOS);
        jdbc.update("INSERT INTO tutor_materias (usuario_id, materia) VALUES (?, 'Cálculo I')", id);
        jdbc.update("INSERT INTO tutor_modalidades (usuario_id, modalidad) VALUES (?, 'VIRTUAL')", id);
        assertThrows(DataIntegrityViolationException.class,
            () -> jdbc.update("INSERT INTO tutor_materias (usuario_id, materia) VALUES (?, 'Cálculo I')", id));
        assertThrows(DataIntegrityViolationException.class,
            () -> jdbc.update("INSERT INTO tutor_modalidades (usuario_id, modalidad) VALUES (?, 'HOLOGRAMA')", id));
    }

    @Test
    void elCodigoSoloAceptaPropositosConocidosYUsuariosExistentes() {
        UUID id = estudiante("d@ejemplo.com");
        String sql = "INSERT INTO codigos_verificacion (id, usuario_id, proposito, codigo_hash, vence_en) "
            + "VALUES (?, ?, ?, 'hash', CURRENT_TIMESTAMP)";
        assertDoesNotThrow(() -> jdbc.update(sql, UUID.randomUUID(), id, "VERIFICAR_CORREO"));
        assertThrows(DataIntegrityViolationException.class, () -> jdbc.update(sql, UUID.randomUUID(), id, "OTRO"));
        assertThrows(DataIntegrityViolationException.class,
            () -> jdbc.update(sql, UUID.randomUUID(), UUID.randomUUID(), "VERIFICAR_CORREO"));
    }

    @Test
    void alBorrarUnUsuarioSeBorranSusCodigosYDatosDeTutor() {
        UUID id = usuario("e2@ejemplo.com", "TUTOR", "PENDIENTE", TERMINOS);
        jdbc.update("INSERT INTO tutor_materias (usuario_id, materia) VALUES (?, 'Física I')", id);
        jdbc.update("INSERT INTO codigos_verificacion (id, usuario_id, proposito, codigo_hash, vence_en) "
            + "VALUES (?, ?, 'VERIFICAR_CORREO', 'hash', CURRENT_TIMESTAMP)", UUID.randomUUID(), id);

        jdbc.update("DELETE FROM usuarios WHERE id = ?", id);

        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM codigos_verificacion WHERE usuario_id = ?", Integer.class, id));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM tutor_materias WHERE usuario_id = ?", Integer.class, id));
    }
}
