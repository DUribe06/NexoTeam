-- Faltar seguido también pausa: la tercera penalización NORMAL en 90 días pausa 7 días.
CREATE OR REPLACE FUNCTION fn_pausa_hasta(p_usuario UUID) RETURNS TIMESTAMP WITH TIME ZONE
LANGUAGE sql STABLE AS $$
    SELECT hasta FROM (
        SELECT MAX(creado_en + make_interval(days =>
            CASE WHEN nivel = 'GRAVE' THEN CASE seguidas WHEN 1 THEN 7 WHEN 2 THEN 14 ELSE 30 END
                 WHEN seguidas >= 3 THEN 7
                 ELSE 0 END)) AS hasta
        FROM (
            SELECT creado_en, nivel,
                   COUNT(*) OVER (PARTITION BY nivel ORDER BY creado_en RANGE BETWEEN INTERVAL '90 days' PRECEDING AND CURRENT ROW) AS seguidas
            FROM penalizaciones_firmes
            WHERE usuario_id = p_usuario
        ) faltas
    ) pausa
    WHERE hasta > now()
$$;

-- Eliminar una cuenta la anonimiza: su historial también es de otras personas, así que las filas se quedan.
ALTER TABLE usuarios DROP CONSTRAINT ck_usuarios_estado_cuenta;
ALTER TABLE usuarios ADD CONSTRAINT ck_usuarios_estado_cuenta
    CHECK (estado_cuenta IN ('PENDIENTE_VERIFICACION_CORREO', 'ACTIVA', 'SUSPENDIDA', 'ELIMINADA'));
