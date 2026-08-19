CREATE TABLE IF NOT EXISTS usuarios (
    id                 BIGSERIAL        PRIMARY KEY,
    nombres            VARCHAR(100)     NOT NULL,
    apellidos          VARCHAR(100)     NOT NULL,
    username           VARCHAR(50)      NOT NULL,
    email              VARCHAR(150)     NOT NULL,
    password           VARCHAR(255)     NOT NULL,
    estado             VARCHAR(20)      NOT NULL DEFAULT 'ACTIVO',
    fecha_creacion     TIMESTAMPTZ      NOT NULL,
    fecha_modificacion TIMESTAMPTZ      NOT NULL,
    CONSTRAINT uq_usuarios_username UNIQUE (username),
    CONSTRAINT uq_usuarios_email    UNIQUE (email),
    CONSTRAINT ck_usuarios_estado   CHECK (estado IN ('ACTIVO', 'INACTIVO'))
);

CREATE INDEX IF NOT EXISTS idx_usuarios_username ON usuarios (username);
CREATE INDEX IF NOT EXISTS idx_usuarios_email    ON usuarios (email);
CREATE INDEX IF NOT EXISTS idx_usuarios_estado   ON usuarios (estado);
