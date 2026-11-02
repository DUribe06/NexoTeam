package com.grupo3.nexo.salud;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SaludControllerTest {

    @Autowired
    MockMvc mvc;

    @Test
    void saludEsPublico() throws Exception {
        mvc.perform(get("/api/salud"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.estado").value("ok"));
    }

    @Test
    void loDemasExigeAutenticacion() throws Exception {
        mvc.perform(get("/api/usuarios")).andExpect(status().isUnauthorized());
    }
}
