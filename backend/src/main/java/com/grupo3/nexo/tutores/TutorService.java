package com.grupo3.nexo.tutores;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.grupo3.nexo.config.HoraColombia;
import com.grupo3.nexo.disponibilidad.DisponibilidadService;
import com.grupo3.nexo.error.ErrorNegocio;
import com.grupo3.nexo.tutores.TutorDtos.ReporteDto;
import com.grupo3.nexo.tutores.TutorDtos.ResenaDto;
import com.grupo3.nexo.tutores.TutorDtos.ResumenResenas;
import com.grupo3.nexo.tutores.TutorDtos.Tutor;
import com.grupo3.nexo.usuarios.Usuario;
import com.grupo3.nexo.usuarios.UsuarioRepository;
import com.grupo3.nexo.usuarios.UsuarioService;

/** RF3, RF4 y RF11: catálogo y perfil de tutores, sus reseñas y los reportes. */
@Service
public class TutorService {

    private final BusquedaTutores busqueda;
    private final UsuarioService cuentas;
    private final UsuarioRepository usuarios;
    private final ResenaRepository resenas;
    private final ReporteRepository reportes;
    private final DisponibilidadService disponibilidad;
    private final Clock reloj;

    public TutorService(BusquedaTutores busqueda, UsuarioService cuentas, UsuarioRepository usuarios,
                        ResenaRepository resenas, ReporteRepository reportes, DisponibilidadService disponibilidad,
                        Clock reloj) {
        this.busqueda = busqueda;
        this.cuentas = cuentas;
        this.usuarios = usuarios;
        this.resenas = resenas;
        this.reportes = reportes;
        this.disponibilidad = disponibilidad;
        this.reloj = reloj;
    }

    @Transactional(readOnly = true)
    public PaginaDto<Tutor> buscar(UUID usuarioId, FiltrosTutores filtros, int pagina, int tamano) {
        cuentas.cuentaEnSesion(usuarioId);
        PaginaDto.validar(pagina, tamano);
        Instant conectadosDesde = reloj.instant().minus(Usuario.VENTANA_EN_LINEA);
        long total = busqueda.contar(usuarioId, filtros, conectadosDesde);
        List<UUID> ids = busqueda.pagina(usuarioId, filtros, conectadosDesde, pagina, tamano);
        return PaginaDto.de(tutores(ids), pagina, tamano, total);
    }

    @Transactional(readOnly = true)
    public Tutor detalle(UUID usuarioId, String tutorId) {
        cuentas.cuentaEnSesion(usuarioId);
        return tutores(List.of(cuentas.tutorPublico(tutorId).getId())).get(0);
    }

    @Transactional(readOnly = true)
    public PaginaDto<ResenaDto> resenas(UUID usuarioId, String tutorId, int pagina, int tamano) {
        cuentas.cuentaEnSesion(usuarioId);
        UUID tutor = cuentas.tutorPublico(tutorId).getId();
        PaginaDto.validar(pagina, tamano);
        Page<ResenaDto> resultado = resenas.deTutor(tutor, PageRequest.of(pagina, tamano));
        return PaginaDto.de(resultado.getContent(), pagina, tamano, resultado.getTotalElements());
    }

    /** Un choque simultáneo con otro reporte igual lo traduce a REPORTE_DUPLICADO {@code ManejadorErrores}. */
    @Transactional
    public ReporteDto reportar(UUID usuarioId, String tutorId, String motivo) {
        cuentas.cuentaEnSesion(usuarioId);
        UUID tutor = cuentas.tutorPublico(tutorId).getId();
        if (tutor.equals(usuarioId)) {
            throw new ErrorNegocio(HttpStatus.UNPROCESSABLE_ENTITY, "REPORTE_A_SI_MISMO", "No puedes reportarte a ti mismo");
        }
        if (reportes.existsByReportanteIdAndTutorIdAndAbiertoTrue(usuarioId, tutor)) {
            throw new ErrorNegocio(HttpStatus.CONFLICT, "REPORTE_DUPLICADO", "Ya tienes un reporte abierto sobre este tutor");
        }
        Reporte reporte = reportes.saveAndFlush(new Reporte(tutor, usuarioId, motivo, reloj.instant()));
        return new ReporteDto(reporte.getId(), HoraColombia.momento(reporte.getCreadoEn()));
    }

    /** Arma los tutores en el orden de `ids`, con una consulta por cada dato en vez de una por tutor. */
    private List<Tutor> tutores(List<UUID> ids) {
        if (ids.isEmpty()) {
            return List.of();
        }
        Map<UUID, Usuario> porId = usuarios.findByIdIn(ids).stream().collect(Collectors.toMap(Usuario::getId, Function.identity()));
        Map<UUID, ResumenResenas> resumenes = resenas.resumenes(ids).stream()
            .collect(Collectors.toMap(ResumenResenas::tutorId, Function.identity()));
        var horarios = disponibilidad.horarios(ids);
        Instant ahora = reloj.instant();

        return ids.stream().map(id -> {
            Usuario u = porId.get(id);
            ResumenResenas resumen = resumenes.get(id);
            // TODO(fase de sesiones): contar sesionesTotales y estudiantes con las sesiones completadas del tutor
            return new Tutor(id, u.getNombre(), u.getHandle(), textoOVacio(u.getBio()), textoOVacio(u.getUniversidad()),
                u.getMaterias(),
                resumen == null ? 0 : Math.round(resumen.promedio() * 10) / 10.0,
                resumen == null ? 0 : resumen.total(),
                0, 0,
                u.getPrecioHora() == null ? 0 : u.getPrecioHora().doubleValue(),
                u.estaEnLinea(ahora),
                horarios.getOrDefault(id, List.of()));
        }).toList();
    }

    private static String textoOVacio(String texto) {
        return texto == null ? "" : texto;
    }
}
