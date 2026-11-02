package com.grupo3.nexo.auth;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.grupo3.nexo.error.ErrorNegocio;

class PoliticaContrasenaTest {

    private final PoliticaContrasena politica = new PoliticaContrasena();

    private ErrorNegocio rechaza(String contrasena) {
        return assertThrows(ErrorNegocio.class, () -> politica.validar(contrasena, "maria.perez@ejemplo.com"));
    }

    @Test
    void aceptaUnaContrasenaConLetrasYNumeros() {
        assertDoesNotThrow(() -> politica.validar("Clave2026seg", "maria.perez@ejemplo.com"));
    }

    @Test
    void rechazaLasQueNoTienenLetraONumero() {
        assertEquals("CONTRASENA_DEBIL", rechaza("soloLetrasAqui").getCodigo());
        assertEquals("CONTRASENA_DEBIL", rechaza("98765432101").getCodigo());
    }

    @Test
    void rechazaLasCortasYLasMasComunes() {
        assertEquals("CONTRASENA_DEBIL", rechaza("Ab1").getCodigo());
        assertEquals("CONTRASENA_DEBIL", rechaza("Password123").getCodigo());
        assertEquals("CONTRASENA_DEBIL", rechaza("QWERTY123").getCodigo());
    }

    @Test
    void rechazaLasQueContienenElUsuarioDelCorreo() {
        assertEquals("CONTRASENA_DEBIL", rechaza("maria.perez2026").getCodigo());
    }

    @Test
    void rechazaLasQueSuperanLos72BytesDeBcrypt() {
        ErrorNegocio e = rechaza("a1" + "ñ".repeat(40));
        assertEquals(HttpStatus.BAD_REQUEST, e.getEstado());
        assertEquals("DATOS_INVALIDOS", e.getCodigo());
    }
}
