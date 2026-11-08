package com.grupo3.nexo.error;

import java.util.List;
import java.util.Locale;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.PessimisticLockingFailureException;
import jakarta.persistence.LockTimeoutException;
import jakarta.persistence.PersistenceException;
import jakarta.persistence.PessimisticLockException;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.UncategorizedSQLException;
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
     * Dos peticiones simultáneas que escriben lo mismo: gana una y la otra choca con una restricción, que aquí se vuelve
     * el error del contrato. Cualquier otro choque (un handle, una materia, una transición ya hecha) se puede reintentar.
     */
    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<ErrorApi> integridad(DataIntegrityViolationException e) {
        String causa = sinDetalle(e.getMostSpecificCause().getMessage());
        log.warn("Violación de integridad: {}", causa);
        String restriccion = causa.toLowerCase(Locale.ROOT);
        ErrorApi error;
        if (restriccion.contains("uq_usuarios_correo")) {
            error = ErrorApi.de("CORREO_DUPLICADO", "Ya existe una cuenta con ese correo");
        } else if (restriccion.contains("uq_reportes_abierto")) {
            error = ErrorApi.de("REPORTE_DUPLICADO", "Ya tienes un reporte abierto sobre este tutor");
        } else if (restriccion.contains("uq_solicitudes_activa")) {
            error = ErrorApi.de("SOLICITUD_DUPLICADA", "Ya tienes una solicitud con este tutor para ese día y hora");
        } else if (restriccion.contains("tg_sesiones_agenda")) {
            error = ErrorApi.de("HORA_NO_DISPONIBLE", "Tú o el estudiante ya tienen otra tutoría a esa hora");
        } else if (restriccion.contains("ex_sesiones_tutor_sin_solape")) {
            error = ErrorApi.de("HORA_NO_DISPONIBLE", "Ya tienes otra tutoría a esa hora");
        } else if (restriccion.contains("ex_sesiones_estudiante_sin_solape")) {
            error = ErrorApi.de("HORA_NO_DISPONIBLE", "El estudiante ya tiene otra tutoría a esa hora");
        } else if (restriccion.contains("uq_calificaciones_sesion_sentido")) {
            error = ErrorApi.de("SESION_YA_CALIFICADA", "Ya calificaste esta sesión");
        } else if (restriccion.contains("tg_mensajes_chat_activo")) {
            error = ErrorApi.de("CHAT_SOLO_LECTURA", "La sesión terminó y este chat solo conserva el historial");
        } else {
            error = ErrorApi.de("CONFLICTO", "Otra operación cambió estos datos al mismo tiempo; inténtalo de nuevo");
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(error);
    }

    /** Lo mismo cuando la espera vence en un bloqueo de JPA (login y códigos): fuera de un repositorio nadie la traduce. */
    @ExceptionHandler({ PessimisticLockException.class, LockTimeoutException.class })
    ResponseEntity<ErrorApi> bloqueoJpa(PersistenceException e) {
        log.warn("Conflicto de bloqueo: {}", sinDetalle(e.getMessage()));
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorApi.de("CONFLICTO", "Otra operación cambió estos datos al mismo tiempo; inténtalo de nuevo"));
    }

    /** JdbcClient no clasifica una espera vencida por `lock_timeout` (55P03): es el mismo caso que el anterior. */
    @ExceptionHandler(UncategorizedSQLException.class)
    ResponseEntity<ErrorApi> sqlSinClasificar(UncategorizedSQLException e) {
        if (e.getSQLException() == null || !"55P03".equals(e.getSQLException().getSQLState())) {
            throw e;
        }
        log.warn("Conflicto de bloqueo: {}", sinDetalle(e.getSQLException().getMessage()));
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorApi.de("CONFLICTO", "Otra operación cambió estos datos al mismo tiempo; inténtalo de nuevo"));
    }

    /**
     * Solo la primera línea, que ya nombra la restricción. Las siguientes (`Detail:`) pueden traer la fila completa, con
     * correo y hash de contraseña, y eso no debe llegar a los registros.
     */
    static String sinDetalle(String mensaje) {
        return String.valueOf(mensaje).lines().findFirst().orElse("");
    }

    /** La base abortó una transacción por un interbloqueo o una espera vencida: nada quedó a medias y se puede repetir. */
    @ExceptionHandler(PessimisticLockingFailureException.class)
    ResponseEntity<ErrorApi> bloqueo(PessimisticLockingFailureException e) {
        log.warn("Conflicto de bloqueo: {}", sinDetalle(e.getMostSpecificCause().getMessage()));
        return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ErrorApi.de("CONFLICTO", "Otra operación cambió estos datos al mismo tiempo; inténtalo de nuevo"));
    }
}
