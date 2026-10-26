-- V18: índices para as buscas paginadas de relatórios da API.
--
-- O esquema canônico em Efficientia-database usa UUID e enums. As migrações
-- V1–V17 da API preservam chaves INTEGER e status VARCHAR por compatibilidade
-- com seus consumidores; não convertemos tabelas/views existentes nesta
-- migração, pois essa conversão exige migração coordenada dos dados e da API.
--
-- Cria índices apenas quando o objeto de operação é uma tabela física e as
-- colunas existem. Isso mantém a migração válida em bancos que expõem aliases
-- de compatibilidade como views ou que ainda não instalaram o schema legado.

-- O repositório de database define fn_registrar_acesso_diario(UUID), mas não
-- cria a tabela referenciada pela função. Provisiona a chave usada no ON CONFLICT.
CREATE SCHEMA IF NOT EXISTS sc_auditoria;
CREATE TABLE IF NOT EXISTS sc_auditoria.tb_monitoramento_dau (
    usuario_id UUID NOT NULL,
    data_acesso DATE NOT NULL DEFAULT CURRENT_DATE
);
CREATE UNIQUE INDEX IF NOT EXISTS uq_monitoramento_dau_usuario_data
    ON sc_auditoria.tb_monitoramento_dau (usuario_id, data_acesso);

DO $$
DECLARE
    relatorio_kind "char";
    tem_motorista BOOLEAN;
    tem_status BOOLEAN;
BEGIN
    SELECT c.relkind
      INTO relatorio_kind
      FROM pg_class c
      JOIN pg_namespace n ON n.oid = c.relnamespace
     WHERE n.nspname = 'sc_operacao'
       AND c.relname = 'tb_relatorio_viagem';

    IF relatorio_kind IN ('r', 'p') THEN
        SELECT EXISTS (
            SELECT 1 FROM information_schema.columns
             WHERE table_schema = 'sc_operacao'
               AND table_name = 'tb_relatorio_viagem'
               AND column_name = 'motorista_id'
        ) INTO tem_motorista;

        SELECT EXISTS (
            SELECT 1 FROM information_schema.columns
             WHERE table_schema = 'sc_operacao'
               AND table_name = 'tb_relatorio_viagem'
               AND column_name = 'status'
        ) INTO tem_status;

        IF tem_motorista AND tem_status THEN
            EXECUTE 'CREATE INDEX IF NOT EXISTS idx_api_relatorio_motorista_status '
                 || 'ON sc_operacao.tb_relatorio_viagem (motorista_id, status)';
        END IF;

        IF tem_status THEN
            EXECUTE 'CREATE INDEX IF NOT EXISTS idx_api_relatorio_status '
                 || 'ON sc_operacao.tb_relatorio_viagem (status)';
        END IF;
    END IF;
END $$;
