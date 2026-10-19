-- Fase 1 (RF1, RF2, RF16): cuentas y códigos de verificación.
-- Los ids (UUID) los genera la aplicación. Las reglas de negocio que se pueden expresar aquí
-- (correo normalizado, un tutor siempre tiene estado de verificación, consentimiento de datos)
-- se dejan como restricciones para que ningún camino de escritura pueda saltárselas.

CREATE TABLE usuarios (
    id                          UUID                     PRIMARY KEY,
    nombre                      VARCHAR(100)             NOT NULL,
    -- Siempre en minúsculas y sin espacios: la aplicación lo normaliza y la base lo exige (D2)
    correo                      VARCHAR(254)             NOT NULL,
    contrasena_hash             VARCHAR(100)             NOT NULL,
    rol                         VARCHAR(20)              NOT NULL,
    documento_identidad         VARCHAR(20),
    estado_cuenta               VARCHAR(40)              NOT NULL,
    -- Verificación académica por el administrador (RF12); solo para tutores
    estado_verificacion_tutor   VARCHAR(20),
    -- Bloqueo temporal tras 5 intentos fallidos (RF2)
    intentos_fallidos           INTEGER                  NOT NULL DEFAULT 0,
    bloqueado_hasta             TIMESTAMP WITH TIME ZONE,
    -- Consentimiento de tratamiento de datos, Ley 1581 de 2012 (D2)
    terminos_version            VARCHAR(30),
    terminos_aceptados_en       TIMESTAMP WITH TIME ZONE,
    creado_en                   TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    actualizado_en              TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT uq_usuarios_correo UNIQUE (correo),
    CONSTRAINT ck_usuarios_correo_normalizado CHECK (correo = LOWER(correo) AND correo = TRIM(correo)),
    CONSTRAINT ck_usuarios_rol CHECK (rol IN ('ESTUDIANTE', 'TUTOR', 'ADMIN')),
    CONSTRAINT ck_usuarios_estado_cuenta
        CHECK (estado_cuenta IN ('PENDIENTE_VERIFICACION_CORREO', 'ACTIVA', 'SUSPENDIDA')),
    CONSTRAINT ck_usuarios_estado_verificacion_tutor
        CHECK (estado_verificacion_tutor IN ('PENDIENTE', 'VERIFICADO', 'RECHAZADO')),
    -- Un tutor siempre tiene estado de verificación; el resto de roles nunca
    CONSTRAINT ck_usuarios_verificacion_solo_tutor CHECK (
        (rol = 'TUTOR' AND estado_verificacion_tutor IS NOT NULL)
        OR (rol <> 'TUTOR' AND estado_verificacion_tutor IS NULL)),
    CONSTRAINT ck_usuarios_intentos CHECK (intentos_fallidos >= 0),
    -- Todo el que se registra acepta los términos; el administrador (creado por SQL) queda exento
    CONSTRAINT ck_usuarios_consentimiento CHECK (
        rol = 'ADMIN'
        OR (terminos_version IS NOT NULL AND terminos_aceptados_en IS NOT NULL))
);

-- Materias y modalidades que ofrece cada tutor (datos del registro, RF1; se usan en la búsqueda, RF3)
CREATE TABLE tutor_materias (
    usuario_id  UUID         NOT NULL,
    materia     VARCHAR(80)  NOT NULL,
    PRIMARY KEY (usuario_id, materia),
    CONSTRAINT fk_tutor_materias_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE
);

CREATE TABLE tutor_modalidades (
    usuario_id  UUID         NOT NULL,
    modalidad   VARCHAR(20)  NOT NULL,
    PRIMARY KEY (usuario_id, modalidad),
    CONSTRAINT fk_tutor_modalidades_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT ck_tutor_modalidades CHECK (modalidad IN ('PRESENCIAL', 'VIRTUAL'))
);

-- Códigos de 6 dígitos para verificar el correo y para restablecer la contraseña (D2).
-- Solo se guarda el hash del código, nunca el código.
CREATE TABLE codigos_verificacion (
    id            UUID                     PRIMARY KEY,
    usuario_id    UUID                     NOT NULL,
    proposito     VARCHAR(30)              NOT NULL,
    codigo_hash   VARCHAR(100)             NOT NULL,
    intentos      INTEGER                  NOT NULL DEFAULT 0,
    vence_en      TIMESTAMP WITH TIME ZONE NOT NULL,
    -- Se llena al usarse o al invalidarse (por reenvío o por agotar intentos): un código sirve una sola vez
    cerrado_en    TIMESTAMP WITH TIME ZONE,
    creado_en     TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_codigos_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios (id) ON DELETE CASCADE,
    CONSTRAINT ck_codigos_proposito CHECK (proposito IN ('VERIFICAR_CORREO', 'RESTABLECER_CONTRASENA')),
    CONSTRAINT ck_codigos_intentos CHECK (intentos >= 0)
);

-- Buscar el código vigente de un usuario y contar los envíos recientes (límite de reenvíos)
CREATE INDEX ix_codigos_usuario_proposito ON codigos_verificacion (usuario_id, proposito, creado_en);
