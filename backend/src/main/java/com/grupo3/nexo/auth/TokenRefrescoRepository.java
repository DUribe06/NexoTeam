package com.grupo3.nexo.auth;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface TokenRefrescoRepository extends JpaRepository<TokenRefresco, UUID> {

    Optional<TokenRefresco> findByTokenHash(String tokenHash);

    @Modifying
    @Query("DELETE FROM TokenRefresco t WHERE t.usuarioId = :usuarioId AND t.venceEn < :ahora")
    void borrarVencidos(@Param("usuarioId") UUID usuarioId, @Param("ahora") Instant ahora);

    /** Con la fila bloqueada: de dos refrescos simultáneos con el mismo token, el segundo lo ve ya usado. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT t FROM TokenRefresco t WHERE t.tokenHash = :tokenHash")
    Optional<TokenRefresco> buscarParaConsumir(@Param("tokenHash") String tokenHash);

    List<TokenRefresco> findByUsuarioIdAndRevocadoEnIsNull(UUID usuarioId);
}
