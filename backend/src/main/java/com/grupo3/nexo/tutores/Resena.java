package com.grupo3.nexo.tutores;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Reseña de un estudiante a un tutor (RF4, RF9). Nace al calificar una sesión, y no se edita. */
@Entity
@Table(name = "resenas")
public class Resena {

    @Id
    private UUID id;

    @Column(name = "tutor_id")
    private UUID tutorId;

    @Column(name = "autor_id")
    private UUID autorId;

    /** La sesión calificada; vacía en los datos demo, que no vienen de una sesión. */
    @Column(name = "sesion_id")
    private UUID sesionId;

    private String texto;

    private int estrellas;

    private LocalDate fecha;

    @Column(name = "creado_en")
    private Instant creadoEn;

    protected Resena() {
    }

    public Resena(UUID tutorId, UUID autorId, UUID sesionId, String texto, int estrellas, LocalDate fecha, Instant creadoEn) {
        this.id = UUID.randomUUID();
        this.tutorId = tutorId;
        this.autorId = autorId;
        this.sesionId = sesionId;
        this.texto = texto;
        this.estrellas = estrellas;
        this.fecha = fecha;
        this.creadoEn = creadoEn;
    }

    public UUID getTutorId() { return tutorId; }
    public UUID getAutorId() { return autorId; }
    public UUID getSesionId() { return sesionId; }
    public String getTexto() { return texto; }
    public int getEstrellas() { return estrellas; }
    public LocalDate getFecha() { return fecha; }
}
