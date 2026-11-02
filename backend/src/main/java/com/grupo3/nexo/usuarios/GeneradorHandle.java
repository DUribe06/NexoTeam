package com.grupo3.nexo.usuarios;

import java.text.Normalizer;
import java.util.Locale;

import org.springframework.stereotype.Component;

/** Genera el nombre público de una cuenta a partir de su nombre, no del correo, que no debe quedar a la vista. */
@Component
public class GeneradorHandle {

    private static final int MAXIMO_BASE = 30;

    private final UsuarioRepository usuarios;

    public GeneradorHandle(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    /** "Valeria Ríos" da `@valeriarios`; si ya está en uso, `@valeriarios2`, `@valeriarios3`... */
    public String paraNombre(String nombre) {
        String base = "@" + limpiar(nombre);
        String candidato = base;
        for (int i = 2; usuarios.existsByHandle(candidato); i++) {
            candidato = base + i;
        }
        return candidato;
    }

    private static String limpiar(String nombre) {
        String limpio = Normalizer.normalize(nombre, Normalizer.Form.NFD)
            .replaceAll("\\p{M}", "")
            .toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]", "");
        if (limpio.isEmpty()) {
            return "usuario";
        }
        return limpio.substring(0, Math.min(limpio.length(), MAXIMO_BASE));
    }
}
