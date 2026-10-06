-- V9: Adaptacao da API ao banco de dados e refinamento do processo de assinaturas
-- Alinhado rigorosamente a modelagem relacional de efficientia (2).pdf (sc_corporativo, sc_frota, sc_operacao, sc_auditoria)

-- 1. Criacao dos esquemas oficiais do banco de dados
CREATE SCHEMA IF NOT EXISTS sc_auditoria;
CREATE SCHEMA IF NOT EXISTS sc_corporativo;
CREATE SCHEMA IF NOT EXISTS sc_frota;
CREATE SCHEMA IF NOT EXISTS sc_operacao;

-- 2. Adaptacao da tabela de usuarios (sc_corporativo.tb_usuario)
CREATE TABLE IF NOT EXISTS public.usuario (
    id SERIAL PRIMARY KEY,
    cpf VARCHAR(11) NOT NULL UNIQUE,
    nome VARCHAR(150) NOT NULL,
    senha_hash VARCHAR(255) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

ALTER TABLE public.usuario ADD COLUMN IF NOT EXISTS url_assinatura_geral TEXT;
ALTER TABLE public.usuario ADD COLUMN IF NOT EXISTS cargo VARCHAR(150);
ALTER TABLE public.usuario ADD COLUMN IF NOT EXISTS nivel_acesso VARCHAR(50);
ALTER TABLE public.usuario ADD COLUMN IF NOT EXISTS cnh_numero VARCHAR(20);
ALTER TABLE public.usuario ADD COLUMN IF NOT EXISTS categoria_cnh VARCHAR(5);
ALTER TABLE public.usuario ADD COLUMN IF NOT EXISTS data_vencimento_cnh DATE;
ALTER TABLE public.usuario ADD COLUMN IF NOT EXISTS nome_completo VARCHAR(250);
ALTER TABLE public.usuario ADD COLUMN IF NOT EXISTS status_cadastro VARCHAR(20) DEFAULT 'ativo';

UPDATE public.usuario
SET nome_completo = nome
WHERE nome_completo IS NULL AND nome IS NOT NULL;

-- 3. Adaptacao do Relatorio de Viagem (sc_operacao.tb_relatorio_viagem)
-- Assinaturas tornam-se TEXT e opcionais no cadastro inicial para suportar coleta progressiva em campo
CREATE TABLE IF NOT EXISTS public.relatorio_viagem (
    id SERIAL PRIMARY KEY,
    numero_gta VARCHAR(50) UNIQUE,
    data_embarque DATE,
    criado_em TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE public.relatorio_viagem ADD COLUMN IF NOT EXISTS url_assinatura_pecuarista TEXT;
ALTER TABLE public.relatorio_viagem ADD COLUMN IF NOT EXISTS url_assinatura_motorista TEXT;
ALTER TABLE public.relatorio_viagem ADD COLUMN IF NOT EXISTS url_assinatura_manobrista TEXT;
ALTER TABLE public.relatorio_viagem ADD COLUMN IF NOT EXISTS url_assinatura_curraleiro TEXT;
ALTER TABLE public.relatorio_viagem ADD COLUMN IF NOT EXISTS capacidade_carga_utilizada INTEGER;
ALTER TABLE public.relatorio_viagem ADD COLUMN IF NOT EXISTS url_laudo_mortalidade TEXT;
ALTER TABLE public.relatorio_viagem ADD COLUMN IF NOT EXISTS unidade_frigorifica_id INTEGER;
ALTER TABLE public.relatorio_viagem ADD COLUMN IF NOT EXISTS idempotency_key UUID;
ALTER TABLE public.relatorio_viagem ADD COLUMN IF NOT EXISTS atualizado_em TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP;

CREATE UNIQUE INDEX IF NOT EXISTS uq_relatorio_viagem_idempotency
    ON public.relatorio_viagem(idempotency_key)
    WHERE idempotency_key IS NOT NULL;
CREATE INDEX IF NOT EXISTS idx_relatorio_viagem_unidade ON public.relatorio_viagem(unidade_frigorifica_id);

-- Garantir a existencia e evolucao das tabelas de paradas e anomalias
CREATE TABLE IF NOT EXISTS public.parada_imprevista (
    id SERIAL PRIMARY KEY,
    relatorio_id INTEGER NOT NULL REFERENCES public.relatorio_viagem(id) ON DELETE CASCADE,
    motivo VARCHAR(50) NOT NULL,
    data_hora_inicio TIMESTAMP NOT NULL,
    data_hora_fim TIMESTAMP NOT NULL
);

CREATE TABLE IF NOT EXISTS public.anomalia_embarque (
    id SERIAL PRIMARY KEY,
    relatorio_id INTEGER NOT NULL REFERENCES public.relatorio_viagem(id) ON DELETE CASCADE,
    anomalia VARCHAR(50) NOT NULL,
    descricao_outros VARCHAR(150),
    quantidade_animais INTEGER DEFAULT 1
);
ALTER TABLE public.anomalia_embarque ADD COLUMN IF NOT EXISTS quantidade_animais INTEGER DEFAULT 1;

CREATE TABLE IF NOT EXISTS public.anomalia_desembarque (
    id SERIAL PRIMARY KEY,
    relatorio_id INTEGER NOT NULL REFERENCES public.relatorio_viagem(id) ON DELETE CASCADE,
    anomalia VARCHAR(50) NOT NULL,
    descricao_outros VARCHAR(150),
    quantidade_animais INTEGER DEFAULT 1
);
ALTER TABLE public.anomalia_desembarque ADD COLUMN IF NOT EXISTS quantidade_animais INTEGER DEFAULT 1;
DO $$
BEGIN
    -- Permitir que colunas de assinaturas sejam nulas para coleta progressiva
    BEGIN
        ALTER TABLE public.relatorio_viagem ALTER COLUMN url_assinatura_pecuarista TYPE TEXT;
        ALTER TABLE public.relatorio_viagem ALTER COLUMN url_assinatura_pecuarista DROP NOT NULL;
    EXCEPTION WHEN OTHERS THEN NULL;
    END;

    BEGIN
        ALTER TABLE public.relatorio_viagem ALTER COLUMN url_assinatura_motorista TYPE TEXT;
        ALTER TABLE public.relatorio_viagem ALTER COLUMN url_assinatura_motorista DROP NOT NULL;
    EXCEPTION WHEN OTHERS THEN NULL;
    END;

    BEGIN
        ALTER TABLE public.relatorio_viagem ALTER COLUMN url_assinatura_manobrista TYPE TEXT;
        ALTER TABLE public.relatorio_viagem ALTER COLUMN url_assinatura_manobrista DROP NOT NULL;
    EXCEPTION WHEN OTHERS THEN NULL;
    END;

    BEGIN
        ALTER TABLE public.relatorio_viagem ALTER COLUMN url_assinatura_curraleiro TYPE TEXT;
        ALTER TABLE public.relatorio_viagem ALTER COLUMN url_assinatura_curraleiro DROP NOT NULL;
    EXCEPTION WHEN OTHERS THEN NULL;
    END;
END $$;

-- 4. Adaptacao de Configuracao de Operacao (sc_corporativo.tb_configuracao_operacao)
CREATE TABLE IF NOT EXISTS public.configuracao_operacao (
    id SERIAL PRIMARY KEY,
    tempo_max_viagem_horas INTEGER NOT NULL DEFAULT 8,
    qtd_assinaturas_obrigatorias INTEGER NOT NULL DEFAULT 4
);

ALTER TABLE public.configuracao_operacao ADD COLUMN IF NOT EXISTS mortalidade_bloqueio_cabecas INTEGER DEFAULT 3;
ALTER TABLE public.configuracao_operacao ADD COLUMN IF NOT EXISTS qtd_assinaturas_obrigatorias INTEGER DEFAULT 4;

-- 5. Tabelas auxiliares e de dominio descritas no modelo ER
CREATE TABLE IF NOT EXISTS sc_operacao.tb_dominio_motivo_parada (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    codigo VARCHAR(50) NOT NULL UNIQUE,
    descricao VARCHAR(150) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS sc_operacao.tb_dominio_anomalia (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tipo_etapa VARCHAR(20) NOT NULL,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    descricao VARCHAR(150) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS sc_auditoria.tb_monitoramento_dau (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    usuario_id INTEGER,
    data_acesso DATE NOT NULL DEFAULT CURRENT_DATE
);

CREATE TABLE IF NOT EXISTS sc_frota.tb_veiculo_base (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID,
    placa VARCHAR(7) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    data_vencimento_inspecao DATE
);

-- 6. Views de compatibilidade nos schemas do PDF apontando para as tabelas existentes.
-- Bancos que ja possuem tabelas fisicas com esses nomes devem conserva-las: PostgreSQL nao
-- permite substituir uma tabela por uma view com CREATE OR REPLACE VIEW.
DO $$
DECLARE
    alias_registro RECORD;
    relacao_destino OID;
BEGIN
    FOR alias_registro IN
        SELECT * FROM (VALUES
            ('sc_corporativo', 'tb_usuario', 'usuario'),
            ('sc_corporativo', 'tb_empresa', 'empresa'),
            ('sc_corporativo', 'tb_endereco', 'endereco'),
            ('sc_corporativo', 'tb_configuracao_operacao', 'configuracao_operacao'),
            ('sc_corporativo', 'tb_fazenda', 'fazenda'),
            ('sc_corporativo', 'tb_unidade_frigorifica', 'unidade_frigorifica'),
            ('sc_frota', 'tb_veiculo_cavalo', 'veiculo_cavalo'),
            ('sc_frota', 'tb_veiculo_carreta', 'veiculo_carreta'),
            ('sc_operacao', 'tb_relatorio_viagem', 'relatorio_viagem'),
            ('sc_operacao', 'tb_parada_imprevista', 'parada_imprevista'),
            ('sc_operacao', 'tb_anomalia_embarque', 'anomalia_embarque'),
            ('sc_operacao', 'tb_anomalia_desembarque', 'anomalia_desembarque'),
            ('sc_operacao', 'tb_auditoria_analise', 'auditoria_analise'),
            ('sc_auditoria', 'tb_catalogo_dados', 'catalogo_dados'),
            ('sc_auditoria', 'tb_auditoria_log', 'auditoria_log')
        ) AS aliases(schema_destino, tabela_destino, tabela_origem)
    LOOP
        IF to_regclass(format('%I.%I', 'public', alias_registro.tabela_origem)) IS NOT NULL THEN
            relacao_destino := to_regclass(
                format('%I.%I', alias_registro.schema_destino, alias_registro.tabela_destino)
            );

            IF relacao_destino IS NULL OR EXISTS (
                SELECT 1 FROM pg_class
                WHERE oid = relacao_destino AND relkind = 'v'
            ) THEN
                EXECUTE format(
                    'CREATE OR REPLACE VIEW %I.%I AS SELECT * FROM %I.%I',
                    alias_registro.schema_destino,
                    alias_registro.tabela_destino,
                    'public',
                    alias_registro.tabela_origem
                );
            ELSE
                RAISE NOTICE 'Mantendo relacao existente %.%; a view de compatibilidade nao sera criada',
                    alias_registro.schema_destino,
                    alias_registro.tabela_destino;
            END IF;
        END IF;
    END LOOP;
END $$;
