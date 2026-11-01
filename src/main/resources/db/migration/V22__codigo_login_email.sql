CREATE TABLE IF NOT EXISTS sc_corporativo.tb_codigo_login_email (
    email VARCHAR(150) NOT NULL,
    perfil VARCHAR(20) NOT NULL,
    codigo_hash VARCHAR(64) NOT NULL,
    expira_em TIMESTAMPTZ NOT NULL,
    proximo_envio_em TIMESTAMPTZ NOT NULL,
    tentativas INTEGER NOT NULL DEFAULT 0,
    PRIMARY KEY (email, perfil)
);
