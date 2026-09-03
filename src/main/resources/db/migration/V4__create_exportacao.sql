CREATE TABLE public.exportacao (
    id UUID PRIMARY KEY,
    idempotency_key UUID NOT NULL,
    estado VARCHAR(20) NOT NULL,
    solicitado_por INTEGER,
    tamanho_origem_bytes BIGINT NOT NULL,
    tamanho_zip_bytes BIGINT,
    storage_key VARCHAR(500),
    nome_arquivo VARCHAR(255),
    erro_codigo VARCHAR(100),
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    iniciado_em TIMESTAMPTZ,
    concluido_em TIMESTAMPTZ,
    expira_em TIMESTAMPTZ,
    versao BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_exportacao_solicitado_por
        FOREIGN KEY (solicitado_por) REFERENCES public.usuario(id) ON DELETE RESTRICT,

    CONSTRAINT uq_exportacao_idempotency_key UNIQUE (idempotency_key),

    CONSTRAINT chk_exportacao_estado CHECK (
        estado IN ('NA_FILA', 'PROCESSANDO', 'CONCLUIDA', 'FALHA', 'EXPIRADA')
    ),
    CONSTRAINT chk_exportacao_tamanho_origem CHECK (
        tamanho_origem_bytes >= 0
    ),
    CONSTRAINT chk_exportacao_tamanho_zip CHECK (
        tamanho_zip_bytes IS NULL OR tamanho_zip_bytes > 0
    )
);

CREATE TABLE public.exportacao_documento (
    exportacao_id UUID NOT NULL,
    documento_id UUID NOT NULL,
    posicao INTEGER NOT NULL,

    CONSTRAINT pk_exportacao_documento PRIMARY KEY (exportacao_id, posicao),
    CONSTRAINT fk_exportacao_documento_exportacao
        FOREIGN KEY (exportacao_id) REFERENCES public.exportacao(id) ON DELETE CASCADE,
    CONSTRAINT fk_exportacao_documento_documento
        FOREIGN KEY (documento_id) REFERENCES public.documento(id) ON DELETE RESTRICT,
    CONSTRAINT chk_exportacao_documento_posicao CHECK (posicao >= 0)
);

CREATE INDEX idx_exportacao_estado_criado
    ON public.exportacao(estado, criado_em);

CREATE INDEX idx_exportacao_expira_em
    ON public.exportacao(expira_em)
    WHERE expira_em IS NOT NULL;

CREATE INDEX idx_exportacao_documento_documento
    ON public.exportacao_documento(documento_id);
