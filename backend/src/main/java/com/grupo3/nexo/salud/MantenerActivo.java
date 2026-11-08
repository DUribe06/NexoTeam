package com.grupo3.nexo.salud;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * El plan gratis de Render apaga el servicio tras 15 minutos sin peticiones entrantes. Si pasan 13 sin ninguna, el
 * servicio se llama a sí mismo por su dirección pública: solo así cuenta como tráfico entrante. La llamada vuelve a
 * pasar por este filtro, y eso reinicia la cuenta. No toca la base, para que Neon pueda dormirse.
 */
@Component
class MantenerActivo extends OncePerRequestFilter {

    static final Duration SILENCIO_MAXIMO = Duration.ofMinutes(13);

    private static final Logger log = LoggerFactory.getLogger(MantenerActivo.class);
    private static final Duration TIEMPO_DE_ESPERA = Duration.ofSeconds(30);

    private final HttpClient cliente = HttpClient.newBuilder().connectTimeout(TIEMPO_DE_ESPERA).build();
    private final String url;
    private final Clock reloj;
    private volatile Instant ultimaActividad;

    /** @param url dirección pública del servicio; vacía fuera de Render, y entonces nunca llama. */
    MantenerActivo(@Value("${nexo.mantener-activo.url:}") String url, Clock reloj) {
        this.url = url;
        this.reloj = reloj;
        this.ultimaActividad = reloj.instant();
    }

    @Override
    protected void doFilterInternal(HttpServletRequest peticion, HttpServletResponse respuesta, FilterChain cadena)
            throws ServletException, IOException {
        ultimaActividad = reloj.instant();
        cadena.doFilter(peticion, respuesta);
    }

    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.MINUTES)
    void llamarSiHaceFalta() {
        if (url.isBlank() || Duration.between(ultimaActividad, reloj.instant()).compareTo(SILENCIO_MAXIMO) < 0) {
            return;
        }
        HttpRequest llamada = HttpRequest.newBuilder(URI.create(url + "/api/salud")).timeout(TIEMPO_DE_ESPERA).build();
        try {
            int estado = cliente.send(llamada, HttpResponse.BodyHandlers.discarding()).statusCode();
            if (estado != 200) {
                log.warn("El aviso para mantener activo el servicio respondió {}", estado);
            }
        } catch (IOException e) {
            // La cuenta no se reinició, así que se reintenta en un minuto
            log.warn("No se pudo enviar el aviso para mantener activo el servicio: {}", e.toString());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
