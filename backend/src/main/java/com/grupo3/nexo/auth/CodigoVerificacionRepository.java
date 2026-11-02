package com.grupo3.nexo.auth;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, UUID> {

    List<CodigoVerificacion> findByUsuarioIdAndProposito(UUID usuarioId, PropositoCodigo proposito);

    List<CodigoVerificacion> findByUsuarioIdAndPropositoAndCerradoEnIsNull(UUID usuarioId, PropositoCodigo proposito);

    Optional<CodigoVerificacion> findFirstByUsuarioIdAndPropositoAndCerradoEnIsNullOrderByCreadoEnDesc(
        UUID usuarioId, PropositoCodigo proposito);

    Optional<CodigoVerificacion> findFirstByUsuarioIdAndPropositoOrderByCreadoEnDesc(UUID usuarioId, PropositoCodigo proposito);

    long countByUsuarioIdAndPropositoAndCreadoEnAfter(UUID usuarioId, PropositoCodigo proposito, Instant desde);
}
