-- Módulo Usuario. Toda cuenta nace en modo Estudiante y cambia entre Estudiante y Tutor desde el Perfil, sin
-- verificación: `rol` pasa a ser el modo en uso (o ADMIN), y el estado de verificación del tutor desaparece.
ALTER TABLE usuarios DROP CONSTRAINT ck_usuarios_verificacion_solo_tutor;
ALTER TABLE usuarios DROP CONSTRAINT ck_usuarios_estado_verificacion_tutor;
ALTER TABLE usuarios DROP COLUMN estado_verificacion_tutor;

-- Nombre público, único. A las cuentas que ya existían se les deriva del id, que es lo único seguro de no repetirse.
ALTER TABLE usuarios ADD COLUMN handle VARCHAR(40);
UPDATE usuarios SET handle = '@u' || REPLACE(CAST(id AS VARCHAR), '-', '');
ALTER TABLE usuarios ALTER COLUMN handle SET NOT NULL;
ALTER TABLE usuarios ADD CONSTRAINT uq_usuarios_handle UNIQUE (handle);

-- La cuenta ya usó el modo Tutor. Quien vuelve a Estudiante conserva su perfil, por eso no se deduce de `rol`.
ALTER TABLE usuarios ADD COLUMN perfil_tutor_activo BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE usuarios SET perfil_tutor_activo = TRUE WHERE rol = 'TUTOR';
ALTER TABLE usuarios ADD CONSTRAINT ck_usuarios_tutor_con_perfil CHECK (rol <> 'TUTOR' OR perfil_tutor_activo = TRUE);

ALTER TABLE usuarios ADD COLUMN universidad VARCHAR(120);
ALTER TABLE usuarios ADD COLUMN carrera VARCHAR(120);
ALTER TABLE usuarios ADD COLUMN bio VARCHAR(500);
-- Tarifa por hora en pesos colombianos; vacía hasta que el tutor la fije
ALTER TABLE usuarios ADD COLUMN precio_hora NUMERIC(10, 2);
ALTER TABLE usuarios ADD CONSTRAINT ck_usuarios_precio_hora CHECK (precio_hora >= 0);

-- Orden en que el tutor indicó sus materias: la primera es la que la app propone al solicitar una tutoría
ALTER TABLE tutor_materias ADD COLUMN posicion INTEGER NOT NULL DEFAULT 0;
