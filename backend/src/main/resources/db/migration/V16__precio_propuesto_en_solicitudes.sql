-- El estudiante puede proponer otro precio por hora al solicitar; al aceptar, el tutor acepta ese precio.
ALTER TABLE solicitudes ADD COLUMN precio_propuesto NUMERIC(10, 2);
ALTER TABLE solicitudes ADD CONSTRAINT ck_solicitudes_precio_propuesto
    CHECK (precio_propuesto IS NULL OR (precio_propuesto > 0 AND precio_propuesto <= 10000000));
