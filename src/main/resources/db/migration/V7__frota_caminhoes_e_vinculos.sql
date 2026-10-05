-- V7: Extensao da frota de caminhoes, auditoria de inspecao e ciclo de vida do relatorio de viagem
-- Alinhado rigorosamente a arquitetura do banco de dados (sc_frota e sc_operacao)

-- 0. Garantir a existencia previa das tabelas relacionais caso o banco venha de baseline parcial
CREATE TABLE IF NOT EXISTS public.veiculo_cavalo (
    id SERIAL PRIMARY KEY,
    placa VARCHAR(7) NOT NULL UNIQUE,
    ativo BOOLEAN DEFAULT TRUE,
    empresa_id INTEGER
);

CREATE TABLE IF NOT EXISTS public.veiculo_carreta (
    id SERIAL PRIMARY KEY,
    placa VARCHAR(7) NOT NULL UNIQUE,
    capacidade_cabecas INTEGER NOT NULL DEFAULT 30,
    ativo BOOLEAN DEFAULT TRUE,
    empresa_id INTEGER
);

CREATE TABLE IF NOT EXISTS public.relatorio_viagem (
    id SERIAL PRIMARY KEY,
    fazenda_id INTEGER,
    motorista_id INTEGER,
    manobrista_id INTEGER,
    curraleiro_id INTEGER,
    cavalo_id INTEGER,
    carreta_id INTEGER,
    numero_gta VARCHAR(50) UNIQUE,
    numero_nota_fiscal VARCHAR(50),
    data_embarque DATE,
    horario_embarque TIME,
    horario_saida_propriedade TIME,
    km_saida_embarcadouro INTEGER,
    data_chegada_unidade DATE,
    horario_chegada_unidade TIME,
    horario_desembarque TIME,
    km_chegada_desembarcadouro INTEGER,
    numero_curral VARCHAR(20),
    sirene_re_funcionou BOOLEAN DEFAULT TRUE,
    qtd_machos INTEGER DEFAULT 0,
    qtd_femeas INTEGER DEFAULT 0,
    qtd_marrucos INTEGER DEFAULT 0,
    qtd_em_pe INTEGER DEFAULT 0,
    qtd_deitado INTEGER DEFAULT 0,
    qtd_morto INTEGER DEFAULT 0,
    qtd_emergencia INTEGER DEFAULT 0,
    motivo_emergencia TEXT,
    comentarios TEXT,
    url_assinatura_pecuarista TEXT,
    url_assinatura_motorista TEXT,
    url_assinatura_manobrista TEXT,
    url_assinatura_curraleiro TEXT,
    empresa_id INTEGER,
    criado_em TIMESTAMPTZ DEFAULT CURRENT_TIMESTAMP
);

-- 1. Atributos complementares de Cavalo Mecanico
ALTER TABLE public.veiculo_cavalo ADD COLUMN IF NOT EXISTS data_vencimento_inspecao DATE DEFAULT (CURRENT_DATE + INTERVAL '30 days');
ALTER TABLE public.veiculo_cavalo ADD COLUMN IF NOT EXISTS km_acumulado INTEGER DEFAULT 0;
ALTER TABLE public.veiculo_cavalo ADD COLUMN IF NOT EXISTS marca VARCHAR(50);
ALTER TABLE public.veiculo_cavalo ADD COLUMN IF NOT EXISTS modelo VARCHAR(50);
ALTER TABLE public.veiculo_cavalo ADD COLUMN IF NOT EXISTS ano_fabricacao INTEGER;

-- 2. Atributos complementares de Carreta / Gaiola
ALTER TABLE public.veiculo_carreta ADD COLUMN IF NOT EXISTS data_vencimento_inspecao DATE DEFAULT (CURRENT_DATE + INTERVAL '30 days');
ALTER TABLE public.veiculo_carreta ADD COLUMN IF NOT EXISTS ativo BOOLEAN DEFAULT TRUE;
ALTER TABLE public.veiculo_carreta ADD COLUMN IF NOT EXISTS marca VARCHAR(50);
ALTER TABLE public.veiculo_carreta ADD COLUMN IF NOT EXISTS modelo VARCHAR(50);
ALTER TABLE public.veiculo_carreta ADD COLUMN IF NOT EXISTS tipo_carreta VARCHAR(50);

-- 3. Ciclo de vida e status no Relatorio de Viagem
ALTER TABLE public.relatorio_viagem ADD COLUMN IF NOT EXISTS status VARCHAR(20) NOT NULL DEFAULT 'rascunho';
ALTER TABLE public.relatorio_viagem ADD COLUMN IF NOT EXISTS enviado_em TIMESTAMPTZ;
ALTER TABLE public.relatorio_viagem ADD COLUMN IF NOT EXISTS finalizado_em TIMESTAMPTZ;

-- 4. Indices de performance para consultas por placa, empresa, motorista e veiculo
CREATE INDEX IF NOT EXISTS idx_veiculo_cavalo_placa ON public.veiculo_cavalo(placa);
CREATE INDEX IF NOT EXISTS idx_veiculo_cavalo_empresa ON public.veiculo_cavalo(empresa_id);
CREATE INDEX IF NOT EXISTS idx_veiculo_carreta_placa ON public.veiculo_carreta(placa);
CREATE INDEX IF NOT EXISTS idx_veiculo_carreta_empresa ON public.veiculo_carreta(empresa_id);
CREATE INDEX IF NOT EXISTS idx_relatorio_viagem_motorista ON public.relatorio_viagem(motorista_id);
CREATE INDEX IF NOT EXISTS idx_relatorio_viagem_cavalo ON public.relatorio_viagem(cavalo_id);
CREATE INDEX IF NOT EXISTS idx_relatorio_viagem_carreta ON public.relatorio_viagem(carreta_id);
CREATE INDEX IF NOT EXISTS idx_relatorio_viagem_status ON public.relatorio_viagem(status);
