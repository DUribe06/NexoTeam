package com.grupo3.nexo.chats;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonInclude;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Esquemas de chats del contrato. */
public final class ChatDtos {

    private ChatDtos() {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record ChatDto(UUID id, UUID contraparteId, String nombre, String ultimoMensaje, OffsetDateTime ultimoMensajeEn,
                          int noLeidos, boolean enLinea, boolean soloLectura, boolean comoTutor, String fotoUrl) {
    }

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record MensajeDto(UUID id, UUID chatId, String texto, String imagenUrl, OffsetDateTime enviadoEn, boolean mio) {
    }

    public record PaginaMensajes(List<MensajeDto> mensajes, boolean hayMas) {
    }

    /** Texto, imagen o los dos (la imagen se sube antes a Cloudinary). Sin ninguno de los dos no hay mensaje. */
    public record SolicitudMensaje(@Size(max = 2000) String texto, UUID idCliente,
                                   @Size(max = 1000) @Pattern(regexp = "https://res\\.cloudinary\\.com/.+") String imagenUrl) {

        @AssertTrue(message = "escribe un mensaje o adjunta una imagen")
        public boolean isConContenido() {
            return !textoLimpio().isEmpty() || imagenUrl != null;
        }

        public String textoLimpio() {
            return texto == null ? "" : texto.trim();
        }
    }
}
