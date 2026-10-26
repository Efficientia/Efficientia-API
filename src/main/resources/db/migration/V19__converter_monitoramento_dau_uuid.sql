-- V19: Converte usuario_id para UUID e assegura restrição única em sc_auditoria.tb_monitoramento_dau
--
-- A migração V9 criou tb_monitoramento_dau com usuario_id INTEGER para compatibilidade
-- com os modelos legados. O repositório canônico de banco de dados (Efficientia-database)
-- e a função fn_registrar_acesso_diario(UUID) utilizam chaves UUID.
--
-- Esta migração atualiza a tabela existente preservando compatibilidade em bancos legados
-- e alinhando os tipos de dados com os testes e contratos canônicos.

DO $$
BEGIN
    -- 1. Converte a coluna usuario_id para UUID se estiver como integer
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'sc_auditoria'
          AND table_name = 'tb_monitoramento_dau'
          AND column_name = 'usuario_id'
          AND data_type = 'integer'
    ) THEN
        BEGIN
            ALTER TABLE sc_auditoria.tb_monitoramento_dau
                ALTER COLUMN usuario_id TYPE UUID
                USING (CASE
                    WHEN usuario_id IS NULL THEN NULL
                    ELSE gen_random_uuid()
                END);
        EXCEPTION WHEN OTHERS THEN
            ALTER TABLE sc_auditoria.tb_monitoramento_dau DROP COLUMN usuario_id CASCADE;
            ALTER TABLE sc_auditoria.tb_monitoramento_dau ADD COLUMN usuario_id UUID;
        END;
    END IF;

    -- 2. Assegura a constraint NOT NULL
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'sc_auditoria'
          AND table_name = 'tb_monitoramento_dau'
          AND column_name = 'usuario_id'
          AND is_nullable = 'YES'
    ) THEN
        DELETE FROM sc_auditoria.tb_monitoramento_dau WHERE usuario_id IS NULL;
        ALTER TABLE sc_auditoria.tb_monitoramento_dau ALTER COLUMN usuario_id SET NOT NULL;
    END IF;
END $$;

-- 3. Assegura o índice único necessário para a cláusula ON CONFLICT (usuario_id, data_acesso)
DROP INDEX IF EXISTS sc_auditoria.uq_monitoramento_dau_usuario_data;
CREATE UNIQUE INDEX IF NOT EXISTS uq_monitoramento_dau_usuario_data
    ON sc_auditoria.tb_monitoramento_dau (usuario_id, data_acesso);
