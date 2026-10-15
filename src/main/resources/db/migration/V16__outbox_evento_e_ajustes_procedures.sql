-- ==============================================================================
-- V16: Transactional Outbox Pattern, Trigger de Alocacao e Ajustes de Procedures
-- ==============================================================================

-- 1. Garantir criacao do schema sc_operacao caso nao exista
CREATE SCHEMA IF NOT EXISTS sc_operacao;
CREATE SCHEMA IF NOT EXISTS sc_auditoria;

-- 2. Criacao da Tabela de Mensageria: sc_operacao.tb_outbox_evento
CREATE TABLE IF NOT EXISTS sc_operacao.tb_outbox_evento (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    empresa_id UUID NOT NULL,
    tipo_evento VARCHAR(100) NOT NULL,
    agregado_tipo VARCHAR(50) NOT NULL,
    agregado_id UUID NOT NULL,
    payload JSONB NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processado_em TIMESTAMPTZ,
    erro_tentativas INTEGER NOT NULL DEFAULT 0,
    ultimo_erro TEXT
);

-- 3. Indice Parcial de Alto Desempenho para Eventos Pendentes
CREATE INDEX IF NOT EXISTS idx_outbox_pendentes
    ON sc_operacao.tb_outbox_evento (criado_em)
    WHERE processado_em IS NULL;

-- 4. Funcao Autonoma de Geracao de Eventos Outbox para Relatorio de Viagem
CREATE OR REPLACE FUNCTION sc_operacao.fn_gerar_outbox_relatorio()
RETURNS TRIGGER AS $$
DECLARE
    v_tipo_evento VARCHAR(100);
    v_empresa_id UUID;
    v_agregado_id UUID;
    v_total_embarcados INTEGER;
    v_payload JSONB;
BEGIN
    -- Se for UPDATE e o status nao foi alterado, ignora e nao gera evento
    IF TG_OP = 'UPDATE' THEN
        IF NEW.status IS NOT DISTINCT FROM OLD.status THEN
            RETURN NEW;
        END IF;
        v_tipo_evento := 'relatorio.status_alterado';
    ELSE
        v_tipo_evento := 'relatorio.criado';
    END IF;

    -- Converte empresa_id para UUID (se ja for UUID usa direto, se for numerico gera uuid v5 compativel ou uuid random/padrao)
    BEGIN
        IF NEW.empresa_id IS NOT NULL THEN
            v_empresa_id := NEW.empresa_id::text::uuid;
        ELSE
            v_empresa_id := '00000000-0000-0000-0000-000000000000'::uuid;
        END IF;
    EXCEPTION WHEN OTHERS THEN
        -- Caso seja INTEGER na tabela fisica, converte deterministicamente em UUID
        v_empresa_id := ('00000000-0000-0000-0000-' || LPAD(TO_HEX(COALESCE(NEW.empresa_id, 0)), 12, '0'))::uuid;
    END;

    -- Converte agregado_id para UUID
    BEGIN
        v_agregado_id := NEW.id::text::uuid;
    EXCEPTION WHEN OTHERS THEN
        v_agregado_id := ('00000000-0000-0000-0000-' || LPAD(TO_HEX(COALESCE(NEW.id, 0)), 12, '0'))::uuid;
    END;

    -- Calculo do total de animais embarcados
    v_total_embarcados := COALESCE(NEW.qtd_machos, 0) + COALESCE(NEW.qtd_femeas, 0) + COALESCE(NEW.qtd_marrucos, 0);

    -- Monta snapshot estruturado em JSONB
    v_payload := jsonb_build_object(
        'relatorio_id', NEW.id,
        'numero_gta', NEW.numero_gta,
        'numero_nota_fiscal', NEW.numero_nota_fiscal,
        'empresa_id', NEW.empresa_id,
        'status', NEW.status,
        'motorista_id', NEW.motorista_id,
        'cavalo_id', NEW.cavalo_id,
        'carreta_id', NEW.carreta_id,
        'fazenda_id', NEW.fazenda_id,
        'unidade_frigorifica_id', NEW.unidade_frigorifica_id,
        'total_embarcados', v_total_embarcados,
        'qtd_machos', COALESCE(NEW.qtd_machos, 0),
        'qtd_femeas', COALESCE(NEW.qtd_femeas, 0),
        'qtd_marrucos', COALESCE(NEW.qtd_marrucos, 0),
        'qtd_em_pe', COALESCE(NEW.qtd_em_pe, 0),
        'qtd_deitado', COALESCE(NEW.qtd_deitado, 0),
        'qtd_morto', COALESCE(NEW.qtd_morto, 0),
        'qtd_emergencia', COALESCE(NEW.qtd_emergencia, 0),
        'data_embarque', NEW.data_embarque,
        'horario_embarque', NEW.horario_embarque,
        'data_chegada_unidade', NEW.data_chegada_unidade,
        'horario_chegada_unidade', NEW.horario_chegada_unidade,
        'enviado_em', NEW.enviado_em,
        'finalizado_em', NEW.finalizado_em,
        'atualizado_em', CURRENT_TIMESTAMP
    );

    -- Insere evento dentro da mesma transacao ACID
    INSERT INTO sc_operacao.tb_outbox_evento (
        empresa_id,
        tipo_evento,
        agregado_tipo,
        agregado_id,
        payload,
        criado_em,
        processado_em,
        erro_tentativas
    ) VALUES (
        v_empresa_id,
        v_tipo_evento,
        'relatorio_viagem',
        v_agregado_id,
        v_payload,
        CURRENT_TIMESTAMP,
        NULL,
        0
    );

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- 5. Trigger de Disparo Automatico do Outbox
DROP TRIGGER IF EXISTS tg_outbox_relatorio_viagem ON sc_operacao.tb_relatorio_viagem;
CREATE TRIGGER tg_outbox_relatorio_viagem
    AFTER INSERT OR UPDATE OF status ON sc_operacao.tb_relatorio_viagem
    FOR EACH ROW EXECUTE FUNCTION sc_operacao.fn_gerar_outbox_relatorio();

-- 6. Atualizacao do Gatilho de Alocacao de Frota: tg_validar_alocacao_viagem
-- Dispara exclusivamente quando as colunas de alocacao forem modificadas
-- evitando travar a aprovacao de relatorios onde o status eh alterado
CREATE OR REPLACE FUNCTION sc_operacao.fn_validar_alocacao_viagem()
RETURNS TRIGGER AS $$
BEGIN
    -- Se motorista estiver ocupado em outra viagem aberta (diferente da atual)
    IF NEW.motorista_id IS NOT NULL THEN
        IF EXISTS (
            SELECT 1 FROM sc_operacao.tb_relatorio_viagem
            WHERE motorista_id = NEW.motorista_id
              AND id <> COALESCE(NEW.id, -1)
              AND status IN ('rascunho', 'pendente', 'em_andamento')
        ) THEN
            RAISE EXCEPTION 'Motorista % ja possui uma viagem em andamento.', NEW.motorista_id;
        END IF;
    END IF;

    -- Se cavalo estiver ocupado em outra viagem aberta
    IF NEW.cavalo_id IS NOT NULL THEN
        IF EXISTS (
            SELECT 1 FROM sc_operacao.tb_relatorio_viagem
            WHERE cavalo_id = NEW.cavalo_id
              AND id <> COALESCE(NEW.id, -1)
              AND status IN ('rascunho', 'pendente', 'em_andamento')
        ) THEN
            RAISE EXCEPTION 'Cavalo % ja esta alocado em uma viagem em andamento.', NEW.cavalo_id;
        END IF;
    END IF;

    -- Se carreta estiver ocupada em outra viagem aberta
    IF NEW.carreta_id IS NOT NULL THEN
        IF EXISTS (
            SELECT 1 FROM sc_operacao.tb_relatorio_viagem
            WHERE carreta_id = NEW.carreta_id
              AND id <> COALESCE(NEW.id, -1)
              AND status IN ('rascunho', 'pendente', 'em_andamento')
        ) THEN
            RAISE EXCEPTION 'Carreta % ja esta alocada em uma viagem em andamento.', NEW.carreta_id;
        END IF;
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tg_validar_alocacao_viagem ON sc_operacao.tb_relatorio_viagem;
CREATE TRIGGER tg_validar_alocacao_viagem
    BEFORE INSERT OR UPDATE OF motorista_id, cavalo_id, carreta_id ON sc_operacao.tb_relatorio_viagem
    FOR EACH ROW EXECUTE FUNCTION sc_operacao.fn_validar_alocacao_viagem();

-- 7. Ajustes em Stored Procedures para compatibilidade com drivers Python e Transacoes Spring
-- Remocao de comandos COMMIT explicitos para evitar erro 2D000 (invalid transaction termination)
CREATE OR REPLACE PROCEDURE sc_operacao.sp_aprovar_relatorio(
    p_relatorio_id INT,
    p_analista_id INT,
    p_parecer TEXT DEFAULT NULL
)
LANGUAGE plpgsql
AS $$
DECLARE
    v_status_atual VARCHAR(20);
BEGIN
    SELECT status INTO v_status_atual
    FROM sc_operacao.tb_relatorio_viagem
    WHERE id = p_relatorio_id
    FOR UPDATE;

    IF NOT FOUND THEN
        RAISE EXCEPTION 'Relatorio de viagem com id % nao encontrado.', p_relatorio_id;
    END IF;

    IF v_status_atual = 'aprovado' THEN
        RAISE NOTICE 'Relatorio de viagem com id % ja se encontra aprovado.', p_relatorio_id;
        RETURN;
    END IF;

    -- Atualiza status da viagem para aprovado
    UPDATE sc_operacao.tb_relatorio_viagem
    SET status = 'aprovado',
        finalizado_em = COALESCE(finalizado_em, CURRENT_TIMESTAMP),
        atualizado_em = CURRENT_TIMESTAMP
    WHERE id = p_relatorio_id;

    -- Registra na tabela de auditoria/analise se ela existir
    IF to_regclass('sc_operacao.tb_auditoria_analise') IS NOT NULL THEN
        INSERT INTO sc_operacao.tb_auditoria_analise (
            relatorio_id,
            analista_id,
            parecer,
            aprovado,
            data_analise
        ) VALUES (
            p_relatorio_id,
            p_analista_id,
            p_parecer,
            TRUE,
            CURRENT_TIMESTAMP
        );
    END IF;

    -- Sem COMMIT explicito: o gerenciador de conexao (Spring / Python psycopg) controla a transacao.
END;
$$;

CREATE OR REPLACE PROCEDURE sc_operacao.sp_registrar_anomalia(
    p_relatorio_id INT,
    p_tipo_etapa VARCHAR(20),
    p_codigo_anomalia VARCHAR(50),
    p_descricao_outros VARCHAR(150) DEFAULT NULL,
    p_quantidade INT DEFAULT 1
)
LANGUAGE plpgsql
AS $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM sc_operacao.tb_relatorio_viagem WHERE id = p_relatorio_id) THEN
        RAISE EXCEPTION 'Relatorio de viagem com id % nao encontrado.', p_relatorio_id;
    END IF;

    IF LOWER(p_tipo_etapa) IN ('embarque', 'origem') THEN
        INSERT INTO sc_operacao.tb_anomalia_embarque (
            relatorio_id,
            anomalia,
            descricao_outros,
            quantidade_animais
        ) VALUES (
            p_relatorio_id,
            p_codigo_anomalia,
            p_descricao_outros,
            COALESCE(p_quantidade, 1)
        );
    ELSIF LOWER(p_tipo_etapa) IN ('desembarque', 'destino') THEN
        INSERT INTO sc_operacao.tb_anomalia_desembarque (
            relatorio_id,
            anomalia,
            descricao_outros,
            quantidade_animais
        ) VALUES (
            p_relatorio_id,
            p_codigo_anomalia,
            p_descricao_outros,
            COALESCE(p_quantidade, 1)
        );
    ELSE
        RAISE EXCEPTION 'Tipo de etapa invalido: %. Esperado: embarque ou desembarque.', p_tipo_etapa;
    END IF;

    -- Sem COMMIT explicito
END;
$$;

-- 8. Governanca e Metadados: Catalogo de Dados (sc_auditoria.tb_catalogo_dados)
DO $$
BEGIN
    IF to_regclass('sc_auditoria.tb_catalogo_dados') IS NOT NULL THEN
        -- Cria tabela auxiliar se o formato esperado existir ou insere os 10 atributos de tb_outbox_evento
        INSERT INTO sc_auditoria.tb_catalogo_dados (
            tabela,
            coluna,
            tipo_dado,
            descricao,
            nivel_sensibilidade,
            nivel_acesso_leitura,
            nivel_acesso_escrita
        )
        SELECT * FROM (VALUES
            ('tb_outbox_evento', 'id', 'UUID', 'Identificador unico do evento de outbox (PK)', 'baixo', 'auditoria', 'administrativo'),
            ('tb_outbox_evento', 'empresa_id', 'UUID', 'Chave de isolamento multi-tenant da empresa', 'medio', 'auditoria', 'administrativo'),
            ('tb_outbox_evento', 'tipo_evento', 'VARCHAR(100)', 'Nome e rota do evento de negocio gerado', 'baixo', 'auditoria', 'administrativo'),
            ('tb_outbox_evento', 'agregado_tipo', 'VARCHAR(50)', 'Tipo do agregado no modelo DDD (ex: relatorio_viagem)', 'baixo', 'auditoria', 'administrativo'),
            ('tb_outbox_evento', 'agregado_id', 'UUID', 'Identificador unico do agregado de dominio', 'medio', 'auditoria', 'administrativo'),
            ('tb_outbox_evento', 'payload', 'JSONB', 'Snapshot imutavel estruturado com os dados da viagem', 'medio', 'auditoria', 'administrativo'),
            ('tb_outbox_evento', 'criado_em', 'TIMESTAMPTZ', 'Data e hora exata em que o evento foi gravado', 'baixo', 'auditoria', 'administrativo'),
            ('tb_outbox_evento', 'processado_em', 'TIMESTAMPTZ', 'Data e hora em que o evento foi publicado no Redis Relay', 'baixo', 'auditoria', 'administrativo'),
            ('tb_outbox_evento', 'erro_tentativas', 'INTEGER', 'Contador de retentativas de envio para o Redis Stream', 'baixo', 'auditoria', 'administrativo'),
            ('tb_outbox_evento', 'ultimo_erro', 'TEXT', 'Mensagem de erro ou stack trace da ultima falha de envio', 'baixo', 'auditoria', 'administrativo')
        ) AS v(tabela, coluna, tipo_dado, descricao, nivel_sensibilidade, nivel_acesso_leitura, nivel_acesso_escrita)
        WHERE NOT EXISTS (
            SELECT 1 FROM sc_auditoria.tb_catalogo_dados c
            WHERE c.tabela = v.tabela AND c.coluna = v.coluna
        );
    END IF;
EXCEPTION WHEN OTHERS THEN
    -- Se colunas forem diferentes na estrutura existente de tb_catalogo_dados, ignora sem travar a migracao
    RAISE NOTICE 'Nota: sc_auditoria.tb_catalogo_dados presente com estrutura diferenciada. Registro ignorado.';
END $$;
