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

/** Las migraciones deben impedir en la base lo que las reglas de RF1, RF2 y D2 y el módulo Usuario prohíben. */
@SpringBootTest
@Transactional
class EsquemaUsuariosTest {

    private static final String TERMINOS = "2026-10-18";

    @Autowired
    JdbcTemplate jdbc;

    private UUID usuario(String correo, String rol, boolean perfilTutor, String terminos) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
            INSERT INTO usuarios (id, nombre, correo, contrasena_hash, handle, rol, estado_cuenta,
                                  perfil_tutor_activo, terminos_version, terminos_aceptados_en)
            VALUES (?, 'Prueba', ?, 'hash', ?, ?, 'PENDIENTE_VERIFICACION_CORREO', ?, ?,
                    CASE WHEN CAST(? AS VARCHAR) IS NULL THEN NULL ELSE CURRENT_TIMESTAMP END)
            """, id, correo, "@" + id.toString().substring(0, 18), rol, perfilTutor, terminos, terminos);
        return id;
    }

    private UUID estudiante(String correo) {
        return usuario(correo, "ESTUDIANTE", false, TERMINOS);
    }

    private UUID tutor(String correo) {
        return usuario(correo, "TUTOR", true, TERMINOS);
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
        assertThrows(DataIntegrityViolationException.class, () -> usuario("a@ejemplo.com", "SUPERUSUARIO", false, TERMINOS));
        UUID id = estudiante("b@ejemplo.com");
        assertThrows(DataIntegrityViolationException.class,
            () -> jdbc.update("UPDATE usuarios SET estado_cuenta = 'INVENTADO' WHERE id = ?", id));
    }

    @Test
    void enModoTutorSiempreHayPerfilYQuienVuelveAEstudianteLoConserva() {
        assertDoesNotThrow(() -> tutor("t@ejemplo.com"));
        assertThrows(DataIntegrityViolationException.class, () -> usuario("t2@ejemplo.com", "TUTOR", false, TERMINOS));
        assertDoesNotThrow(() -> usuario("e@ejemplo.com", "ESTUDIANTE", true, TERMINOS));
    }

    @Test
    void elHandleEsUnicoYObligatorio() {
        UUID id = estudiante("h1@ejemplo.com");
        UUID otro = estudiante("h2@ejemplo.com");
        jdbc.update("UPDATE usuarios SET handle = '@repetido' WHERE id = ?", id);
        assertThrows(DataIntegrityViolationException.class,
            () -> jdbc.update("UPDATE usuarios SET handle = '@repetido' WHERE id = ?", otro));
        assertThrows(DataIntegrityViolationException.class,
            () -> jdbc.update("UPDATE usuarios SET handle = NULL WHERE id = ?", otro));
    }

    @Test
    void laTarifaNoPuedeSerNegativa() {
        UUID id = tutor("p@ejemplo.com");
        assertDoesNotThrow(() -> jdbc.update("UPDATE usuarios SET precio_hora = 0 WHERE id = ?", id));
        assertThrows(DataIntegrityViolationException.class,
            () -> jdbc.update("UPDATE usuarios SET precio_hora = -1 WHERE id = ?", id));
    }

    @Test
    void exigeElConsentimientoSalvoParaElAdministrador() {
        assertThrows(DataIntegrityViolationException.class, () -> usuario("sin@ejemplo.com", "ESTUDIANTE", false, null));
        assertDoesNotThrow(() -> usuario("admin@ejemplo.com", "ADMIN", false, null));
    }

    @Test
    void noPermiteIntentosFallidosNegativos() {
        UUID id = estudiante("c@ejemplo.com");
        assertThrows(DataIntegrityViolationException.class,
            () -> jdbc.update("UPDATE usuarios SET intentos_fallidos = -1 WHERE id = ?", id));
    }

    @Test
    void guardaLasMateriasDelTutorSinRepetirlas() {
        UUID id = tutor("tutor@ejemplo.com");
        jdbc.update("INSERT INTO tutor_materias (usuario_id, materia) VALUES (?, 'Cálculo I')", id);
        assertThrows(DataIntegrityViolationException.class,
            () -> jdbc.update("INSERT INTO tutor_materias (usuario_id, materia) VALUES (?, 'Cálculo I')", id));
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
        UUID id = tutor("e2@ejemplo.com");
        jdbc.update("INSERT INTO tutor_materias (usuario_id, materia) VALUES (?, 'Física I')", id);
        jdbc.update("INSERT INTO codigos_verificacion (id, usuario_id, proposito, codigo_hash, vence_en) "
            + "VALUES (?, ?, 'VERIFICAR_CORREO', 'hash', CURRENT_TIMESTAMP)", UUID.randomUUID(), id);

        jdbc.update("DELETE FROM usuarios WHERE id = ?", id);

        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM codigos_verificacion WHERE usuario_id = ?", Integer.class, id));
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM tutor_materias WHERE usuario_id = ?", Integer.class, id));
    }
}
