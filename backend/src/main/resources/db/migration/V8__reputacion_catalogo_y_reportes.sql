-- El promedio del tutor se mantiene en la misma transacción que cada calificación (lo hace un trigger de V9). Calcularlo
-- en cada búsqueda obliga a recorrer todas las reseñas; así el catálogo se ordena con un índice.
ALTER TABLE usuarios ADD COLUMN suma_estrellas INTEGER NOT NULL DEFAULT 0;
ALTER TABLE usuarios ADD COLUMN total_resenas INTEGER NOT NULL DEFAULT 0;
ALTER TABLE usuarios ADD COLUMN calificacion_promedio NUMERIC(5, 4) GENERATED ALWAYS AS (
    CASE WHEN total_resenas = 0 THEN 0 ELSE ROUND(suma_estrellas::NUMERIC / total_resenas, 4) END) STORED;
ALTER TABLE usuarios ADD CONSTRAINT ck_usuarios_reputacion
    CHECK (total_resenas >= 0 AND suma_estrellas BETWEEN total_resenas AND 5 * total_resenas);

-- Mismo orden que GET /api/tutores: mejor promedio, más reseñas, nombre e id para que la paginación sea estable
CREATE INDEX ix_usuarios_catalogo ON usuarios (calificacion_promedio DESC, total_resenas DESC, nombre, id)
    WHERE perfil_tutor_activo = TRUE AND estado_cuenta = 'ACTIVA';

-- Lo que la aplicación ya valida, para que ningún otro camino de escritura se lo salte
ALTER TABLE usuarios ADD CONSTRAINT ck_usuarios_handle CHECK (handle = LOWER(handle) AND handle LIKE '@_%');
ALTER TABLE usuarios ADD CONSTRAINT ck_usuarios_precio_hora_maximo CHECK (precio_hora <= 10000000);
ALTER TABLE tutor_materias ADD CONSTRAINT ck_tutor_materias_texto CHECK (materia = BTRIM(materia) AND materia <> '');
ALTER TABLE tutor_materias ADD CONSTRAINT ck_tutor_materias_posicion CHECK (posicion >= 0);
CREATE UNIQUE INDEX uq_tutor_materias_sin_mayusculas ON tutor_materias (usuario_id, LOWER(materia));

-- Reportes: con PostgreSQL como única base, "uno abierto por persona y tutor" se dice con un índice parcial y sobra la
-- columna `abierto`. Son evidencia para el administrador (RF12): borrar una cuenta no puede llevárselos en cascada.
ALTER TABLE reportes DROP CONSTRAINT uq_reportes_abierto;
ALTER TABLE reportes DROP CONSTRAINT ck_reportes_abierto_o_cerrado;
ALTER TABLE reportes DROP COLUMN abierto;
CREATE UNIQUE INDEX uq_reportes_abierto ON reportes (reportante_id, tutor_id) WHERE cerrado_en IS NULL;
ALTER TABLE reportes DROP CONSTRAINT fk_reportes_tutor;
ALTER TABLE reportes DROP CONSTRAINT fk_reportes_reportante;
ALTER TABLE reportes ADD CONSTRAINT fk_reportes_tutor FOREIGN KEY (tutor_id) REFERENCES usuarios (id) ON DELETE RESTRICT;
ALTER TABLE reportes ADD CONSTRAINT fk_reportes_reportante
    FOREIGN KEY (reportante_id) REFERENCES usuarios (id) ON DELETE RESTRICT;
CREATE INDEX ix_reportes_reportante ON reportes (reportante_id);
