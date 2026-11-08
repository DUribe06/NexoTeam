-- RF7: la disponibilidad es la única fuente del horario público del tutor. Cada fila es una hora de la semana
-- publicada en una modalidad, y una hora sin filas no está publicada. Por eso las modalidades ya no se guardan aparte:
-- tutor_modalidades nunca se llenó (el registro dejó de pedir datos de tutor) y sería un segundo modelo del horario.
DROP TABLE tutor_modalidades;

-- El tutor ya creó su calendario (POST /api/disponibilidad/yo). Vacío hasta entonces: "Agregar horario" en la app.
ALTER TABLE usuarios ADD COLUMN calendario_creado_en TIMESTAMP WITH TIME ZONE;

CREATE TABLE tutor_disponibilidad (
    id          UUID         PRIMARY KEY,
    usuario_id  UUID         NOT NULL,
    dia         VARCHAR(10)  NOT NULL,
    hora        TIME         NOT NULL,
    modalidad   VARCHAR(20)  NOT NULL,

    CONSTRAINT fk_tutor_disponibilidad_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT uq_tutor_disponibilidad UNIQUE (usuario_id, dia, hora, modalidad),
    CONSTRAINT ck_tutor_disponibilidad_dia
        CHECK (dia IN ('LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', 'SABADO', 'DOMINGO')),
    -- Las únicas horas del calendario, cada una de 1 h
    CONSTRAINT ck_tutor_disponibilidad_hora CHECK (hora IN (
        TIME '08:00:00', TIME '10:00:00', TIME '12:00:00', TIME '14:00:00',
        TIME '16:00:00', TIME '18:00:00', TIME '20:00:00')),
    CONSTRAINT ck_tutor_disponibilidad_modalidad CHECK (modalidad IN ('PRESENCIAL', 'VIRTUAL'))
);
