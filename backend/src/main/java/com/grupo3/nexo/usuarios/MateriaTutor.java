package com.grupo3.nexo.usuarios;

import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

/** Una materia del tutor con su lugar en la lista: la primera es la que la app propone al solicitar una tutoría. */
@Embeddable
public class MateriaTutor {

    @Column(name = "materia")
    private String materia;

    @Column(name = "posicion")
    private int posicion;

    protected MateriaTutor() {
    }

    MateriaTutor(String materia, int posicion) {
        this.materia = materia;
        this.posicion = posicion;
    }

    public String getMateria() { return materia; }
    public int getPosicion() { return posicion; }

    @Override
    public boolean equals(Object otro) {
        return otro instanceof MateriaTutor m && posicion == m.posicion && materia.equals(m.materia);
    }

    @Override
    public int hashCode() {
        return Objects.hash(materia, posicion);
    }
}
