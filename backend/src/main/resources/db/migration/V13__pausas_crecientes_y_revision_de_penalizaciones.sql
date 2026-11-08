-- Qué efecto tiene una penalización. Se deduce de las filas, sin guardar la pausa: termina sola y, si una revisión
-- retira una penalización, las cuentas se corrigen sin tocar `penalizaciones`, que es inmutable.

-- ---------- Revisión ----------
-- Quien cree que fue un error la pide una sola vez. La pausa sigue mientras tanto; la resuelve un administrador (RF12).
CREATE TABLE revisiones_penalizacion (
    id              UUID                     NOT NULL PRIMARY KEY,
    penalizacion_id UUID                     NOT NULL,
    motivo          VARCHAR(500)             NOT NULL,
    estado          VARCHAR(10)              NOT NULL DEFAULT 'ABIERTA',
    creado_en       TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resuelto_en     TIMESTAMP WITH TIME ZONE,

    CONSTRAINT fk_revisiones_penalizacion FOREIGN KEY (penalizacion_id) REFERENCES penalizaciones (id) ON DELETE RESTRICT,
    CONSTRAINT uq_revisiones_penalizacion UNIQUE (penalizacion_id),
    CONSTRAINT ck_revisiones_estado CHECK (estado IN ('ABIERTA', 'ACEPTADA', 'RECHAZADA')),
    CONSTRAINT ck_revisiones_resuelta CHECK ((estado = 'ABIERTA') = (resuelto_en IS NULL))
);

CREATE INDEX ix_revisiones_abiertas ON revisiones_penalizacion (creado_en) WHERE estado = 'ABIERTA';

-- Las que cuentan: todas menos las que una revisión retiró
CREATE VIEW penalizaciones_firmes AS
SELECT p.* FROM penalizaciones p
WHERE NOT EXISTS (SELECT 1 FROM revisiones_penalizacion r WHERE r.penalizacion_id = p.id AND r.estado = 'ACEPTADA');

-- ---------- Contador ----------
-- Solo los últimos 90 días y solo las del papel pedido: faltar como estudiante no resta como tutor.
CREATE FUNCTION fn_penalizaciones_recientes(p_usuario UUID, p_como_tutor BOOLEAN) RETURNS INTEGER
LANGUAGE sql STABLE AS $$
    SELECT COUNT(*)::INTEGER
    FROM penalizaciones_firmes p JOIN sesiones s ON s.id = p.sesion_id
    WHERE p.usuario_id = p_usuario
      AND p.creado_en > now() - INTERVAL '90 days'
      AND CASE WHEN p_como_tutor THEN s.tutor_id ELSE s.estudiante_id END = p_usuario
$$;

-- ---------- Pausa ----------
-- Cada grave pausa 7 días; si es la segunda en 90 días, 14; desde la tercera, 30. NULL si no hay pausa vigente.
CREATE FUNCTION fn_pausa_hasta(p_usuario UUID) RETURNS TIMESTAMP WITH TIME ZONE
LANGUAGE sql STABLE AS $$
    SELECT hasta FROM (
        SELECT MAX(creado_en + make_interval(days => CASE seguidas WHEN 1 THEN 7 WHEN 2 THEN 14 ELSE 30 END)) AS hasta
        FROM (
            SELECT creado_en, COUNT(*) OVER (ORDER BY creado_en RANGE BETWEEN INTERVAL '90 days' PRECEDING AND CURRENT ROW) AS seguidas
            FROM penalizaciones_firmes
            WHERE usuario_id = p_usuario AND nivel = 'GRAVE'
        ) graves
    ) pausa
    WHERE hasta > now()
$$;
