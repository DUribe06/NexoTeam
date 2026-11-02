-- De aquí sale `enLinea` del tutor: conectado si usó la API en los últimos 5 minutos.
ALTER TABLE usuarios ADD COLUMN ultima_actividad_en TIMESTAMP WITH TIME ZONE;
