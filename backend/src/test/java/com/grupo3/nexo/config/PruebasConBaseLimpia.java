package com.grupo3.nexo.config;

import java.util.HashSet;
import java.util.Set;

import org.flywaydb.core.Flyway;
import org.springframework.boot.flyway.autoconfigure.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Cada corrida parte de un esquema vacío, una sola vez por esquema: varios contextos de Spring lo comparten dentro de
 * la misma corrida. La guarda impide que una configuración equivocada limpie un esquema que no es de pruebas.
 */
@Configuration
class PruebasConBaseLimpia {

    private static final Set<String> LIMPIOS = new HashSet<>();

    @Bean
    FlywayMigrationStrategy limpiarYMigrar() {
        return flyway -> {
            String esquema = esquemaDe(flyway);
            if (!esquema.startsWith("pruebas")) {
                throw new IllegalStateException("Las pruebas no limpian el esquema " + esquema);
            }
            if (LIMPIOS.add(esquema)) {
                flyway.clean();
            }
            flyway.migrate();
        };
    }

    private static String esquemaDe(Flyway flyway) {
        return flyway.getConfiguration().getDefaultSchema();
    }
}
