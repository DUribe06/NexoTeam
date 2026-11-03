package com.grupo3.nexo.disponibilidad;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grupo3.nexo.config.HoraColombia;
import com.grupo3.nexo.disponibilidad.DisponibilidadDtos.CalendarioCreado;
import com.grupo3.nexo.disponibilidad.DisponibilidadDtos.DiaDisponibilidad;
import com.grupo3.nexo.disponibilidad.DisponibilidadDtos.DiaSolicitable;
import com.grupo3.nexo.disponibilidad.DisponibilidadDtos.FranjaTutor;
import com.grupo3.nexo.disponibilidad.DisponibilidadDtos.HoraDisponible;
import com.grupo3.nexo.disponibilidad.DisponibilidadDtos.SemanaDisponibilidad;
import com.grupo3.nexo.disponibilidad.DisponibilidadDtos.SolicitudDefinirHora;
import com.grupo3.nexo.disponibilidad.DisponibilidadDtos.SolicitudDefinirSemana;
import com.grupo3.nexo.error.ErrorNegocio;
import com.grupo3.nexo.usuarios.Modalidad;
import com.grupo3.nexo.usuarios.Usuario;
import com.grupo3.nexo.usuarios.UsuarioService;

/** RF7: el calendario semanal del tutor. Es la única fuente de su horario público. */
@Service
public class DisponibilidadService {

    private static final int DIAS_SOLICITABLES = 14;

    private final HoraPublicadaRepository horas;
    private final UsuarioService usuarios;
    private final JdbcClient jdbc;
    private final Clock reloj;

    public DisponibilidadService(HoraPublicadaRepository horas, UsuarioService usuarios, JdbcClient jdbc, Clock reloj) {
        this.horas = horas;
        this.usuarios = usuarios;
        this.jdbc = jdbc;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public SemanaDisponibilidad propia(UUID usuarioId) {
        return semanaDe(usuarios.tutorEnSesion(usuarioId));
    }

    @Transactional
    public CalendarioCreado crear(UUID usuarioId) {
        Usuario tutor = usuarios.tutorEnSesion(usuarioId);
        boolean nuevo = !tutor.tieneCalendario();
        tutor.crearCalendario(reloj.instant());
        return new CalendarioCreado(nuevo, semanaDe(tutor));
    }

    /** Solo toca las filas que cambian: borrar e insertar la misma fila en una transacción chocaría con el UNIQUE. */
    @Transactional
    public SemanaDisponibilidad definirHora(UUID usuarioId, SolicitudDefinirHora s) {
        if (s.modalidades().size() != Set.copyOf(s.modalidades()).size()) {
            throw ErrorNegocio.datosInvalidos("modalidades", "no se pueden repetir");
        }
        Usuario tutor = usuarios.tutorEnSesion(usuarioId);
        LocalTime hora = LocalTime.parse(s.hora());
        if (!HorasCalendario.HORAS.contains(hora)) {
            throw new ErrorNegocio(HttpStatus.UNPROCESSABLE_ENTITY, "HORA_NO_VALIDA",
                "Esa hora no está en el calendario; usa 08:00, 10:00, 12:00, 14:00, 16:00, 18:00 o 20:00");
        }
        if (!tutor.tieneCalendario()) {
            throw new ErrorNegocio(HttpStatus.CONFLICT, "CALENDARIO_NO_CREADO", "Primero crea tu calendario");
        }

        List<HoraPublicada> actuales = horas.findByUsuarioIdAndDiaAndHora(usuarioId, s.dia(), hora);
        actuales.stream().filter(h -> !s.modalidades().contains(h.getModalidad())).forEach(horas::delete);
        Set<Modalidad> yaPublicadas = actuales.stream().map(HoraPublicada::getModalidad).collect(Collectors.toSet());
        s.modalidades().stream().filter(m -> !yaPublicadas.contains(m))
            .forEach(m -> horas.save(new HoraPublicada(usuarioId, s.dia(), hora, m)));
        return semanaDe(tutor);
    }

    /**
     * Reemplaza la semana en una sola transacción: o queda toda o no cambia nada. Como {@link #definirHora}, solo toca
     * las filas que cambian. Crea el calendario si hacía falta, para que el tutor no tenga que pedirlo aparte.
     */
    @Transactional
    public SemanaDisponibilidad definirSemana(UUID usuarioId, SolicitudDefinirSemana s) {
        Usuario tutor = usuarios.tutorEnSesion(usuarioId);
        record Fila(DiaSemana dia, LocalTime hora, Modalidad modalidad) {
        }
        Set<Fila> deseadas = new HashSet<>();
        Set<String> franjas = new HashSet<>();
        for (SolicitudDefinirHora h : s.horas()) {
            LocalTime hora = LocalTime.parse(h.hora());
            if (!HorasCalendario.HORAS.contains(hora)) {
                throw new ErrorNegocio(HttpStatus.UNPROCESSABLE_ENTITY, "HORA_NO_VALIDA",
                    "Esa hora no está en el calendario; usa 08:00, 10:00, 12:00, 14:00, 16:00, 18:00 o 20:00");
            }
            if (!franjas.add(h.dia() + " " + hora)) {
                throw ErrorNegocio.datosInvalidos("horas", "no se puede repetir un día y una hora");
            }
            if (h.modalidades().size() != Set.copyOf(h.modalidades()).size()) {
                throw ErrorNegocio.datosInvalidos("modalidades", "no se pueden repetir");
            }
            h.modalidades().forEach(m -> deseadas.add(new Fila(h.dia(), hora, m)));
        }
        if (!tutor.tieneCalendario()) {
            tutor.crearCalendario(reloj.instant());
        }

        Set<Fila> yaPublicadas = new HashSet<>();
        for (HoraPublicada actual : horas.findByUsuarioId(usuarioId)) {
            Fila fila = new Fila(actual.getDia(), actual.getHora(), actual.getModalidad());
            if (deseadas.contains(fila)) {
                yaPublicadas.add(fila);
            } else {
                horas.delete(actual);
            }
        }
        deseadas.stream().filter(f -> !yaPublicadas.contains(f))
            .forEach(f -> horas.save(new HoraPublicada(usuarioId, f.dia(), f.hora(), f.modalidad())));
        return semanaDe(tutor);
    }

    /** Los próximos 14 días desde mañana con alguna hora publicada, sin las que ya choquen con una sesión programada. */
    @Transactional(readOnly = true)
    public List<DiaSolicitable> solicitables(UUID usuarioId, String tutorId) {
        usuarios.cuentaEnSesion(usuarioId);
        Usuario tutor = usuarios.tutorPublico(tutorId);
        Map<DiaSemana, Map<LocalTime, Set<Modalidad>>> publicadas = agrupar(horas.findByUsuarioId(tutor.getId()));

        LocalDate hoy = HoraColombia.hoy(reloj);
        List<Sesion> ocupadas = sesionesProgramadas(tutor.getId());
        List<DiaSolicitable> dias = new ArrayList<>();
        for (int i = 1; i <= DIAS_SOLICITABLES; i++) {
            LocalDate fecha = hoy.plusDays(i);
            Map<LocalTime, Set<Modalidad>> delDia = publicadas.get(DiaSemana.de(fecha));
            if (delDia != null) {
                List<HoraDisponible> libres = delDia.entrySet().stream()
                    .filter(e -> ocupadas.stream().noneMatch(s -> s.chocaCon(fecha, e.getKey())))
                    .map(e -> new HoraDisponible(HorasCalendario.texto(e.getKey()), List.copyOf(e.getValue())))
                    .toList();
                if (!libres.isEmpty()) {
                    dias.add(new DiaSolicitable(fecha, libres));
                }
            }
        }
        return dias;
    }

    private record Sesion(Instant inicio, Instant fin) {

        /** Cada hora del calendario dura 1 h. */
        boolean chocaCon(LocalDate fecha, LocalTime hora) {
            Instant desde = fecha.atTime(hora).atZone(HoraColombia.ZONA).toInstant();
            return inicio.isBefore(desde.plus(Duration.ofHours(1))) && fin.isAfter(desde);
        }
    }

    private List<Sesion> sesionesProgramadas(UUID tutor) {
        return jdbc.sql("SELECT inicio, fin FROM sesiones WHERE (tutor_id = ? OR estudiante_id = ?) AND estado = 'PROGRAMADA'").params(tutor, tutor)
            .query((rs, n) -> new Sesion(rs.getObject("inicio", OffsetDateTime.class).toInstant(),
                rs.getObject("fin", OffsetDateTime.class).toInstant()))
            .list();
    }

    /** El horario público de cada tutor: solo las horas publicadas, en orden de calendario. */
    @Transactional(readOnly = true)
    public Map<UUID, List<FranjaTutor>> horarios(Collection<UUID> tutorIds) {
        return horas.findByUsuarioIdIn(tutorIds).stream()
            .collect(Collectors.groupingBy(HoraPublicada::getUsuarioId,
                Collectors.collectingAndThen(Collectors.toList(), DisponibilidadService::horarioDe)));
    }

    private static List<FranjaTutor> horarioDe(List<HoraPublicada> delTutor) {
        List<FranjaTutor> horario = new ArrayList<>();
        agrupar(delTutor).forEach((dia, delDia) -> delDia.forEach((hora, modalidades) ->
            horario.add(new FranjaTutor(dia, HorasCalendario.texto(hora), List.copyOf(modalidades)))));
        return horario;
    }

    private SemanaDisponibilidad semanaDe(Usuario tutor) {
        if (!tutor.tieneCalendario()) {
            return new SemanaDisponibilidad(false, List.of());
        }
        Map<DiaSemana, Map<LocalTime, Set<Modalidad>>> publicadas = agrupar(horas.findByUsuarioId(tutor.getId()));
        List<DiaDisponibilidad> dias = new ArrayList<>();
        for (DiaSemana dia : DiaSemana.values()) {
            Map<LocalTime, Set<Modalidad>> delDia = publicadas.getOrDefault(dia, Map.of());
            dias.add(new DiaDisponibilidad(dia, HorasCalendario.HORAS.stream()
                .map(h -> new HoraDisponible(HorasCalendario.texto(h), List.copyOf(delDia.getOrDefault(h, Set.of()))))
                .toList()));
        }
        return new SemanaDisponibilidad(true, dias);
    }

    /** Día → hora → modalidades. Los tres niveles quedan ordenados: los mapas por clave y el EnumSet por el enum. */
    private static Map<DiaSemana, Map<LocalTime, Set<Modalidad>>> agrupar(List<HoraPublicada> filas) {
        Map<DiaSemana, Map<LocalTime, Set<Modalidad>>> grupos = new EnumMap<>(DiaSemana.class);
        filas.forEach(f -> grupos.computeIfAbsent(f.getDia(), d -> new TreeMap<>())
            .computeIfAbsent(f.getHora(), h -> EnumSet.noneOf(Modalidad.class))
            .add(f.getModalidad()));
        return grupos;
    }
}
