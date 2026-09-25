-- V5: Estrutura corporativa multi-tenant (SaaS), configuracao de operacao e login de administradores
-- Alinhado rigorosamente ao script do banco de dados e estrutura do PDF

-- 1. Expansão de Enums
ALTER TYPE public.tipo_usuario ADD VALUE IF NOT EXISTS 'administrador';

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'nivel_acesso_sistema') THEN
        CREATE TYPE public.nivel_acesso_sistema AS ENUM (
            'administrativo',
            'auditoria',
            'conducao'
        );
    END IF;

    IF NOT EXISTS (SELECT 1 FROM pg_type WHERE typname = 'tipo_categoria_cnh') THEN
        CREATE TYPE public.tipo_categoria_cnh AS ENUM (
            'a',
            'b',
            'c',
            'd',
            'e'
        );
    END IF;
END $$;

-- 2. Tabela de Governança: Catálogo de Dados
CREATE TABLE IF NOT EXISTS public.catalogo_dados (
    id SERIAL PRIMARY KEY,
    esquema_nome VARCHAR(50) DEFAULT 'public',
    tabela_nome VARCHAR(100) NOT NULL,
    coluna_nome VARCHAR(100),
    descricao TEXT NOT NULL,
    regra_negocio_critica TEXT,
    nivel_acesso_leitura VARCHAR(100) NOT NULL,
    nivel_acesso_escrita VARCHAR(100) NOT NULL,
    atualizado_em TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 3. Tabela Master de Empresas (Tenant SaaS)
CREATE TABLE IF NOT EXISTS public.empresa (
    id SERIAL PRIMARY KEY,
    endereco_id INTEGER REFERENCES public.endereco(id) ON DELETE SET NULL,
    nome VARCHAR(150),
    razao_social VARCHAR(150),
    codigo_interno VARCHAR(20) NOT NULL UNIQUE,
    email VARCHAR(150) UNIQUE,
    cnpj VARCHAR(14) UNIQUE NOT NULL,
    senha_hash VARCHAR(255),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_empresa_codigo ON public.empresa(codigo_interno);
CREATE INDEX IF NOT EXISTS idx_empresa_cnpj ON public.empresa(cnpj);
CREATE INDEX IF NOT EXISTS idx_empresa_email ON public.empresa(email);

-- 4. Singleton de Configuração Operacional (1:1 com Empresa)
CREATE TABLE IF NOT EXISTS public.configuracao_operacao (
    id SERIAL PRIMARY KEY,
    empresa_id INTEGER NOT NULL UNIQUE REFERENCES public.empresa(id) ON DELETE CASCADE,
    tempo_max_viagem_horas INTEGER NOT NULL DEFAULT 8,
    prazo_analise_horas INTEGER NOT NULL DEFAULT 24,
    meta_mortalidade NUMERIC(5,2) NOT NULL DEFAULT 0.5 CHECK (meta_mortalidade >= 0 AND meta_mortalidade <= 100),
    qtd_assinaturas_obrigatorias INTEGER NOT NULL DEFAULT 4,
    alerta_sirene_re BOOLEAN NOT NULL DEFAULT TRUE,
    alerta_inspecao_dias INTEGER NOT NULL DEFAULT 15,
    alerta_cnh_vencida BOOLEAN NOT NULL DEFAULT TRUE,
    alerta_tempo_parada_imprevista INTEGER NOT NULL DEFAULT 60,
    atualizado_em TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_configuracao_operacao_empresa ON public.configuracao_operacao(empresa_id);

-- 5. Tabela de Administradores vinculados à Empresa
CREATE TABLE IF NOT EXISTS public.empresa_admin (
    id SERIAL PRIMARY KEY,
    empresa_id INTEGER NOT NULL REFERENCES public.empresa(id) ON DELETE CASCADE,
    codigo_empresa VARCHAR(20) NOT NULL,
    cnpj_empresa VARCHAR(14) NOT NULL,
    nome VARCHAR(150) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    cpf VARCHAR(11) UNIQUE,
    telefone VARCHAR(20),
    cargo VARCHAR(100) DEFAULT 'Administrador',
    senha_hash VARCHAR(255) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_empresa_admin_empresa_id ON public.empresa_admin(empresa_id);
CREATE INDEX IF NOT EXISTS idx_empresa_admin_codigo ON public.empresa_admin(codigo_empresa);
CREATE INDEX IF NOT EXISTS idx_empresa_admin_email ON public.empresa_admin(email);

-- 6. Tabela de Auditoria Universal Multi-tenant
CREATE TABLE IF NOT EXISTS public.auditoria_log (
    id BIGSERIAL PRIMARY KEY,
    empresa_id INTEGER NOT NULL,
    tabela_afetada VARCHAR(100) NOT NULL,
    operacao VARCHAR(10) NOT NULL,
    usuario_db VARCHAR(100) NOT NULL,
    dados_antigos JSONB,
    dados_novos JSONB,
    data_hora TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_auditoria_log_empresa ON public.auditoria_log(empresa_id);

-- 7. Vínculo Multi-tenant nas tabelas de negócio existentes
ALTER TABLE public.usuario ADD COLUMN IF NOT EXISTS empresa_id INTEGER REFERENCES public.empresa(id) ON DELETE SET NULL;
ALTER TABLE public.veiculo_cavalo ADD COLUMN IF NOT EXISTS empresa_id INTEGER REFERENCES public.empresa(id) ON DELETE SET NULL;
ALTER TABLE public.veiculo_carreta ADD COLUMN IF NOT EXISTS empresa_id INTEGER REFERENCES public.empresa(id) ON DELETE SET NULL;
ALTER TABLE public.fazenda ADD COLUMN IF NOT EXISTS empresa_id INTEGER REFERENCES public.empresa(id) ON DELETE SET NULL;
ALTER TABLE public.unidade_frigorifica ADD COLUMN IF NOT EXISTS empresa_id INTEGER REFERENCES public.empresa(id) ON DELETE SET NULL;
ALTER TABLE public.relatorio_viagem ADD COLUMN IF NOT EXISTS empresa_id INTEGER REFERENCES public.empresa(id) ON DELETE SET NULL;
