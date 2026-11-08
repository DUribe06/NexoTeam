package com.grupo3.nexo.salud;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.sun.net.httpserver.HttpServer;

class MantenerActivoTest {

    private final RelojManual reloj = new RelojManual();
    private final List<String> llamadas = new CopyOnWriteArrayList<>();
    private int estadoDeRespuesta = 200;
    private HttpServer servidor;
    private MantenerActivo mantener;

    @BeforeEach
    void arrancarServidor() throws Exception {
        servidor = HttpServer.create(new InetSocketAddress("localhost", 0), 0);
        servidor.createContext("/", intercambio -> {
            llamadas.add(intercambio.getRequestURI().getPath());
            intercambio.sendResponseHeaders(estadoDeRespuesta, -1);
            intercambio.close();
        });
        servidor.start();
        mantener = new MantenerActivo("http://localhost:" + servidor.getAddress().getPort(), reloj);
    }

    @AfterEach
    void detenerServidor() {
        servidor.stop(0);
    }

    @Test
    void noLlamaAntesDeTiempo() {
        reloj.avanzar(MantenerActivo.SILENCIO_MAXIMO.minusSeconds(1));

        mantener.llamarSiHaceFalta();

        assertThat(llamadas).isEmpty();
    }

    @Test
    void llamaALaSaludCuandoSeCumpleElSilencio() {
        reloj.avanzar(MantenerActivo.SILENCIO_MAXIMO);

        mantener.llamarSiHaceFalta();

        assertThat(llamadas).containsExactly("/api/salud");
    }

    @Test
    void unaPeticionReiniciaLaCuenta() throws Exception {
        reloj.avanzar(Duration.ofMinutes(12));
        mantener.doFilter(new MockHttpServletRequest(), new MockHttpServletResponse(), new MockFilterChain());

        reloj.avanzar(Duration.ofMinutes(12));
        mantener.llamarSiHaceFalta();
        assertThat(llamadas).isEmpty();

        reloj.avanzar(Duration.ofMinutes(1));
        mantener.llamarSiHaceFalta();
        assertThat(llamadas).hasSize(1);
    }

    @Test
    void siElAvisoNoLlegaReintentaSinRomperLaTarea() {
        reloj.avanzar(MantenerActivo.SILENCIO_MAXIMO);
        servidor.stop(0);

        mantener.llamarSiHaceFalta();

        assertThat(llamadas).isEmpty();
    }

    @Test
    void unaRespuestaDeErrorNoRompeLaTarea() {
        estadoDeRespuesta = 503;
        reloj.avanzar(MantenerActivo.SILENCIO_MAXIMO);

        mantener.llamarSiHaceFalta();

        assertThat(llamadas).hasSize(1);
    }

    @Test
    void sinDireccionNuncaLlama() {
        MantenerActivo apagado = new MantenerActivo("", reloj);
        reloj.avanzar(Duration.ofHours(1));

        apagado.llamarSiHaceFalta();

        assertThat(llamadas).isEmpty();
    }

    private static final class RelojManual extends Clock {

        private Instant ahora = Instant.parse("2026-11-05T12:00:00Z");

        void avanzar(Duration tiempo) {
            ahora = ahora.plus(tiempo);
        }

        @Override
        public Instant instant() {
            return ahora;
        }

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zona) {
            return this;
        }
    }
}
