package com.grupo3.nexo.salud;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Comprobación de que el servicio está arriba; la app la usa para verificar la conexión. */
@RestController
public class SaludController {

    @GetMapping("/api/salud")
    public Map<String, String> salud() {
        return Map.of("estado", "ok", "servicio", "nexo-backend");
    }
}
