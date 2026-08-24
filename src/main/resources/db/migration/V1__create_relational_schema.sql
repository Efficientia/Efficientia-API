-- Schema relacional inicial do Efficientia.
-- Em um banco existente, o Flyway cria o baseline e não executa esta migration.
-- Em um banco vazio, esta migration reproduz o modelo SQL versionado.

CREATE TYPE public.tipo_usuario AS ENUM (
    'motorista',
    'manobrista',
    'analista',
    'pecuarista',
    'curraleiro'
);

CREATE TYPE public.motivo_parada AS ENUM (
    'transbordo',
    'acidente_pista',
    'atoleiro',
    'problema_mecanico',
    'outro'
);

CREATE TYPE public.tipo_anomalia_embarque AS ENUM (
    'sangrando',
    'excesso_magreza_debilitado',
    'cutucoes_fortes',
    'mancando',
    'sujo_pisoteio',
    'tentativa_quebrar_cauda',
    'gaiola_cheia',
    'chute_paulada_ferrao',
    'arraste',
    'outro'
);

CREATE TYPE public.tipo_anomalia_desembarque AS ENUM (
    'gaiola_buraco_estrado_solto',
    'porteiras_nao_abrem',
    'uso_abusivo_choque',
    'problema_mecanico_veiculo',
    'outros_atos_abuso'
);

CREATE TABLE public.usuario (
    id SERIAL PRIMARY KEY,
    tipo public.tipo_usuario NOT NULL,
    cpf VARCHAR(11) NOT NULL UNIQUE,
    codigo_interno VARCHAR(50) UNIQUE,
    nome VARCHAR(150) NOT NULL,
    data_nascimento DATE,
    email VARCHAR(150) UNIQUE CHECK (email LIKE '%@%'),
    telefone VARCHAR(20) CHECK (LENGTH(telefone) >= 10),
    senha_hash VARCHAR(255) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE public.veiculo_cavalo (
    id SERIAL PRIMARY KEY,
    placa VARCHAR(7) NOT NULL UNIQUE,
    ativo BOOLEAN DEFAULT TRUE
);

CREATE TABLE public.veiculo_carreta (
    id SERIAL PRIMARY KEY,
    placa VARCHAR(7) NOT NULL UNIQUE,
    capacidade_cabecas INTEGER NOT NULL CHECK (capacidade_cabecas > 0)
);

CREATE TABLE public.endereco (
    id SERIAL PRIMARY KEY,
    cep VARCHAR(10) NOT NULL,
    logradouro VARCHAR(150) NOT NULL,
    numero VARCHAR(20) NOT NULL,
    cidade VARCHAR(100) NOT NULL,
    estado VARCHAR(2) NOT NULL
);

CREATE TABLE public.fazenda (
    id SERIAL PRIMARY KEY,
    pecuarista_id INTEGER NOT NULL REFERENCES public.usuario(id) ON DELETE RESTRICT,
    endereco_id INTEGER NOT NULL REFERENCES public.endereco(id) ON DELETE RESTRICT,
    nome VARCHAR(150) NOT NULL
);

CREATE TABLE public.unidade_frigorifica (
    id SERIAL PRIMARY KEY,
    analista_id INTEGER NOT NULL REFERENCES public.usuario(id) ON DELETE RESTRICT,
    endereco_id INTEGER NOT NULL REFERENCES public.endereco(id) ON DELETE RESTRICT,
    nome VARCHAR(150) NOT NULL
);

CREATE TABLE public.relatorio_viagem (
    id SERIAL PRIMARY KEY,
    fazenda_id INTEGER NOT NULL REFERENCES public.fazenda(id),
    motorista_id INTEGER NOT NULL REFERENCES public.usuario(id),
    manobrista_id INTEGER NOT NULL REFERENCES public.usuario(id),
    curraleiro_id INTEGER NOT NULL REFERENCES public.usuario(id),
    cavalo_id INTEGER NOT NULL REFERENCES public.veiculo_cavalo(id),
    carreta_id INTEGER NOT NULL REFERENCES public.veiculo_carreta(id),
    numero_gta VARCHAR(50) NOT NULL UNIQUE,
    numero_nota_fiscal VARCHAR(50) NOT NULL,
    data_embarque DATE NOT NULL,
    horario_embarque TIME NOT NULL,
    horario_saida_propriedade TIME NOT NULL,
    km_saida_embarcadouro INTEGER NOT NULL CHECK (km_saida_embarcadouro >= 0),
    data_chegada_unidade DATE NOT NULL,
    horario_chegada_unidade TIME NOT NULL,
    horario_desembarque TIME NOT NULL,
    km_chegada_desembarcadouro INTEGER NOT NULL,
    numero_curral VARCHAR(20) NOT NULL,
    CONSTRAINT chk_km_chegada CHECK (km_chegada_desembarcadouro >= km_saida_embarcadouro),
    sirene_re_funcionou BOOLEAN NOT NULL,
    qtd_machos INTEGER NOT NULL DEFAULT 0 CHECK (qtd_machos >= 0),
    qtd_femeas INTEGER NOT NULL DEFAULT 0 CHECK (qtd_femeas >= 0),
    qtd_marrucos INTEGER NOT NULL DEFAULT 0 CHECK (qtd_marrucos >= 0),
    qtd_em_pe INTEGER NOT NULL DEFAULT 0 CHECK (qtd_em_pe >= 0),
    qtd_deitado INTEGER NOT NULL DEFAULT 0 CHECK (qtd_deitado >= 0),
    qtd_morto INTEGER NOT NULL DEFAULT 0 CHECK (qtd_morto >= 0),
    qtd_emergencia INTEGER NOT NULL DEFAULT 0 CHECK (qtd_emergencia >= 0),
    motivo_emergencia TEXT,
    comentarios TEXT,
    url_assinatura_pecuarista VARCHAR(255) NOT NULL,
    url_assinatura_motorista VARCHAR(255) NOT NULL,
    url_assinatura_manobrista VARCHAR(255) NOT NULL,
    url_assinatura_curraleiro VARCHAR(255) NOT NULL,
    criado_em TIMESTAMP DEFAULT NOW()
);

CREATE TABLE public.parada_imprevista (
    id SERIAL PRIMARY KEY,
    relatorio_id INTEGER NOT NULL REFERENCES public.relatorio_viagem(id) ON DELETE CASCADE,
    motivo public.motivo_parada NOT NULL,
    data_hora_inicio TIMESTAMP NOT NULL,
    data_hora_fim TIMESTAMP NOT NULL,
    CONSTRAINT chk_data_parada CHECK (data_hora_fim > data_hora_inicio)
);

CREATE TABLE public.anomalia_embarque (
    id SERIAL PRIMARY KEY,
    relatorio_id INTEGER NOT NULL REFERENCES public.relatorio_viagem(id) ON DELETE CASCADE,
    anomalia public.tipo_anomalia_embarque NOT NULL,
    descricao_outros VARCHAR(150)
);

CREATE TABLE public.anomalia_desembarque (
    id SERIAL PRIMARY KEY,
    relatorio_id INTEGER NOT NULL REFERENCES public.relatorio_viagem(id) ON DELETE CASCADE,
    anomalia public.tipo_anomalia_desembarque NOT NULL,
    descricao_outros VARCHAR(150)
);

CREATE TABLE public.auditoria_analise (
    id SERIAL PRIMARY KEY,
    relatorio_id INTEGER NOT NULL REFERENCES public.relatorio_viagem(id) ON DELETE CASCADE,
    analista_id INTEGER NOT NULL REFERENCES public.usuario(id) ON DELETE RESTRICT,
    data_hora_analise TIMESTAMP NOT NULL DEFAULT NOW(),
    aprovado BOOLEAN NOT NULL,
    parecer_tecnico TEXT
);

CREATE INDEX idx_fazenda_pecuarista ON public.fazenda(pecuarista_id);
CREATE INDEX idx_unidade_analista ON public.unidade_frigorifica(analista_id);
CREATE INDEX idx_relatorio_viagem_motorista ON public.relatorio_viagem(motorista_id);
CREATE INDEX idx_relatorio_viagem_fazenda ON public.relatorio_viagem(fazenda_id);
CREATE INDEX idx_relatorio_viagem_datas ON public.relatorio_viagem(data_embarque, data_chegada_unidade);
CREATE INDEX idx_parada_imprevista_relatorio ON public.parada_imprevista(relatorio_id);
CREATE INDEX idx_anomalia_embarque_relatorio ON public.anomalia_embarque(relatorio_id);
CREATE INDEX idx_anomalia_desembarque_relatorio ON public.anomalia_desembarque(relatorio_id);
CREATE INDEX idx_auditoria_analise_relatorio ON public.auditoria_analise(relatorio_id);

CREATE OR REPLACE FUNCTION public.trg_valida_idade_usuario()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.data_nascimento IS NOT NULL
       AND NEW.data_nascimento > CURRENT_DATE - INTERVAL '18 years' THEN
        RAISE EXCEPTION 'O usuário deve ter pelo menos 18 anos de idade.';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_usuario_valida_idade
BEFORE INSERT OR UPDATE ON public.usuario
FOR EACH ROW EXECUTE FUNCTION public.trg_valida_idade_usuario();
