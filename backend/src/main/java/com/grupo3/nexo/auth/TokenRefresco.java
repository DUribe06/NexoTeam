package com.grupo3.nexo.auth;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Token de refresco: solo se guarda el hash SHA-256 del token, nunca el token. */
@Entity
@Table(name = "tokens_refresco")
public class TokenRefresco {

    @Id
    private UUID id;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(name = "token_hash")
    private String tokenHash;

    @Column(name = "vence_en")
    private Instant venceEn;

    @Column(name = "revocado_en")
    private Instant revocadoEn;

    @Column(name = "creado_en")
    private Instant creadoEn;

    protected TokenRefresco() {
    }

    public TokenRefresco(UUID usuarioId, String tokenHash, Instant venceEn, Instant creadoEn) {
        this.id = UUID.randomUUID();
        this.usuarioId = usuarioId;
        this.tokenHash = tokenHash;
        this.venceEn = venceEn;
        this.creadoEn = creadoEn;
    }

    public void revocar(Instant ahora) {
        if (revocadoEn == null) {
            revocadoEn = ahora;
        }
    }

    public boolean estaRevocado() { return revocadoEn != null; }
    public boolean estaVencido(Instant ahora) { return !ahora.isBefore(venceEn); }

    public UUID getUsuarioId() { return usuarioId; }
    public String getTokenHash() { return tokenHash; }
    public Instant getVenceEn() { return venceEn; }
}
