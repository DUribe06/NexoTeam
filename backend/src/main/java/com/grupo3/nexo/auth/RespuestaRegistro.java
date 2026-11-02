package com.grupo3.nexo.auth;

import java.util.UUID;

import com.grupo3.nexo.usuarios.EstadoCuenta;

public record RespuestaRegistro(UUID usuarioId, String correo, EstadoCuenta estadoCuenta, int codigoVenceEnSegundos) {
}
