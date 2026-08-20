CREATE TABLE IF NOT EXISTS personas (
    id                 BIGSERIAL        PRIMARY KEY,
    tipo_documento     VARCHAR(20)      NOT NULL,
    numero_documento   VARCHAR(50)      NOT NULL,
    complemento        VARCHAR(10)      NULL,
    fecha_nacimiento   DATE             NOT NULL,
    apellido_paterno   VARCHAR(100)     NULL,
    apellido_materno   VARCHAR(100)     NULL,
    apellido_esposo    VARCHAR(100)     NULL,
    nombres            VARCHAR(200)     NOT NULL,
    genero             VARCHAR(20)      NOT NULL,
    estado_civil       VARCHAR(20)      NOT NULL,
    fecha_creacion     TIMESTAMPTZ      NOT NULL,
    fecha_modificacion TIMESTAMPTZ      NOT NULL,

    -- Unicidad documental: NULL en complemento participa como cadena vacía.
    -- Se usa COALESCE para compatibilidad con PostgreSQL < 15.
    -- Alternativa en PostgreSQL 15+: UNIQUE NULLS NOT DISTINCT (tipo_documento, numero_documento, complemento)
    CONSTRAINT uq_personas_documento
        UNIQUE (tipo_documento, numero_documento, COALESCE(complemento, '')),

    CONSTRAINT ck_personas_tipo_documento
        CHECK (tipo_documento IN ('CI', 'PASAPORTE', 'CEX', 'NIT')),

    CONSTRAINT ck_personas_genero
        CHECK (genero IN ('MASCULINO', 'FEMENINO', 'OTRO')),

    CONSTRAINT ck_personas_estado_civil
        CHECK (estado_civil IN ('SOLTERO', 'CASADO', 'DIVORCIADO', 'VIUDO', 'UNION_LIBRE'))
);

CREATE INDEX IF NOT EXISTS idx_personas_tipo_documento
    ON personas (tipo_documento);

CREATE INDEX IF NOT EXISTS idx_personas_numero_documento
    ON personas (numero_documento);

CREATE INDEX IF NOT EXISTS idx_personas_documento_completo
    ON personas (tipo_documento, numero_documento, COALESCE(complemento, ''));
