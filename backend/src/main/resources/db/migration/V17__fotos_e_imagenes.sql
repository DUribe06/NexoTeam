-- Fotos de perfil e imágenes del chat. Los archivos viven en Cloudinary: aquí solo se guarda su dirección.
-- La foto propia gana sobre la de Google; sin ninguna, la app muestra las iniciales.
ALTER TABLE usuarios
    ADD COLUMN foto_google VARCHAR(1000),
    ADD COLUMN foto_url    VARCHAR(1000),
    ADD CONSTRAINT ck_usuarios_foto_google CHECK (foto_google IS NULL OR foto_google LIKE 'https://%'),
    ADD CONSTRAINT ck_usuarios_foto_url CHECK (foto_url IS NULL OR foto_url LIKE 'https://res.cloudinary.com/%');

-- Un mensaje puede ser solo una imagen: entonces el texto queda vacío
ALTER TABLE mensajes
    ADD COLUMN imagen_url VARCHAR(1000),
    ADD CONSTRAINT ck_mensajes_imagen CHECK (imagen_url IS NULL OR imagen_url LIKE 'https://res.cloudinary.com/%'),
    DROP CONSTRAINT ck_mensajes_texto,
    ADD CONSTRAINT ck_mensajes_texto CHECK (BTRIM(texto) <> '' OR imagen_url IS NOT NULL);
