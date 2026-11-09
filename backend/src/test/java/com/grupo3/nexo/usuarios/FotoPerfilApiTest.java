package com.grupo3.nexo.usuarios;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

import com.grupo3.nexo.auth.PruebaAuthBase;

/** /api/usuarios/yo/foto: la foto de perfil propia, que la app sube antes a Cloudinary. */
class FotoPerfilApiTest extends PruebaAuthBase {

    private static final String CORREO = "valeria@ejemplo.com";
    private static final String FOTO = "https://res.cloudinary.com/nexo/image/upload/v1/perfil.jpg";

    private String token() throws Exception {
        return campo(cuentaActiva(CORREO), "$.tokenAcceso");
    }

    private ResultActions yo(String token) throws Exception {
        return mvc.perform(get("/api/usuarios/yo").header("Authorization", "Bearer " + token));
    }

    private ResultActions ponerFoto(String token, String url) throws Exception {
        return mvc.perform(put("/api/usuarios/yo/foto").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"%s\"}".formatted(url)));
    }

    private ResultActions quitarFoto(String token) throws Exception {
        return mvc.perform(delete("/api/usuarios/yo/foto").header("Authorization", "Bearer " + token));
    }

    @Test
    void exigeToken() throws Exception {
        mvc.perform(put("/api/usuarios/yo/foto").contentType(MediaType.APPLICATION_JSON).content("{\"url\":\"%s\"}".formatted(FOTO)))
            .andExpect(status().isUnauthorized());
        mvc.perform(delete("/api/usuarios/yo/foto")).andExpect(status().isUnauthorized());
    }

    @Test
    void unaCuentaNuevaNoTieneFoto() throws Exception {
        yo(token()).andExpect(status().isOk()).andExpect(jsonPath("$.fotoUrl").doesNotExist());
    }

    @Test
    void guardaLaFotoDeCloudinaryYLaDevuelveEnElPerfil() throws Exception {
        String token = token();
        ponerFoto(token, FOTO).andExpect(status().isOk()).andExpect(jsonPath("$.fotoUrl").value(FOTO));
        yo(token).andExpect(jsonPath("$.fotoUrl").value(FOTO));
    }

    @Test
    void rechazaDireccionesQueNoSonDeCloudinary() throws Exception {
        String token = token();
        for (String url : java.util.List.of("https://ejemplo.com/foto.jpg", "http://res.cloudinary.com/nexo/foto.jpg",
                "https://res.cloudinary.com.malo.com/foto.jpg", "https://res.cloudinary.com/", "")) {
            ponerFoto(token, url).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.codigo").value("DATOS_INVALIDOS"));
        }
        mvc.perform(put("/api/usuarios/yo/foto").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content("{}")).andExpect(status().isBadRequest());
        yo(token).andExpect(jsonPath("$.fotoUrl").doesNotExist());
    }

    @Test
    void quitarlaDejaElPerfilSinFoto() throws Exception {
        String token = token();
        ponerFoto(token, FOTO).andExpect(status().isOk());
        quitarFoto(token).andExpect(status().isOk()).andExpect(jsonPath("$.fotoUrl").doesNotExist());
        yo(token).andExpect(jsonPath("$.fotoUrl").doesNotExist());
        // Quitarla otra vez no es un error
        quitarFoto(token).andExpect(status().isOk());
    }
}
