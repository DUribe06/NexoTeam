package com.grupo3.nexo.error;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Traduce las excepciones al esquema de error del contrato. */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(ErrorNegocio.class)
    ResponseEntity<ErrorApi> negocio(ErrorNegocio e) {
        return ResponseEntity.status(e.getEstado()).body(new ErrorApi(e.getCodigo(), e.getMessage(), e.getDetalles()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorApi> validacion(MethodArgumentNotValidException e) {
        List<ErrorApi.Detalle> detalles = e.getBindingResult().getFieldErrors().stream()
            .map(f -> new ErrorApi.Detalle(f.getField(), f.getDefaultMessage()))
            .toList();
        return ResponseEntity.badRequest()
            .body(new ErrorApi("DATOS_INVALIDOS", "Hay datos incorrectos en la solicitud", detalles));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    ResponseEntity<ErrorApi> cuerpoIlegible(HttpMessageNotReadableException e) {
        return ResponseEntity.badRequest()
            .body(ErrorApi.de("DATOS_INVALIDOS", "El cuerpo de la solicitud no es un JSON válido"));
    }

    /** Dos registros simultáneos con el mismo correo: gana uno y el otro choca con la restricción única. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorApi> integridad(DataIntegrityViolationException e) {
        log.warn("Violación de integridad: {}", e.getMostSpecificCause().getMessage());
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorApi.de("CORREO_DUPLICADO", "Ya existe una cuenta con ese correo"));
    }
}
