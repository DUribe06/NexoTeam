-- El estudiante elige cuánto dura la tutoría: 1 o 2 horas. El calendario va de dos en dos horas, así que una sesión de
-- 2 h termina justo cuando empieza la siguiente hora publicada y nunca la pisa.
ALTER TABLE solicitudes ADD COLUMN duracion_horas SMALLINT NOT NULL DEFAULT 1;
ALTER TABLE solicitudes ADD CONSTRAINT ck_solicitudes_duracion CHECK (duracion_horas IN (1, 2));
