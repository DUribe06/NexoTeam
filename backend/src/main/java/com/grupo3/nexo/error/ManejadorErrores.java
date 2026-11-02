package com.grupo3.nexo.error;

import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

/** Traduce las excepciones al esquema de error del contrato. */
@RestControllerAdvice
public class ManejadorErrores {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErrores.class);

    @ExceptionHandler(ErrorNegocio.class)
    ResponseEntity<ErrorApi> negocio(ErrorNegocio e) {
        ResponseEntity.BodyBuilder respuesta = ResponseEntity.status(e.getEstado());
        if (e.getReintentarEnSegundos() != null) {
            respuesta.header("Retry-After", String.valueOf(e.getReintentarEnSegundos()));
        }
        return respuesta.body(new ErrorApi(e.getCodigo(), e.getMessage(), e.getDetalles()));
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

    /** Un parámetro de la URL con un valor que no corresponde, como `?modo=OTRO`. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorApi> parametroInvalido(MethodArgumentTypeMismatchException e) {
        return ResponseEntity.badRequest().body(new ErrorApi("DATOS_INVALIDOS", "Hay datos incorrectos en la solicitud",
            List.of(new ErrorApi.Detalle(e.getName(), "no es un valor válido"))));
    }

    /**
     * Dos peticiones simultáneas que escriben lo mismo: gana una y la otra choca con una restricción. Solo el choque
     * con el correo es un correo duplicado y el de un reporte abierto, un reporte duplicado; cualquier otro (un handle,
     * una materia) se puede reintentar.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorApi> integridad(DataIntegrityViolationException e) {
        String causa = String.valueOf(e.getMostSpecificCause().getMessage());
        log.warn("Violación de integridad: {}", causa);
        String restriccion = causa.toLowerCase(Locale.ROOT);
        ErrorApi error;
        if (restriccion.contains("uq_usuarios_correo")) {
            error = ErrorApi.de("CORREO_DUPLICADO", "Ya existe una cuenta con ese correo");
        } else if (restriccion.contains("uq_reportes_abierto")) {
            error = ErrorApi.de("REPORTE_DUPLICADO", "Ya tienes un reporte abierto sobre este tutor");
        } else {
            error = ErrorApi.de("CONFLICTO", "Otra operación cambió estos datos al mismo tiempo; inténtalo de nuevo");
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }
}
