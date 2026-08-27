-- Metadados dos documentos pertencentes à API REST principal.
-- O conteúdo binário fica no storage privado; o PostgreSQL guarda somente
-- metadados e, quando aplicável, a assinatura textual curta.

CREATE TABLE public.documento (
    id UUID PRIMARY KEY,
    viagem_id INTEGER NOT NULL,
    tipo_documento VARCHAR(40) NOT NULL,
    origem VARCHAR(30) NOT NULL,
    assinante_id INTEGER,
    papel_assinante VARCHAR(30),
    modalidade_assinatura VARCHAR(20),
    texto_assinatura VARCHAR(150),
    descricao VARCHAR(300),
    nome_original VARCHAR(255),
    mime_type VARCHAR(50),
    tamanho_bytes BIGINT,
    sha256 CHAR(64),
    storage_key VARCHAR(500),
    idempotency_key UUID NOT NULL,
    criado_por INTEGER,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    versao BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_documento_viagem
        FOREIGN KEY (viagem_id) REFERENCES public.relatorio_viagem(id) ON DELETE RESTRICT,
    CONSTRAINT fk_documento_assinante
        FOREIGN KEY (assinante_id) REFERENCES public.usuario(id) ON DELETE RESTRICT,
    CONSTRAINT fk_documento_criado_por
        FOREIGN KEY (criado_por) REFERENCES public.usuario(id) ON DELETE RESTRICT,

    CONSTRAINT uq_documento_storage_key UNIQUE (storage_key),
    CONSTRAINT uq_documento_idempotency_key UNIQUE (idempotency_key),

    CONSTRAINT chk_documento_tipo CHECK (
        tipo_documento IN (
            'RELATORIO_VIAGEM',
            'BOLETIM_EMBARQUE',
            'BOLETIM_DESEMBARQUE',
            'ASSINATURA'
        )
    ),
    CONSTRAINT chk_documento_origem CHECK (
        origem IN ('UPLOAD', 'CAMERA', 'DESENHO', 'TEXTO', 'GERADO_SISTEMA')
    ),
    CONSTRAINT chk_documento_papel_assinante CHECK (
        papel_assinante IS NULL OR papel_assinante IN (
            'MOTORISTA', 'MANOBRISTA', 'CURRALEIRO', 'FUNCIONARIO_FRIBOI'
        )
    ),
    CONSTRAINT chk_documento_modalidade_assinatura CHECK (
        modalidade_assinatura IS NULL
        OR modalidade_assinatura IN ('FOTO', 'DESENHO', 'TEXTO')
    ),
    CONSTRAINT chk_documento_mime_type CHECK (
        mime_type IS NULL OR mime_type IN ('application/pdf', 'image/png')
    ),
    CONSTRAINT chk_documento_tamanho CHECK (
        tamanho_bytes IS NULL OR tamanho_bytes > 0
    ),
    CONSTRAINT chk_documento_sha256 CHECK (
        sha256 IS NULL OR sha256 ~ '^[0-9A-Fa-f]{64}$'
    ),
    CONSTRAINT chk_documento_assinante CHECK (
        (
            tipo_documento = 'ASSINATURA'
            AND assinante_id IS NOT NULL
            AND papel_assinante IS NOT NULL
            AND modalidade_assinatura IS NOT NULL
        )
        OR
        (
            tipo_documento <> 'ASSINATURA'
            AND assinante_id IS NULL
            AND papel_assinante IS NULL
            AND modalidade_assinatura IS NULL
        )
    ),
    CONSTRAINT chk_documento_conteudo_exclusivo CHECK (
        (
            modalidade_assinatura = 'TEXTO'
            AND texto_assinatura IS NOT NULL
            AND LENGTH(TRIM(texto_assinatura)) BETWEEN 1 AND 150
            AND nome_original IS NULL
            AND mime_type IS NULL
            AND tamanho_bytes IS NULL
            AND sha256 IS NULL
            AND storage_key IS NULL
        )
        OR
        (
            modalidade_assinatura IS DISTINCT FROM 'TEXTO'
            AND texto_assinatura IS NULL
            AND nome_original IS NOT NULL
            AND LENGTH(TRIM(nome_original)) BETWEEN 1 AND 255
            AND mime_type IS NOT NULL
            AND tamanho_bytes IS NOT NULL
            AND sha256 IS NOT NULL
            AND storage_key IS NOT NULL
            AND LENGTH(TRIM(storage_key)) BETWEEN 1 AND 500
        )
    ),
    CONSTRAINT chk_documento_texto_origem CHECK (
        modalidade_assinatura IS DISTINCT FROM 'TEXTO' OR origem = 'TEXTO'
    ),
    CONSTRAINT chk_documento_assinatura_binaria_png CHECK (
        tipo_documento <> 'ASSINATURA'
        OR modalidade_assinatura = 'TEXTO'
        OR mime_type = 'image/png'
    )
);

CREATE INDEX idx_documento_viagem_criado
    ON public.documento(viagem_id, criado_em DESC);

CREATE INDEX idx_documento_assinante_criado
    ON public.documento(assinante_id, criado_em DESC)
    WHERE assinante_id IS NOT NULL;

CREATE INDEX idx_documento_cursor
    ON public.documento(criado_em, id);

CREATE INDEX idx_documento_sha256
    ON public.documento(sha256)
    WHERE sha256 IS NOT NULL;
