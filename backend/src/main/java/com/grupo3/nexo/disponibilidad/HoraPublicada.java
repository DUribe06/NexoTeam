package com.grupo3.nexo.disponibilidad;

import java.time.LocalTime;
import java.util.UUID;

import com.grupo3.nexo.usuarios.Modalidad;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Una hora de la semana ofrecida en una modalidad. Una hora sin filas no está publicada. */
@Entity
@Table(name = "tutor_disponibilidad")
public class HoraPublicada {

    @Id
    private UUID id;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    private DiaSemana dia;

    private LocalTime hora;

    @Enumerated(EnumType.STRING)
    private Modalidad modalidad;

    protected HoraPublicada() {
    }

    public HoraPublicada(UUID usuarioId, DiaSemana dia, LocalTime hora, Modalidad modalidad) {
        this.id = UUID.randomUUID();
        this.usuarioId = usuarioId;
        this.dia = dia;
        this.hora = hora;
        this.modalidad = modalidad;
    }

    public UUID getUsuarioId() { return usuarioId; }
    public DiaSemana getDia() { return dia; }
    public LocalTime getHora() { return hora; }
    public Modalidad getModalidad() { return modalidad; }
}
