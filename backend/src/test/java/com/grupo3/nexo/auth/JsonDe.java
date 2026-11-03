package com.grupo3.nexo.auth;

import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

final class JsonDe {

    private JsonDe() {
    }

    static String campo(ResultActions respuesta, String ruta) throws Exception {
        return JsonPath.read(respuesta.andReturn().getResponse().getContentAsString(), ruta);
    }
}
