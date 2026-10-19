package com.grupo3.nexo.error;

import java.util.List;

/** Cuerpo de toda respuesta de error; coincide con el esquema `Error` del contrato. */
public record ErrorApi(String codigo, String mensaje, List<Detalle> detalles) {

    public record Detalle(String campo, String problema) {
    }

    public static ErrorApi de(String codigo, String mensaje) {
        return new ErrorApi(codigo, mensaje, List.of());
    }
}
