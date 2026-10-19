package com.grupo3.nexo.auth;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface TokenRefrescoRepository extends JpaRepository<TokenRefresco, UUID> {

    Optional<TokenRefresco> findByTokenHash(String tokenHash);

    List<TokenRefresco> findByUsuarioIdAndRevocadoEnIsNull(UUID usuarioId);
}
