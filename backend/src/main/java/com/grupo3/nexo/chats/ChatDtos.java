package com.grupo3.nexo.chats;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Esquemas de chats del contrato. */
public final class ChatDtos {

    private ChatDtos() {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ChatDto(UUID id, UUID contraparteId, String nombre, String ultimoMensaje, OffsetDateTime ultimoMensajeEn,
                          int noLeidos, boolean enLinea, boolean soloLectura, boolean comoTutor) {
    }

    public record MensajeDto(UUID id, UUID chatId, String texto, OffsetDateTime enviadoEn, boolean mio) {
    }

    public record PaginaMensajes(List<MensajeDto> mensajes, boolean hayMas) {
    }

    public record SolicitudMensaje(@NotBlank @Size(max = 2000) String texto, UUID idCliente) {
    }
}
