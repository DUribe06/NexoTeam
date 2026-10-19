package com.grupo3.nexo.error;

import java.util.List;

import org.springframework.http.HttpStatus;

/** Error esperado de una regla de negocio; el manejador lo convierte en la respuesta del contrato. */
public class ErrorNegocio extends RuntimeException {

    private final HttpStatus estado;
    private final String codigo;
    private final List<ErrorApi.Detalle> detalles;

    public ErrorNegocio(HttpStatus estado, String codigo, String mensaje) {
        this(estado, codigo, mensaje, List.of());
    }

    public ErrorNegocio(HttpStatus estado, String codigo, String mensaje, List<ErrorApi.Detalle> detalles) {
        super(mensaje);
        this.estado = estado;
        this.codigo = codigo;
        this.detalles = detalles;
    }

    public static ErrorNegocio datosInvalidos(String campo, String problema) {
        return new ErrorNegocio(HttpStatus.BAD_REQUEST, "DATOS_INVALIDOS", "Hay datos incorrectos en la solicitud",
            List.of(new ErrorApi.Detalle(campo, problema)));
    }

    public HttpStatus getEstado() { return estado; }
    public String getCodigo() { return codigo; }
    public List<ErrorApi.Detalle> getDetalles() { return detalles; }
}
