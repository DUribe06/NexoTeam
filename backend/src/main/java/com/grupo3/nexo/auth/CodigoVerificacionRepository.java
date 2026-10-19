package com.grupo3.nexo.auth;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface CodigoVerificacionRepository extends JpaRepository<CodigoVerificacion, UUID> {

    List<CodigoVerificacion> findByUsuarioIdAndProposito(UUID usuarioId, PropositoCodigo proposito);
}
