package com.grupo3.nexo.auth;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.grupo3.nexo.correo.DominiosDesechables;
import com.grupo3.nexo.correo.VerificadorDominioCorreo;
import com.grupo3.nexo.error.ErrorNegocio;

/** Reglas de D2: se acepta cualquier correo mientras su dominio exista y no sea de correo temporal. */
@Component
public class ValidadorCorreo {

    private final DominiosDesechables desechables;
    private final VerificadorDominioCorreo verificador;

    public ValidadorCorreo(DominiosDesechables desechables, VerificadorDominioCorreo verificador) {
        this.desechables = desechables;
        this.verificador = verificador;
    }

    /** Evita la consulta de red: para rechazar antes lo que ya se sabe que no sirve. */
    public void validarDominioNoDesechable(String correo) {
        if (desechables.esDesechable(dominioDe(correo))) {
            throw new ErrorNegocio(HttpStatus.UNPROCESSABLE_ENTITY, "CORREO_DESECHABLE",
                "No se aceptan correos temporales; usa tu correo personal o institucional");
        }
    }

    public void validarQueElDominioRecibeCorreo(String correo) {
        if (!verificador.dominioRecibeCorreo(dominioDe(correo))) {
            throw new ErrorNegocio(HttpStatus.UNPROCESSABLE_ENTITY, "CORREO_SIN_MX",
                "El dominio de ese correo no existe o no recibe correo");
        }
    }

    static String dominioDe(String correo) {
        return correo.substring(correo.lastIndexOf('@') + 1);
    }
}
