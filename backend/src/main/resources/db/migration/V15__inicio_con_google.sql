-- Inicio con Google: `google_sub` es el id estable que Google da a cada persona; el correo puede cambiar, ese no.
-- Quien solo entra con Google no tiene contraseña; puede crearse una con "Recuperar contraseña".
ALTER TABLE usuarios ALTER COLUMN contrasena_hash DROP NOT NULL;
ALTER TABLE usuarios ADD COLUMN google_sub VARCHAR(255);
ALTER TABLE usuarios ADD CONSTRAINT uq_usuarios_google_sub UNIQUE (google_sub);
