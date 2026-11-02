-- Fase 1 (RF2): sesiones renovables. El token de refresco es aleatorio y opaco; aquí solo se guarda su
-- hash SHA-256 (hex), así que quien lea la base no puede usarlo. Cada uso lo reemplaza por uno nuevo (rotación).
CREATE TABLE tokens_refresco (
    id           UUID                     PRIMARY KEY,
    usuario_id   UUID                     NOT NULL,
    token_hash   VARCHAR(64)              NOT NULL,
    vence_en     TIMESTAMP WITH TIME ZONE NOT NULL,
    revocado_en  TIMESTAMP WITH TIME ZONE,
    creado_en    TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_tokens_refresco_hash UNIQUE (token_hash),
    CONSTRAINT fk_tokens_refresco_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE
);

CREATE INDEX ix_tokens_refresco_usuario ON tokens_refresco (usuario_id);
