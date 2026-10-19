package com.grupo3.nexo.correo;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class DominiosDesechablesTest {

    private final DominiosDesechables dominios = new DominiosDesechables();

    @Test
    void reconoceLosDominiosDeLaLista() {
        assertTrue(dominios.esDesechable("mailinator.com"));
        assertTrue(dominios.esDesechable("YOPMAIL.COM"));
    }

    @Test
    void reconoceSusSubdominios() {
        assertTrue(dominios.esDesechable("cualquiera.mailinator.com"));
    }

    @Test
    void noMarcaCorreosNormalesNiDominiosParecidos() {
        assertFalse(dominios.esDesechable("gmail.com"));
        assertFalse(dominios.esDesechable("correo.uis.edu.co"));
        assertFalse(dominios.esDesechable("notmailinator.com"));
    }
}
