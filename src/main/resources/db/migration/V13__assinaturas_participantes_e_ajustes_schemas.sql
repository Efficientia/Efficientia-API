-- V13: Suporte a assinaturas de participantes da viagem e ajustes de schema
-- 1. Garante colunas de tb_usuario em sc_corporativo
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'sc_corporativo' AND table_name = 'tb_usuario') THEN
        ALTER TABLE sc_corporativo.tb_usuario ADD COLUMN IF NOT EXISTS url_assinatura_geral TEXT;
        ALTER TABLE sc_corporativo.tb_usuario ADD COLUMN IF NOT EXISTS cargo VARCHAR(150);
        ALTER TABLE sc_corporativo.tb_usuario ADD COLUMN IF NOT EXISTS nivel_acesso VARCHAR(50);
        ALTER TABLE sc_corporativo.tb_usuario ADD COLUMN IF NOT EXISTS cnh_numero VARCHAR(20);
        ALTER TABLE sc_corporativo.tb_usuario ADD COLUMN IF NOT EXISTS categoria_cnh VARCHAR(5);
        ALTER TABLE sc_corporativo.tb_usuario ADD COLUMN IF NOT EXISTS data_vencimento_cnh DATE;
        ALTER TABLE sc_corporativo.tb_usuario ADD COLUMN IF NOT EXISTS nome_completo VARCHAR(250);
        ALTER TABLE sc_corporativo.tb_usuario ADD COLUMN IF NOT EXISTS status_cadastro VARCHAR(20) DEFAULT 'ativo';
        ALTER TABLE sc_corporativo.tb_usuario ADD COLUMN IF NOT EXISTS empresa_id INTEGER;
        ALTER TABLE sc_corporativo.tb_usuario ADD COLUMN IF NOT EXISTS codigo_interno VARCHAR(50);
        ALTER TABLE sc_corporativo.tb_usuario ADD COLUMN IF NOT EXISTS data_nascimento DATE;
        ALTER TABLE sc_corporativo.tb_usuario ADD COLUMN IF NOT EXISTS telefone VARCHAR(20);
    END IF;
END $$;

-- 2. Atualiza a constraint de papel_assinante na tabela documento para suportar PECUARISTA
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'documento') THEN
        IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_documento_papel_assinante') THEN
            ALTER TABLE public.documento DROP CONSTRAINT chk_documento_papel_assinante;
        END IF;

        ALTER TABLE public.documento ADD CONSTRAINT chk_documento_papel_assinante CHECK (
            papel_assinante IS NULL OR papel_assinante IN ('MOTORISTA', 'MANOBRISTA', 'CURRALEIRO', 'FUNCIONARIO_FRIBOI', 'PECUARISTA')
        );
    END IF;
END $$;

-- 3. Garante e alinha a tabela public.assinatura_motorista
CREATE TABLE IF NOT EXISTS public.assinatura_motorista (
    id UUID PRIMARY KEY,
    motorista_id INTEGER NOT NULL,
    modalidade VARCHAR(30) NOT NULL,
    texto_origem VARCHAR(150),
    mime_type VARCHAR(50) NOT NULL DEFAULT 'image/png',
    conteudo BYTEA NOT NULL,
    tamanho_bytes BIGINT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    idempotency_key UUID NOT NULL UNIQUE,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    criado_por INTEGER NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    versao BIGINT NOT NULL DEFAULT 0
);

-- 4. Redefine as Foreign Keys de assinatura_motorista para sc_corporativo.tb_usuario
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'assinatura_motorista')
       AND EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'sc_corporativo' AND table_name = 'tb_usuario') THEN
        
        IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_assinatura_motorista') THEN
            ALTER TABLE public.assinatura_motorista DROP CONSTRAINT fk_assinatura_motorista;
        END IF;
        IF EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_assinatura_motorista_criado_por') THEN
            ALTER TABLE public.assinatura_motorista DROP CONSTRAINT fk_assinatura_motorista_criado_por;
        END IF;

        ALTER TABLE public.assinatura_motorista
            ADD CONSTRAINT fk_assinatura_motorista
            FOREIGN KEY (motorista_id) REFERENCES sc_corporativo.tb_usuario(id) ON DELETE RESTRICT;

        ALTER TABLE public.assinatura_motorista
            ADD CONSTRAINT fk_assinatura_motorista_criado_por
            FOREIGN KEY (criado_por) REFERENCES sc_corporativo.tb_usuario(id) ON DELETE RESTRICT;
    END IF;
END $$;

-- 5. Garante view de compatibilidade public.usuario caso não exista
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'sc_corporativo' AND table_name = 'tb_usuario')
       AND NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_schema = 'public' AND table_name = 'usuario' AND table_type = 'BASE TABLE') THEN
        CREATE OR REPLACE VIEW public.usuario AS SELECT * FROM sc_corporativo.tb_usuario;
    END IF;
END $$;
