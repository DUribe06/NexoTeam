package com.grupo3.nexo.disponibilidad;

import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface HoraPublicadaRepository extends JpaRepository<HoraPublicada, UUID> {

    List<HoraPublicada> findByUsuarioId(UUID usuarioId);

    List<HoraPublicada> findByUsuarioIdIn(Collection<UUID> usuarioIds);

    List<HoraPublicada> findByUsuarioIdAndDiaAndHora(UUID usuarioId, DiaSemana dia, LocalTime hora);
}
