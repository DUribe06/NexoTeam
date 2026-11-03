-- RF5, RF6, RF8, RF9 y RF10. Las reglas que pueden romperse con dos peticiones simultáneas viven aquí y no en el código:
-- ver backend/docs/base-de-datos.md. Los momentos son TIMESTAMPTZ: fecha y hora de la API se derivan en hora de Colombia.

-- Permite combinar `tutor_id WITH =` y un rango de tiempo en una restricción de exclusión
CREATE EXTENSION IF NOT EXISTS btree_gist WITH SCHEMA public;

-- ---------- Chats ----------
-- Uno por par (estudiante, tutor); nace al aceptar la primera solicitud. Quienes cruzan los papeles tienen dos chats.
CREATE TABLE chats (
    id                      UUID                     PRIMARY KEY,
    estudiante_id           UUID                     NOT NULL,
    tutor_id                UUID                     NOT NULL,
    -- Fin de la última sesión programada entre los dos. Vacío o vencido: solo lectura. Lo mantiene un trigger de sesiones,
    -- y al ser un momento el chat se cierra solo cuando termina la hora, sin que nadie tenga que escribir nada.
    activo_hasta            TIMESTAMP WITH TIME ZONE,
    ultima_secuencia        BIGINT                   NOT NULL DEFAULT 0,
    ultimo_mensaje_en       TIMESTAMP WITH TIME ZONE,
    leido_hasta_estudiante  BIGINT                   NOT NULL DEFAULT 0,
    leido_hasta_tutor       BIGINT                   NOT NULL DEFAULT 0,
    creado_en               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_chats_par UNIQUE (estudiante_id, tutor_id),
    CONSTRAINT fk_chats_estudiante FOREIGN KEY (estudiante_id) REFERENCES usuarios (id) ON DELETE RESTRICT,
    CONSTRAINT fk_chats_tutor FOREIGN KEY (tutor_id) REFERENCES usuarios (id) ON DELETE RESTRICT,
    CONSTRAINT ck_chats_personas_distintas CHECK (estudiante_id <> tutor_id),
    CONSTRAINT ck_chats_lectura CHECK (
        leido_hasta_estudiante BETWEEN 0 AND ultima_secuencia AND leido_hasta_tutor BETWEEN 0 AND ultima_secuencia),
    CONSTRAINT ck_chats_ultimo_mensaje CHECK ((ultima_secuencia = 0) = (ultimo_mensaje_en IS NULL))
);

-- Bandeja del tutor; la del estudiante usa uq_chats_par
CREATE INDEX ix_chats_tutor ON chats (tutor_id);

-- ---------- Mensajes ----------
-- `secuencia` es el cursor de `desde` y `antes`: sin huecos dentro del chat y sin los empates que tendría una fecha.
-- `emisor` es el lado, no una persona: así un mensaje no puede ser de alguien ajeno al chat.
CREATE TABLE mensajes (
    id          UUID                     PRIMARY KEY,
    chat_id     UUID                     NOT NULL,
    secuencia   BIGINT                   NOT NULL,
    enviado_en  TIMESTAMP WITH TIME ZONE NOT NULL,
    emisor      VARCHAR(10)              NOT NULL,
    -- Lo genera la app: reintentar tras un fallo de red no duplica el mensaje
    id_cliente  UUID,
    texto       VARCHAR(2000)            NOT NULL,

    CONSTRAINT fk_mensajes_chat FOREIGN KEY (chat_id) REFERENCES chats (id) ON DELETE CASCADE,
    CONSTRAINT uq_mensajes_chat_secuencia UNIQUE (chat_id, secuencia),
    CONSTRAINT uq_mensajes_id_cliente UNIQUE (chat_id, emisor, id_cliente),
    CONSTRAINT ck_mensajes_emisor CHECK (emisor IN ('ESTUDIANTE', 'TUTOR')),
    CONSTRAINT ck_mensajes_secuencia CHECK (secuencia > 0),
    CONSTRAINT ck_mensajes_texto CHECK (BTRIM(texto) <> '')
);

-- Bloquear el chat serializa los envíos, así que la secuencia no tiene huecos ni carreras, y el chequeo de solo lectura
-- no puede adelantarse a una cancelación simultánea. La hora la pone el servidor, salvo los datos demo, que traen la suya.
CREATE FUNCTION fn_mensajes_numera() RETURNS TRIGGER LANGUAGE plpgsql AS $$
DECLARE
    v_activo_hasta TIMESTAMP WITH TIME ZONE;
    v_ultima BIGINT;
BEGIN
    SELECT activo_hasta, ultima_secuencia INTO v_activo_hasta, v_ultima
    FROM chats WHERE id = NEW.chat_id FOR NO KEY UPDATE;
    IF NOT FOUND THEN
        RAISE EXCEPTION 'fk_mensajes_chat: el chat no existe'
            USING ERRCODE = 'foreign_key_violation', CONSTRAINT = 'fk_mensajes_chat';
    END IF;
    IF v_activo_hasta IS NULL OR v_activo_hasta <= clock_timestamp() THEN
        RAISE EXCEPTION 'tg_mensajes_chat_activo: el chat está en solo lectura'
            USING ERRCODE = 'check_violation', CONSTRAINT = 'tg_mensajes_chat_activo';
    END IF;
    NEW.secuencia := v_ultima + 1;
    NEW.enviado_en := COALESCE(NEW.enviado_en, clock_timestamp());
    RETURN NEW;
END $$;

CREATE TRIGGER tg_mensajes_numera BEFORE INSERT ON mensajes
    FOR EACH ROW EXECUTE FUNCTION fn_mensajes_numera();

-- Va después del insert: un `ON CONFLICT DO NOTHING` por idCliente repetido no llega aquí y no gasta la secuencia.
CREATE FUNCTION fn_mensajes_actualiza_chat() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    UPDATE chats SET ultima_secuencia = NEW.secuencia, ultimo_mensaje_en = NEW.enviado_en WHERE id = NEW.chat_id;
    RETURN NULL;
END $$;

CREATE TRIGGER tg_mensajes_actualiza_chat AFTER INSERT ON mensajes
    FOR EACH ROW EXECUTE FUNCTION fn_mensajes_actualiza_chat();

-- ---------- Solicitudes ----------
CREATE TABLE solicitudes (
    id              UUID                     PRIMARY KEY,
    estudiante_id   UUID                     NOT NULL,
    tutor_id        UUID                     NOT NULL,
    materia         VARCHAR(80)              NOT NULL,
    tema            VARCHAR(200)             NOT NULL,
    inicio          TIMESTAMP WITH TIME ZONE NOT NULL,
    modalidad       VARCHAR(20)              NOT NULL,
    mensaje         VARCHAR(500)             NOT NULL DEFAULT '',
    estado          VARCHAR(20)              NOT NULL DEFAULT 'PENDIENTE',
    motivo_rechazo  VARCHAR(300),
    enviada_en      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    vence_en        TIMESTAMP WITH TIME ZONE NOT NULL,

    CONSTRAINT fk_solicitudes_estudiante FOREIGN KEY (estudiante_id) REFERENCES usuarios (id) ON DELETE RESTRICT,
    CONSTRAINT fk_solicitudes_tutor FOREIGN KEY (tutor_id) REFERENCES usuarios (id) ON DELETE RESTRICT,
    -- Destino de la clave foránea compuesta de sesiones
    CONSTRAINT uq_solicitudes_para_sesion UNIQUE (id, estudiante_id, tutor_id, inicio),
    CONSTRAINT ck_solicitudes_estado
        CHECK (estado IN ('PENDIENTE', 'ACEPTADA', 'RECHAZADA', 'FINALIZADA', 'CANCELADA', 'EXPIRADA')),
    CONSTRAINT ck_solicitudes_modalidad CHECK (modalidad IN ('PRESENCIAL', 'VIRTUAL')),
    CONSTRAINT ck_solicitudes_personas_distintas CHECK (estudiante_id <> tutor_id),
    CONSTRAINT ck_solicitudes_textos CHECK (BTRIM(materia) <> '' AND BTRIM(tema) <> ''),
    CONSTRAINT ck_solicitudes_motivo CHECK (motivo_rechazo IS NULL OR estado = 'RECHAZADA'),
    CONSTRAINT ck_solicitudes_inicio_en_calendario CHECK (
        (inicio AT TIME ZONE 'America/Bogota')::TIME IN (TIME '08:00', TIME '10:00', TIME '12:00', TIME '14:00',
                                                          TIME '16:00', TIME '18:00', TIME '20:00')),
    -- Desde mañana hasta 14 días, en días de Colombia: 15 días cubre cualquier hora de envío. El día exacto lo valida el servicio.
    CONSTRAINT ck_solicitudes_inicio_futuro CHECK (inicio > enviada_en AND inicio < enviada_en + INTERVAL '15 days'),
    CONSTRAINT ck_solicitudes_plazo CHECK (vence_en = LEAST(enviada_en + INTERVAL '24 hours', inicio))
);

-- Un estudiante no repite con el mismo tutor y hora mientras siga viva. Dos estudiantes sí pueden pedir la misma hora:
-- gana quien el tutor acepte, y eso lo decide la exclusión de sesiones.
CREATE UNIQUE INDEX uq_solicitudes_activa ON solicitudes (estudiante_id, tutor_id, inicio)
    WHERE estado IN ('PENDIENTE', 'ACEPTADA');
CREATE INDEX ix_solicitudes_estudiante ON solicitudes (estudiante_id, enviada_en DESC);
CREATE INDEX ix_solicitudes_tutor ON solicitudes (tutor_id, enviada_en DESC);
-- La puesta al día busca las vencidas en cada petición; casi siempre no hay ninguna
CREATE INDEX ix_solicitudes_por_vencer ON solicitudes (vence_en) WHERE estado = 'PENDIENTE';

CREATE FUNCTION fn_solicitudes_transicion() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF (NEW.estudiante_id, NEW.tutor_id, NEW.materia, NEW.tema, NEW.inicio, NEW.modalidad, NEW.mensaje, NEW.enviada_en)
       IS DISTINCT FROM
       (OLD.estudiante_id, OLD.tutor_id, OLD.materia, OLD.tema, OLD.inicio, OLD.modalidad, OLD.mensaje, OLD.enviada_en) THEN
        RAISE EXCEPTION 'tg_solicitudes_transicion: lo pedido en una solicitud no cambia'
            USING ERRCODE = 'check_violation', CONSTRAINT = 'tg_solicitudes_transicion';
    END IF;
    IF NEW.estado <> OLD.estado AND NOT (
           (OLD.estado = 'PENDIENTE' AND NEW.estado IN ('ACEPTADA', 'RECHAZADA', 'CANCELADA', 'EXPIRADA'))
        OR (OLD.estado = 'ACEPTADA' AND NEW.estado IN ('CANCELADA', 'FINALIZADA'))) THEN
        RAISE EXCEPTION 'tg_solicitudes_transicion: % a % no es válido', OLD.estado, NEW.estado
            USING ERRCODE = 'check_violation', CONSTRAINT = 'tg_solicitudes_transicion';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER tg_solicitudes_transicion BEFORE UPDATE ON solicitudes
    FOR EACH ROW EXECUTE FUNCTION fn_solicitudes_transicion();

-- ---------- Sesiones ----------
CREATE TABLE sesiones (
    id             UUID                     PRIMARY KEY,
    solicitud_id   UUID                     NOT NULL,
    estudiante_id  UUID                     NOT NULL,
    tutor_id       UUID                     NOT NULL,
    inicio         TIMESTAMP WITH TIME ZONE NOT NULL,
    fin            TIMESTAMP WITH TIME ZONE NOT NULL,
    estado         VARCHAR(20)              NOT NULL DEFAULT 'PROGRAMADA',
    creado_en      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_sesiones_solicitud UNIQUE (solicitud_id),
    -- Destino de la clave foránea compuesta de calificaciones
    CONSTRAINT uq_sesiones_para_calificar UNIQUE (id, estado, estudiante_id, tutor_id),
    -- Compuesta: una sesión no puede tener otras personas ni otra hora que las de su solicitud
    CONSTRAINT fk_sesiones_solicitud FOREIGN KEY (solicitud_id, estudiante_id, tutor_id, inicio)
        REFERENCES solicitudes (id, estudiante_id, tutor_id, inicio) ON UPDATE RESTRICT ON DELETE RESTRICT,
    -- Toda sesión tiene chat: no se puede aceptar una solicitud sin habilitarlo
    CONSTRAINT fk_sesiones_chat FOREIGN KEY (estudiante_id, tutor_id)
        REFERENCES chats (estudiante_id, tutor_id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT ck_sesiones_estado CHECK (estado IN ('PROGRAMADA', 'COMPLETADA', 'CANCELADA', 'NO_ASISTIO')),
    CONSTRAINT ck_sesiones_duracion CHECK (fin > inicio AND fin <= inicio + INTERVAL '4 hours'),
    -- Lo que resuelve dos aceptaciones simultáneas: la segunda choca aquí aunque ambas hayan leído la hora libre.
    -- Rango semiabierto: una sesión que acaba a las 10:00 no choca con la que empieza a las 10:00.
    CONSTRAINT ex_sesiones_tutor_sin_solape EXCLUDE USING GIST (tutor_id WITH =, TSTZRANGE(inicio, fin, '[)') WITH &&)
        WHERE (estado = 'PROGRAMADA'),
    CONSTRAINT ex_sesiones_estudiante_sin_solape EXCLUDE USING GIST (estudiante_id WITH =, TSTZRANGE(inicio, fin, '[)') WITH &&)
        WHERE (estado = 'PROGRAMADA')
);

-- Listado e historial por modo, la más reciente primero
CREATE INDEX ix_sesiones_tutor ON sesiones (tutor_id, inicio DESC);
CREATE INDEX ix_sesiones_estudiante ON sesiones (estudiante_id, inicio DESC);
-- Próximas sesiones por modo: la consulta del Inicio
CREATE INDEX ix_sesiones_tutor_programadas ON sesiones (tutor_id, inicio) WHERE estado = 'PROGRAMADA';
CREATE INDEX ix_sesiones_estudiante_programadas ON sesiones (estudiante_id, inicio) WHERE estado = 'PROGRAMADA';
-- La puesta al día busca las terminadas en cada petición; casi siempre no hay ninguna
CREATE INDEX ix_sesiones_por_terminar ON sesiones (fin) WHERE estado = 'PROGRAMADA';

CREATE FUNCTION fn_sesiones_transicion() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.estado <> OLD.estado AND NOT (
           (OLD.estado = 'PROGRAMADA' AND NEW.estado = 'COMPLETADA' AND NEW.fin <= now())
        OR (OLD.estado = 'PROGRAMADA' AND NEW.estado = 'CANCELADA' AND NEW.inicio > now())
        OR (OLD.estado = 'PROGRAMADA' AND NEW.estado = 'NO_ASISTIO' AND NEW.fin <= now())
        OR (OLD.estado = 'COMPLETADA' AND NEW.estado = 'NO_ASISTIO')) THEN
        RAISE EXCEPTION 'tg_sesiones_transicion: % a % no es válido', OLD.estado, NEW.estado
            USING ERRCODE = 'check_violation', CONSTRAINT = 'tg_sesiones_transicion';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER tg_sesiones_transicion BEFORE UPDATE OF estado ON sesiones
    FOR EACH ROW EXECUTE FUNCTION fn_sesiones_transicion();

-- El bloqueo evita que dos cambios simultáneos sobre el mismo par calculen `activo_hasta` sin verse el uno al otro
CREATE FUNCTION fn_sesiones_sincroniza_chat() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    PERFORM 1 FROM chats WHERE estudiante_id = NEW.estudiante_id AND tutor_id = NEW.tutor_id FOR NO KEY UPDATE;
    UPDATE chats SET activo_hasta = (
        SELECT MAX(s.fin) FROM sesiones s
        WHERE s.estudiante_id = NEW.estudiante_id AND s.tutor_id = NEW.tutor_id AND s.estado = 'PROGRAMADA')
    WHERE estudiante_id = NEW.estudiante_id AND tutor_id = NEW.tutor_id;
    RETURN NULL;
END $$;

CREATE TRIGGER tg_sesiones_sincroniza_chat AFTER INSERT OR UPDATE OF estado ON sesiones
    FOR EACH ROW EXECUTE FUNCTION fn_sesiones_sincroniza_chat();

-- Diferido al final de la transacción: solicitud y sesión cambian en dos sentencias y solo importa cómo quedan juntas
CREATE FUNCTION fn_solicitud_y_sesion_coherentes() RETURNS TRIGGER LANGUAGE plpgsql AS $$
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
        WHEN 'ACEPTADA' THEN v_estado_sesion IS NOT NULL AND v_estado_sesion IN ('PROGRAMADA', 'NO_ASISTIO')
        WHEN 'FINALIZADA' THEN v_estado_sesion IS NOT NULL AND v_estado_sesion IN ('COMPLETADA', 'NO_ASISTIO')
        ELSE v_estado_sesion IS NULL OR v_estado_sesion = 'CANCELADA'
    END) THEN
        RAISE EXCEPTION 'ctg_solicitud_y_sesion_coherentes: solicitud % con sesión %', v_estado_solicitud, v_estado_sesion
            USING ERRCODE = 'check_violation', CONSTRAINT = 'ctg_solicitud_y_sesion_coherentes';
    END IF;
    RETURN NULL;
END $$;

CREATE CONSTRAINT TRIGGER ctg_solicitudes_coherentes AFTER INSERT OR UPDATE OF estado ON solicitudes
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION fn_solicitud_y_sesion_coherentes();
CREATE CONSTRAINT TRIGGER ctg_sesiones_coherentes AFTER INSERT OR UPDATE OF estado ON sesiones
    DEFERRABLE INITIALLY DEFERRED FOR EACH ROW EXECUTE FUNCTION fn_solicitud_y_sesion_coherentes();

-- ---------- Calificaciones ----------
-- Son también las reseñas del tutor: una reseña es la calificación de un estudiante, no otra copia del dato (RF4, RF9).
CREATE TABLE calificaciones (
    id             UUID                     PRIMARY KEY,
    sesion_id      UUID                     NOT NULL,
    estudiante_id  UUID                     NOT NULL,
    tutor_id       UUID                     NOT NULL,
    -- Siempre COMPLETADA: entra en la clave foránea, y así la base impide calificar una sesión en cualquier otro estado
    estado_sesion  VARCHAR(20)              NOT NULL DEFAULT 'COMPLETADA',
    sentido        VARCHAR(20)              NOT NULL,
    estrellas      INTEGER                  NOT NULL,
    comentario     VARCHAR(500),
    creado_en      TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_calificaciones_sesion_completada FOREIGN KEY (sesion_id, estado_sesion, estudiante_id, tutor_id)
        REFERENCES sesiones (id, estado, estudiante_id, tutor_id) ON UPDATE RESTRICT ON DELETE RESTRICT,
    CONSTRAINT uq_calificaciones_sesion_sentido UNIQUE (sesion_id, sentido),
    CONSTRAINT ck_calificaciones_solo_completada CHECK (estado_sesion = 'COMPLETADA'),
    CONSTRAINT ck_calificaciones_sentido CHECK (sentido IN ('ESTUDIANTE_A_TUTOR', 'TUTOR_A_ESTUDIANTE')),
    CONSTRAINT ck_calificaciones_estrellas CHECK (estrellas BETWEEN 1 AND 5),
    CONSTRAINT ck_calificaciones_comentario CHECK (comentario IS NULL OR BTRIM(comentario) <> '')
);

-- GET /api/tutores/{id}/resenas, la más reciente primero
CREATE INDEX ix_calificaciones_resenas_tutor ON calificaciones (tutor_id, creado_en DESC, id)
    WHERE sentido = 'ESTUDIANTE_A_TUTOR';

-- No se edita (RF9). Solo se admite que moderación borre el comentario de una reseña ofensiva.
CREATE FUNCTION fn_calificaciones_inmutables() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF NEW.comentario IS NOT NULL OR
       (NEW.id, NEW.sesion_id, NEW.estudiante_id, NEW.tutor_id, NEW.estado_sesion, NEW.sentido, NEW.estrellas, NEW.creado_en)
       IS DISTINCT FROM
       (OLD.id, OLD.sesion_id, OLD.estudiante_id, OLD.tutor_id, OLD.estado_sesion, OLD.sentido, OLD.estrellas, OLD.creado_en) THEN
        RAISE EXCEPTION 'tg_calificaciones_inmutables: una calificación no se edita'
            USING ERRCODE = 'check_violation', CONSTRAINT = 'tg_calificaciones_inmutables';
    END IF;
    RETURN NEW;
END $$;

CREATE TRIGGER tg_calificaciones_inmutables BEFORE UPDATE ON calificaciones
    FOR EACH ROW EXECUTE FUNCTION fn_calificaciones_inmutables();

-- El UPDATE bloquea la fila del tutor: dos calificaciones simultáneas suman en orden y el promedio no pierde ninguna
CREATE FUNCTION fn_calificaciones_reputacion() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    IF TG_OP = 'INSERT' AND NEW.sentido = 'ESTUDIANTE_A_TUTOR' THEN
        UPDATE usuarios SET suma_estrellas = suma_estrellas + NEW.estrellas, total_resenas = total_resenas + 1
        WHERE id = NEW.tutor_id;
    ELSIF TG_OP = 'DELETE' AND OLD.sentido = 'ESTUDIANTE_A_TUTOR' THEN
        UPDATE usuarios SET suma_estrellas = suma_estrellas - OLD.estrellas, total_resenas = total_resenas - 1
        WHERE id = OLD.tutor_id;
    END IF;
    RETURN NULL;
END $$;

CREATE TRIGGER tg_calificaciones_reputacion AFTER INSERT OR DELETE ON calificaciones
    FOR EACH ROW EXECUTE FUNCTION fn_calificaciones_reputacion();

-- Las reseñas pasan a ser calificaciones. Las únicas filas posibles son datos demo sin sesión (`sesion_id` vacío), y la
-- semilla local las vuelve a crear con sus sesiones.
DROP TABLE resenas;
