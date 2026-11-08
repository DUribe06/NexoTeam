-- RF4 y RF9: reseñas que los estudiantes dejan a un tutor. El promedio y el total se calculan de aquí; no se guardan.
CREATE TABLE resenas (
    id         UUID                     PRIMARY KEY,
    tutor_id   UUID                     NOT NULL,
    autor_id   UUID                     NOT NULL,
    -- La sesión calificada. Sin clave foránea hasta que exista la tabla de sesiones; la conecta esa fase.
    -- Los datos demo no vienen de una sesión, por eso admite vacío.
    sesion_id  UUID,
    -- Vacío si el estudiante calificó sin comentario
    texto      VARCHAR(500)             NOT NULL DEFAULT '',
    estrellas  INTEGER                  NOT NULL,
    -- Día en que se publicó, en hora de Colombia
    fecha      DATE                     NOT NULL,
    creado_en  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_resenas_tutor FOREIGN KEY (tutor_id) REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT fk_resenas_autor FOREIGN KEY (autor_id) REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT ck_resenas_estrellas CHECK (estrellas BETWEEN 1 AND 5),
    CONSTRAINT ck_resenas_autor_distinto CHECK (autor_id <> tutor_id),
    -- Solo el estudiante publica reseña al calificar, y una sola vez por sesión (varios vacíos sí caben)
    CONSTRAINT uq_resenas_sesion UNIQUE (sesion_id)
);

CREATE INDEX ix_resenas_tutor_fecha ON resenas (tutor_id, fecha);

-- RF11: reportes a un tutor, para que los revise el administrador (RF12).
CREATE TABLE reportes (
    id             UUID                     PRIMARY KEY,
    tutor_id       UUID                     NOT NULL,
    reportante_id  UUID                     NOT NULL,
    motivo         VARCHAR(500)             NOT NULL,
    -- TRUE mientras está abierto y vacío al cerrarse. Así el UNIQUE, que ignora los vacíos, admite un solo reporte
    -- abierto por persona y tutor. H2 no tiene índices parciales, y esto vale igual en PostgreSQL y en H2.
    abierto        BOOLEAN                  DEFAULT TRUE,
    creado_en      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cerrado_en     TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_reportes_tutor FOREIGN KEY (tutor_id) REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT fk_reportes_reportante FOREIGN KEY (reportante_id) REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT uq_reportes_abierto UNIQUE (reportante_id, tutor_id, abierto),
    CONSTRAINT ck_reportes_abierto_o_cerrado CHECK (
        (abierto = TRUE AND cerrado_en IS NULL) OR (abierto IS NULL AND cerrado_en IS NOT NULL)),
    CONSTRAINT ck_reportes_a_otro CHECK (reportante_id <> tutor_id)
);

CREATE INDEX ix_reportes_tutor ON reportes (tutor_id);
