-- Una sesión presencial solo empieza si los dos se encontraron: el tutor envía el PIN al llegar, el estudiante lo ve y se
-- lo dice, y el tutor lo escribe. Quien no aparece a los 10 minutos de la hora acordada recibe una penalización. Si
-- alguno va tarde, los dos pueden pactar por chat una nueva hora o cancelar; incumplir la nueva hora penaliza más grave.

-- El PIN va en texto plano porque la app debe mostrárselo al estudiante y solo sirve mientras dura la sesión.
-- gen_random_uuid() es aleatorio de calidad criptográfica, a diferencia de random().
ALTER TABLE sesiones
    ADD COLUMN pin CHAR(4) NOT NULL
        DEFAULT lpad(((hashtextextended(gen_random_uuid()::TEXT, 0) % 10000 + 10000) % 10000)::TEXT, 4, '0'),
    ADD COLUMN pin_enviado_en TIMESTAMPTZ,
    ADD COLUMN pin_intentos SMALLINT NOT NULL DEFAULT 0,
    ADD COLUMN pin_bloqueado_hasta TIMESTAMPTZ,
    ADD COLUMN iniciada_en TIMESTAMPTZ,
    -- Retardo pactado: la nueva hora y de quién es. La sesión termina a la misma hora: otra puede venir después.
    ADD COLUMN hora_pactada TIMESTAMPTZ,
    ADD COLUMN retardo_de UUID,
    -- Por qué se canceló, si no fue el estudiante quien lo hizo antes de la hora
    ADD COLUMN cierre VARCHAR(20),
    ADD CONSTRAINT fk_sesiones_retardo_de FOREIGN KEY (retardo_de) REFERENCES usuarios (id) ON DELETE RESTRICT,
    ADD CONSTRAINT ck_sesiones_pin CHECK (pin ~ '^[0-9]{4}$'),
    ADD CONSTRAINT ck_sesiones_pin_intentos CHECK (pin_intentos >= 0),
    ADD CONSTRAINT ck_sesiones_iniciada_con_pin CHECK (iniciada_en IS NULL OR pin_enviado_en IS NOT NULL),
    ADD CONSTRAINT ck_sesiones_retardo CHECK (
        (hora_pactada IS NULL AND retardo_de IS NULL)
        OR (hora_pactada > inicio AND hora_pactada < fin AND retardo_de IN (estudiante_id, tutor_id))),
    ADD CONSTRAINT ck_sesiones_cierre CHECK (
        cierre IS NULL OR (estado = 'CANCELADA' AND cierre IN ('TUTOR_NO_LLEGO', 'ESTUDIANTE_NO_LLEGO', 'ACORDADA')));

-- Hasta cuándo se puede empezar: 10 minutos después de la hora vigente o, si el PIN llegó más tarde, de que llegó. Así
-- un tutor que lo envía en el minuto 9 no deja al estudiante sin tiempo de decírselo. Nunca pasa del final de la sesión.
CREATE FUNCTION fn_sesion_limite(p_inicio TIMESTAMPTZ, p_fin TIMESTAMPTZ, p_hora_pactada TIMESTAMPTZ, p_pin_enviado_en TIMESTAMPTZ)
    RETURNS TIMESTAMPTZ LANGUAGE sql IMMUTABLE AS $$
    SELECT LEAST(p_fin, GREATEST(COALESCE(p_hora_pactada, p_inicio), p_pin_enviado_en) + INTERVAL '10 minutes')
$$;

-- La puesta al día busca las presenciales por vencer en cada petición; casi siempre no hay ninguna
CREATE INDEX ix_sesiones_sin_iniciar ON sesiones (inicio) WHERE estado = 'PROGRAMADA' AND iniciada_en IS NULL;

-- El PIN no cambia; se envía, se inicia y se pacta el retardo una sola vez y mientras está programada. Pasada la hora
-- solo se cancela con un motivo: lo pone la puesta al día o el acuerdo de los dos.
CREATE OR REPLACE FUNCTION fn_sesiones_transicion() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF (NEW.id, NEW.solicitud_id, NEW.estudiante_id, NEW.tutor_id, NEW.inicio, NEW.fin, NEW.creado_en, NEW.pin)
       IS DISTINCT FROM
       (OLD.id, OLD.solicitud_id, OLD.estudiante_id, OLD.tutor_id, OLD.inicio, OLD.fin, OLD.creado_en, OLD.pin) THEN
        RAISE EXCEPTION 'tg_sesiones_transicion: de una sesión solo cambia el estado'
            USING ERRCODE = 'check_violation', CONSTRAINT = 'tg_sesiones_transicion';
    END IF;
    IF NEW.pin_enviado_en IS DISTINCT FROM OLD.pin_enviado_en AND (OLD.pin_enviado_en IS NOT NULL OR OLD.estado <> 'PROGRAMADA') THEN
        RAISE EXCEPTION 'tg_sesiones_transicion: el PIN se envía una sola vez y mientras está programada'
            USING ERRCODE = 'check_violation', CONSTRAINT = 'tg_sesiones_transicion';
    END IF;
    IF NEW.iniciada_en IS DISTINCT FROM OLD.iniciada_en AND (OLD.iniciada_en IS NOT NULL OR OLD.estado <> 'PROGRAMADA') THEN
        RAISE EXCEPTION 'tg_sesiones_transicion: una sesión empieza una sola vez y mientras está programada'
            USING ERRCODE = 'check_violation', CONSTRAINT = 'tg_sesiones_transicion';
    END IF;
    IF (NEW.hora_pactada, NEW.retardo_de) IS DISTINCT FROM (OLD.hora_pactada, OLD.retardo_de)
       AND (OLD.hora_pactada IS NOT NULL OR OLD.estado <> 'PROGRAMADA' OR OLD.iniciada_en IS NOT NULL) THEN
        RAISE EXCEPTION 'tg_sesiones_transicion: el retardo se pacta una sola vez y antes de empezar'
            USING ERRCODE = 'check_violation', CONSTRAINT = 'tg_sesiones_transicion';
    END IF;
    IF NEW.cierre IS DISTINCT FROM OLD.cierre AND NOT (OLD.estado = 'PROGRAMADA' AND NEW.estado = 'CANCELADA') THEN
        RAISE EXCEPTION 'tg_sesiones_transicion: el motivo se pone al cancelar'
            USING ERRCODE = 'check_violation', CONSTRAINT = 'tg_sesiones_transicion';
    END IF;
    IF NEW.estado <> OLD.estado AND NOT (
           (OLD.estado = 'PROGRAMADA' AND NEW.estado = 'COMPLETADA' AND OLD.fin <= now())
        OR (OLD.estado = 'PROGRAMADA' AND NEW.estado = 'CANCELADA' AND OLD.iniciada_en IS NULL
            AND (OLD.inicio > now() OR NEW.cierre IS NOT NULL))
        OR (OLD.estado = 'PROGRAMADA' AND NEW.estado = 'NO_ASISTIO' AND OLD.fin <= now())
        OR (OLD.estado = 'COMPLETADA' AND NEW.estado = 'NO_ASISTIO')) THEN
        RAISE EXCEPTION 'tg_sesiones_transicion: % a % no es válido', OLD.estado, NEW.estado
            USING ERRCODE = 'check_violation', CONSTRAINT = 'tg_sesiones_transicion';
    END IF;
    RETURN NEW;
END $$;

-- ---------- Penalizaciones ----------
-- Una por sesión, a quien no llegó. GRAVE si ya había un retardo pactado. Es historial: no se cambia ni se borra.
CREATE TABLE penalizaciones (
    id         UUID                     NOT NULL PRIMARY KEY,
    usuario_id UUID                     NOT NULL,
    sesion_id  UUID                     NOT NULL,
    nivel      VARCHAR(10)              NOT NULL,
    creado_en  TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_penalizaciones_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE RESTRICT,
    CONSTRAINT fk_penalizaciones_sesion FOREIGN KEY (sesion_id) REFERENCES sesiones (id) ON DELETE RESTRICT,
    CONSTRAINT uq_penalizaciones_sesion UNIQUE (sesion_id),
    CONSTRAINT ck_penalizaciones_nivel CHECK (nivel IN ('NORMAL', 'GRAVE'))
);

CREATE INDEX ix_penalizaciones_usuario ON penalizaciones (usuario_id, creado_en DESC);

CREATE TRIGGER tg_penalizaciones_inmutables BEFORE UPDATE OR DELETE ON penalizaciones
    FOR EACH ROW EXECUTE FUNCTION fn_historial_inmutable();

-- ---------- Acuerdos por chat ----------
-- Uno propone y el otro acepta o rechaza. Rechazado se puede volver a proponer; aceptado, el retardo es uno solo.
CREATE TABLE acuerdos_sesion (
    id            UUID                     NOT NULL PRIMARY KEY,
    sesion_id     UUID                     NOT NULL,
    tipo          VARCHAR(15)              NOT NULL,
    propuesto_por UUID                     NOT NULL,
    nueva_hora    TIMESTAMP WITH TIME ZONE,
    estado        VARCHAR(10)              NOT NULL DEFAULT 'PROPUESTO',
    creado_en     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resuelto_en   TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_acuerdos_sesion FOREIGN KEY (sesion_id) REFERENCES sesiones (id) ON DELETE RESTRICT,
    CONSTRAINT fk_acuerdos_propuesto_por FOREIGN KEY (propuesto_por) REFERENCES usuarios (id) ON DELETE RESTRICT,
    CONSTRAINT ck_acuerdos_tipo CHECK (tipo IN ('RETARDO', 'CANCELACION')),
    CONSTRAINT ck_acuerdos_estado CHECK (estado IN ('PROPUESTO', 'ACEPTADO', 'RECHAZADO')),
    CONSTRAINT ck_acuerdos_nueva_hora CHECK ((tipo = 'RETARDO') = (nueva_hora IS NOT NULL)),
    CONSTRAINT ck_acuerdos_resuelto CHECK ((estado = 'PROPUESTO') = (resuelto_en IS NULL))
);

CREATE UNIQUE INDEX uq_acuerdos_uno_propuesto ON acuerdos_sesion (sesion_id) WHERE estado = 'PROPUESTO';
CREATE UNIQUE INDEX uq_acuerdos_un_retardo ON acuerdos_sesion (sesion_id) WHERE tipo = 'RETARDO' AND estado = 'ACEPTADO';
