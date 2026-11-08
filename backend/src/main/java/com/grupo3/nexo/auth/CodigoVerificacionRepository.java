package com.grupo3.nexo.auth;

import java.time.Instant;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, UUID> {

    @Modifying
    @Query("DELETE FROM CodigoVerificacion c WHERE c.usuarioId = :usuarioId AND c.creadoEn < :antesDe")
    void borrarAntiguos(@Param("usuarioId") UUID usuarioId, @Param("antesDe") Instant antesDe);

    List<CodigoVerificacion> findByUsuarioIdAndProposito(UUID usuarioId, PropositoCodigo proposito);

    List<CodigoVerificacion> findByUsuarioIdAndPropositoAndCerradoEnIsNull(UUID usuarioId, PropositoCodigo proposito);

    Optional<CodigoVerificacion> findFirstByUsuarioIdAndPropositoAndCerradoEnIsNullOrderByCreadoEnDesc(
        UUID usuarioId, PropositoCodigo proposito);

    Optional<CodigoVerificacion> findFirstByUsuarioIdAndPropositoOrderByCreadoEnDesc(UUID usuarioId, PropositoCodigo proposito);

    long countByUsuarioIdAndPropositoAndCreadoEnAfter(UUID usuarioId, PropositoCodigo proposito, Instant desde);
}
