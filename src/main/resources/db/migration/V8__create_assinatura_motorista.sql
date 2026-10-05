-- V8: Assinatura fixa do motorista como imagem (PNG), versionamento imutavel e vinculo com documentos
-- Requisito Mobile e Web: persistencia em BYTEA, auditoria, SHA-256 e idempotencia

CREATE TABLE IF NOT EXISTS public.assinatura_motorista (
    id UUID PRIMARY KEY,
    motorista_id INTEGER NOT NULL,
    modalidade VARCHAR(30) NOT NULL,
    texto_origem VARCHAR(150),
    mime_type VARCHAR(50) NOT NULL,
    conteudo BYTEA NOT NULL,
    tamanho_bytes BIGINT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    idempotency_key UUID NOT NULL UNIQUE,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    criado_por INTEGER NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    versao BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_assinatura_motorista
        FOREIGN KEY (motorista_id) REFERENCES public.usuario(id) ON DELETE RESTRICT,
    CONSTRAINT fk_assinatura_motorista_criado_por
        FOREIGN KEY (criado_por) REFERENCES public.usuario(id) ON DELETE RESTRICT,
    CONSTRAINT chk_assinatura_modalidade
        CHECK (modalidade IN ('DESENHO', 'NOME_DIGITADO')),
    CONSTRAINT chk_assinatura_mime_type
        CHECK (mime_type = 'image/png'),
    CONSTRAINT chk_assinatura_tamanho
        CHECK (tamanho_bytes > 0),
    CONSTRAINT chk_assinatura_sha256
        CHECK (sha256 ~ '^[0-9A-Fa-f]{64}$')
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_assinatura_motorista_ativa
    ON public.assinatura_motorista(motorista_id)
    WHERE ativa = TRUE;

CREATE INDEX IF NOT EXISTS idx_assinatura_motorista_idempotency
    ON public.assinatura_motorista(idempotency_key);

DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'documento') THEN
        ALTER TABLE public.documento ADD COLUMN IF NOT EXISTS assinatura_motorista_id UUID NULL;
        IF NOT EXISTS (
            SELECT 1 FROM pg_constraint WHERE conname = 'fk_documento_assinatura_motorista'
        ) THEN
            ALTER TABLE public.documento
                ADD CONSTRAINT fk_documento_assinatura_motorista
                FOREIGN KEY (assinatura_motorista_id)
                REFERENCES public.assinatura_motorista(id)
                ON DELETE RESTRICT;
        END IF;
    END IF;
END $$;
