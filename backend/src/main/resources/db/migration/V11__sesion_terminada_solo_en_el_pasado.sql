-- Una sesión no nace terminada con un final por venir. Lo demás ya lo cubre la transición (V10), que solo mira los UPDATE.
-- Se puede insertar en estado terminado porque así crea la semilla local su historial, siempre con fechas pasadas.
CREATE FUNCTION fn_sesiones_terminada_en_el_pasado() RETURNS TRIGGER LANGUAGE plpgsql AS $$
BEGIN
    RAISE EXCEPTION 'tg_sesiones_terminada_en_el_pasado: una sesión que aún no acaba no puede estar %', NEW.estado
        USING ERRCODE = 'check_violation', CONSTRAINT = 'tg_sesiones_terminada_en_el_pasado';
END $$;

CREATE TRIGGER tg_sesiones_terminada_en_el_pasado BEFORE INSERT ON sesiones
    FOR EACH ROW WHEN (NEW.estado IN ('COMPLETADA', 'NO_ASISTIO') AND NEW.fin > now())
    EXECUTE FUNCTION fn_sesiones_terminada_en_el_pasado();
