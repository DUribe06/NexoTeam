package com.grupo3.nexo.tutores;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Reporte a un tutor (RF11). Queda abierto hasta que lo revise el administrador (RF12). */
@Entity
@Table(name = "reportes")
public class Reporte {

    @Id
    private UUID id;

    @Column(name = "tutor_id")
    private UUID tutorId;

    @Column(name = "reportante_id")
    private UUID reportanteId;

    private String motivo;

    @Column(name = "creado_en")
    private Instant creadoEn;

    /** Vacío mientras sigue abierto. */
    @Column(name = "cerrado_en")
    private Instant cerradoEn;

    protected Reporte() {
    }

    Reporte(UUID tutorId, UUID reportanteId, String motivo, Instant creadoEn) {
        this.id = UUID.randomUUID();
        this.tutorId = tutorId;
        this.reportanteId = reportanteId;
        this.motivo = motivo;
        this.creadoEn = creadoEn;
    }

    public UUID getId() { return id; }
    public Instant getCreadoEn() { return creadoEn; }
}
