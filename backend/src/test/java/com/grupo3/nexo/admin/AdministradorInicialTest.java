package com.grupo3.nexo.admin;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.security.crypto.password.PasswordEncoder;

import com.grupo3.nexo.auth.PruebaAuthBase;

/** Sin esta cuenta nadie podría entrar al panel: el registro público nunca da el rol de administrador. */
class AdministradorInicialTest extends PruebaAuthBase {

    private static final String CORREO = "admin.inicial@ejemplo.com";

    @Autowired private JdbcClient cliente;
    @Autowired private PasswordEncoder encriptador;

    private void arrancarCon(String correo, String contrasena) {
        new AdministradorInicial(cliente, encriptador, correo, contrasena).run(null);
    }

    @Test
    void creaLaCuentaYPuedeEntrar() throws Exception {
        arrancarCon("  Admin.Inicial@Ejemplo.com ", "Panel2026seg");
        login(CORREO, "Panel2026seg").andExpect(status().isOk()).andExpect(jsonPath("$.usuario.rol").value("ADMIN"));
    }

    @Test
    void siYaExisteNoCambiaLaContrasena() throws Exception {
        arrancarCon(CORREO, "Panel2026seg");
        arrancarCon(CORREO, "OtraClave2026");
        login(CORREO, "OtraClave2026").andExpect(status().isUnauthorized());
        login(CORREO, "Panel2026seg").andExpect(status().isOk());
    }

    @Test
    void sinVariablesNoCreaNada() {
        Integer antes = jdbc.queryForObject("SELECT COUNT(*) FROM usuarios", Integer.class);
        arrancarCon("", "");
        arrancarCon(CORREO, "");
        Assertions.assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM usuarios", Integer.class)).isEqualTo(antes);
    }
}
