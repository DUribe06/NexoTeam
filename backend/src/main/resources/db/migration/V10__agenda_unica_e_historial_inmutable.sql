-- Correcciones de la revisión independiente de V8 y V9 (ver backend/docs/base-de-datos.md).

-- ---------- Una persona, una sesión a la vez, sea cual sea su papel ----------
-- Las exclusiones de V9 comparan tutor con tutor y estudiante con estudiante. Quien da una tutoría a las 10:00 podía
-- además tomar otra a las 10:00. Esta regla cruza columnas, y una exclusión no puede expresarla.
--
-- El bloqueo consultivo hace que dos aceptaciones que comparten persona pasen de una en una: la segunda ve lo confirmado
-- por la primera. También evita que las exclusiones se interbloqueen entre sí cuando varias aceptaciones compiten por la
-- misma hora. Se toman siempre en el mismo orden para que dos aceptaciones no se esperen mutuamente.
CREATE FUNCTION fn_sesiones_agenda() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    PERFORM pg_advisory_xact_lock(hashtextextended(LEAST(NEW.estudiante_id, NEW.tutor_id)::TEXT, 0));
    PERFORM pg_advisory_xact_lock(hashtextextended(GREATEST(NEW.estudiante_id, NEW.tutor_id)::TEXT, 0));
    IF EXISTS (
        SELECT 1 FROM sesiones s
        WHERE s.estado = 'PROGRAMADA' AND s.inicio < NEW.fin AND s.fin > NEW.inicio
          AND (s.estudiante_id IN (NEW.estudiante_id, NEW.tutor_id) OR s.tutor_id IN (NEW.estudiante_id, NEW.tutor_id))
    ) THEN
        RAISE EXCEPTION 'tg_sesiones_agenda: una de las dos personas ya tiene otra sesión a esa hora'
            USING ERRCODE = 'check_violation', CONSTRAINT = 'tg_sesiones_agenda';
    END IF;
    RETURN NEW;
END $$;

-- Solo al insertar: ninguna transición vuelve a PROGRAMADA y las horas de una sesión no cambian (ver abajo)
CREATE TRIGGER tg_sesiones_agenda BEFORE INSERT ON sesiones
    FOR EACH ROW WHEN (NEW.estado = 'PROGRAMADA') EXECUTE FUNCTION fn_sesiones_agenda();

-- ---------- Una sesión solo cambia de estado ----------
-- Antes, cambiar `fin` desincronizaba el chat y `SET estado = 'COMPLETADA', fin = now()` completaba una sesión en curso.
CREATE OR REPLACE FUNCTION fn_sesiones_transicion() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF (NEW.id, NEW.solicitud_id, NEW.estudiante_id, NEW.tutor_id, NEW.inicio, NEW.fin, NEW.creado_en)
       IS DISTINCT FROM
       (OLD.id, OLD.solicitud_id, OLD.estudiante_id, OLD.tutor_id, OLD.inicio, OLD.fin, OLD.creado_en) THEN
        RAISE EXCEPTION 'tg_sesiones_transicion: de una sesión solo cambia el estado'
            USING ERRCODE = 'check_violation', CONSTRAINT = 'tg_sesiones_transicion';
    END IF;
    IF NEW.estado <> OLD.estado AND NOT (
           (OLD.estado = 'PROGRAMADA' AND NEW.estado = 'COMPLETADA' AND OLD.fin <= now())
        OR (OLD.estado = 'PROGRAMADA' AND NEW.estado = 'CANCELADA' AND OLD.inicio > now())
        OR (OLD.estado = 'PROGRAMADA' AND NEW.estado = 'NO_ASISTIO' AND OLD.fin <= now())
        OR (OLD.estado = 'COMPLETADA' AND NEW.estado = 'NO_ASISTIO')) THEN
        RAISE EXCEPTION 'tg_sesiones_transicion: % a % no es válido', OLD.estado, NEW.estado
            USING ERRCODE = 'check_violation', CONSTRAINT = 'tg_sesiones_transicion';
    END IF;
    RETURN NEW;
END $$;

DROP TRIGGER tg_sesiones_transicion ON sesiones;
CREATE TRIGGER tg_sesiones_transicion BEFORE UPDATE ON sesiones
    FOR EACH ROW EXECUTE FUNCTION fn_sesiones_transicion();

-- ---------- El historial no se borra ni se reescribe ----------
-- Borrar una sesión dejaría su solicitud ACEPTADA sin sesión y el chat abierto; tocar un mensaje rompería la secuencia.
CREATE FUNCTION fn_historial_inmutable() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION '%: esta fila es historial y no se puede cambiar ni borrar', TG_NAME
        USING ERRCODE = 'check_violation', CONSTRAINT = TG_NAME;
END $$;

CREATE TRIGGER tg_sesiones_sin_borrado BEFORE DELETE ON sesiones
    FOR EACH ROW EXECUTE FUNCTION fn_historial_inmutable();
CREATE TRIGGER tg_mensajes_inmutables BEFORE UPDATE OR DELETE ON mensajes
    FOR EACH ROW EXECUTE FUNCTION fn_historial_inmutable();

-- Con mensajes inmutables, la cascada ya no podría ocurrir: mejor decirlo con la restricción
ALTER TABLE mensajes DROP CONSTRAINT fk_mensajes_chat;
ALTER TABLE mensajes ADD CONSTRAINT fk_mensajes_chat FOREIGN KEY (chat_id) REFERENCES chats (id) ON DELETE RESTRICT;

-- ---------- Una aceptada siempre está por venir ----------
-- ACEPTADA con NO_ASISTIO quedaba ACEPTADA para siempre, porque la puesta al día solo mira las PROGRAMADA.
-- Marcar una inasistencia debe cerrar también la solicitud, en la misma transacción.
CREATE OR REPLACE FUNCTION fn_solicitud_y_sesion_coherentes() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    v_solicitud UUID;
    v_estado_solicitud VARCHAR;
    v_estado_sesion VARCHAR;
BEGIN
    IF TG_TABLE_NAME = 'solicitudes' THEN
        v_solicitud := NEW.id;
    ELSE
        v_solicitud := NEW.solicitud_id;
    END IF;
    SELECT so.estado, se.estado INTO v_estado_solicitud, v_estado_sesion
    FROM solicitudes so LEFT JOIN sesiones se ON se.solicitud_id = so.id
    WHERE so.id = v_solicitud;
    IF NOT (CASE v_estado_solicitud
        WHEN 'PENDIENTE' THEN v_estado_sesion IS NULL
        WHEN 'EXPIRADA' THEN v_estado_sesion IS NULL
        WHEN 'RECHAZADA' THEN v_estado_sesion IS NULL
        WHEN 'ACEPTADA' THEN v_estado_sesion IS NOT NULL AND v_estado_sesion = 'PROGRAMADA'
        WHEN 'FINALIZADA' THEN v_estado_sesion IS NOT NULL AND v_estado_sesion IN ('COMPLETADA', 'NO_ASISTIO')
        ELSE v_estado_sesion IS NULL OR v_estado_sesion = 'CANCELADA'
    END) THEN
        RAISE EXCEPTION 'ctg_solicitud_y_sesion_coherentes: solicitud % con sesión %', v_estado_solicitud, v_estado_sesion
            USING ERRCODE = 'check_violation', CONSTRAINT = 'ctg_solicitud_y_sesion_coherentes';
    END IF;
    RETURN NULL;
END $$;

-- ---------- Un CHECK que no dependa de la zona horaria de la sesión ----------
-- Sumar días a un TIMESTAMPTZ depende de `TimeZone`; sumar horas no. 15 días son 360 horas.
ALTER TABLE solicitudes DROP CONSTRAINT ck_solicitudes_inicio_futuro;
ALTER TABLE solicitudes ADD CONSTRAINT ck_solicitudes_inicio_futuro
    CHECK (inicio > enviada_en AND inicio < enviada_en + INTERVAL '360 hours');
