package com.grupo3.nexo.correo;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/** Lista de dominios de correo temporal (correo/dominios-desechables.txt). También cubre sus subdominios. */
@Component
public class DominiosDesechables {

    private final Set<String> dominios;

    public DominiosDesechables() {
        try (BufferedReader lector = new BufferedReader(new InputStreamReader(
            new ClassPathResource("correo/dominios-desechables.txt").getInputStream(), StandardCharsets.UTF_8))) {
            this.dominios = lector.lines()
                .map(String::trim)
                .filter(l -> !l.isEmpty() && !l.startsWith("#"))
                .collect(Collectors.toUnmodifiableSet());
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la lista de dominios desechables", e);
        }
    }

    public boolean esDesechable(String dominio) {
        String d = dominio.toLowerCase();
        while (true) {
            if (dominios.contains(d)) {
                return true;
            }
            int punto = d.indexOf('.');
            if (punto < 0) {
                return false;
            }
            d = d.substring(punto + 1);
        }
    }
}
