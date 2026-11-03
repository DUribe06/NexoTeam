package com.grupo3.nexo.chats;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.grupo3.nexo.chats.ChatDtos.ChatDto;
import com.grupo3.nexo.chats.ChatDtos.MensajeDto;
import com.grupo3.nexo.chats.ChatDtos.PaginaMensajes;
import com.grupo3.nexo.chats.ChatDtos.SolicitudMensaje;
import com.grupo3.nexo.usuarios.Modo;

import jakarta.validation.Valid;

/** Endpoints de /api/chats según el contrato (openapi/nexo-api.yaml). */
@RestController
@RequestMapping("/api/chats")
public class ChatController {

    private final ChatService chats;

    public ChatController(ChatService chats) {
        this.chats = chats;
    }

    @GetMapping
    public List<ChatDto> bandeja(@AuthenticationPrincipal Jwt jwt, @RequestParam(required = false) Modo modo) {
        return chats.bandeja(id(jwt), modo);
    }

    @GetMapping("/con/{personaId}")
    public ChatDto con(@AuthenticationPrincipal Jwt jwt, @PathVariable String personaId,
                       @RequestParam(required = false) Modo modo) {
        return chats.con(id(jwt), personaId, modo);
    }

    @GetMapping("/{chatId}")
    public ChatDto uno(@AuthenticationPrincipal Jwt jwt, @PathVariable String chatId) {
        return chats.uno(id(jwt), chatId);
    }

    @GetMapping("/{chatId}/mensajes")
    public PaginaMensajes mensajes(@AuthenticationPrincipal Jwt jwt, @PathVariable String chatId,
                                   @RequestParam(required = false) UUID antes,
                                   @RequestParam(required = false) UUID desde,
                                   @RequestParam(defaultValue = "50") int limite) {
        return chats.mensajes(id(jwt), chatId, antes, desde, limite);
    }

    @PostMapping("/{chatId}/mensajes")
    @ResponseStatus(HttpStatus.CREATED)
    public MensajeDto enviar(@AuthenticationPrincipal Jwt jwt, @PathVariable String chatId,
                             @Valid @RequestBody SolicitudMensaje s) {
        return chats.enviar(id(jwt), chatId, s);
    }

    @PostMapping("/{chatId}/leido")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void leido(@AuthenticationPrincipal Jwt jwt, @PathVariable String chatId) {
        chats.marcarLeido(id(jwt), chatId);
    }

    private static UUID id(Jwt jwt) {
        return UUID.fromString(jwt.getSubject());
    }
}
