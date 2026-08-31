CREATE TABLE public.documento_auditoria (
    id UUID PRIMARY KEY,
    documento_id UUID NOT NULL,
    operacao VARCHAR(30) NOT NULL,
    usuario_id INTEGER,
    detalhes VARCHAR(500),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT chk_documento_auditoria_operacao
        CHECK (operacao IN ('ATUALIZACAO', 'EXCLUSAO'))
);

CREATE INDEX idx_documento_auditoria_documento_criado
    ON public.documento_auditoria(documento_id, criado_em DESC);
