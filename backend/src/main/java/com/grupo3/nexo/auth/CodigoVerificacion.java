package com.grupo3.nexo.auth;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Código de 6 dígitos de un solo uso. Solo se guarda su hash. */
@Entity
@Table(name = "codigos_verificacion")
public class CodigoVerificacion {

    @Id
    private UUID id;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    private PropositoCodigo proposito;

    @Column(name = "codigo_hash")
    private String codigoHash;

    private int intentos;

    @Column(name = "vence_en")
    private Instant venceEn;

    @Column(name = "cerrado_en")
    private Instant cerradoEn;

    @Column(name = "creado_en")
    private Instant creadoEn;

    protected CodigoVerificacion() {
    }

    public CodigoVerificacion(UUID usuarioId, PropositoCodigo proposito, String codigoHash, Instant venceEn, Instant creadoEn) {
        this.id = UUID.randomUUID();
        this.usuarioId = usuarioId;
        this.proposito = proposito;
        this.codigoHash = codigoHash;
        this.venceEn = venceEn;
        this.creadoEn = creadoEn;
    }

    public void registrarIntentoFallido() {
        intentos++;
    }

    /** Se cierra al usarse, al vencer, al agotar los intentos o al pedir uno nuevo: un código sirve una sola vez. */
    public void cerrar(Instant ahora) {
        if (cerradoEn == null) {
            cerradoEn = ahora;
        }
    }

    public UUID getId() { return id; }
    public UUID getUsuarioId() { return usuarioId; }
    public PropositoCodigo getProposito() { return proposito; }
    public String getCodigoHash() { return codigoHash; }
    public int getIntentos() { return intentos; }
    public Instant getVenceEn() { return venceEn; }
    public Instant getCerradoEn() { return cerradoEn; }
    public Instant getCreadoEn() { return creadoEn; }
}
