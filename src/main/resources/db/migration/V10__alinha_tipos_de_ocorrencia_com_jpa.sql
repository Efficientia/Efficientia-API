-- Os valores continuam restritos aos mesmos códigos da enumeração original,
-- mas os campos passam a VARCHAR para corresponder às entidades JPA String.
-- V9 pode ter criado views de compatibilidade sobre estas tabelas. PostgreSQL
-- bloqueia a troca do tipo enquanto uma view depender da coluna, então guardamos
-- a definição, removemos apenas as views (nunca tabelas físicas) e as recriamos.
DO $$
DECLARE
    definicao_view_parada TEXT;
    definicao_view_anomalia_embarque TEXT;
    definicao_view_anomalia_desembarque TEXT;
BEGIN
    IF EXISTS (
        SELECT 1
        FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = 'sc_operacao'
          AND c.relname = 'tb_parada_imprevista'
          AND c.relkind = 'v'
    ) THEN
        definicao_view_parada := pg_get_viewdef('sc_operacao.tb_parada_imprevista'::regclass, TRUE);
        DROP VIEW sc_operacao.tb_parada_imprevista;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = 'sc_operacao'
          AND c.relname = 'tb_anomalia_embarque'
          AND c.relkind = 'v'
    ) THEN
        definicao_view_anomalia_embarque := pg_get_viewdef('sc_operacao.tb_anomalia_embarque'::regclass, TRUE);
        DROP VIEW sc_operacao.tb_anomalia_embarque;
    END IF;

    IF EXISTS (
        SELECT 1
        FROM pg_class c
        JOIN pg_namespace n ON n.oid = c.relnamespace
        WHERE n.nspname = 'sc_operacao'
          AND c.relname = 'tb_anomalia_desembarque'
          AND c.relkind = 'v'
    ) THEN
        definicao_view_anomalia_desembarque := pg_get_viewdef('sc_operacao.tb_anomalia_desembarque'::regclass, TRUE);
        DROP VIEW sc_operacao.tb_anomalia_desembarque;
    END IF;

    ALTER TABLE public.parada_imprevista
        ALTER COLUMN motivo TYPE VARCHAR(50) USING motivo::text;

    ALTER TABLE public.anomalia_embarque
        ALTER COLUMN anomalia TYPE VARCHAR(50) USING anomalia::text;

    ALTER TABLE public.anomalia_desembarque
        ALTER COLUMN anomalia TYPE VARCHAR(50) USING anomalia::text;

    IF definicao_view_parada IS NOT NULL THEN
        EXECUTE 'CREATE VIEW sc_operacao.tb_parada_imprevista AS ' || definicao_view_parada;
    END IF;

    IF definicao_view_anomalia_embarque IS NOT NULL THEN
        EXECUTE 'CREATE VIEW sc_operacao.tb_anomalia_embarque AS ' || definicao_view_anomalia_embarque;
    END IF;

    IF definicao_view_anomalia_desembarque IS NOT NULL THEN
        EXECUTE 'CREATE VIEW sc_operacao.tb_anomalia_desembarque AS ' || definicao_view_anomalia_desembarque;
    END IF;
END $$;

ALTER TABLE public.parada_imprevista
    ADD CONSTRAINT chk_parada_imprevista_motivo
        CHECK (motivo IN ('transbordo', 'acidente_pista', 'atoleiro', 'problema_mecanico', 'outro'));

ALTER TABLE public.anomalia_embarque
    ADD CONSTRAINT chk_anomalia_embarque_codigo
        CHECK (anomalia IN (
            'sangrando', 'excesso_magreza_debilitado', 'cutucoes_fortes', 'mancando',
            'sujo_pisoteio', 'tentativa_quebrar_cauda', 'gaiola_cheia', 'chute_paulada_ferrao',
            'arraste', 'outro'
        ));

ALTER TABLE public.anomalia_desembarque
    ADD CONSTRAINT chk_anomalia_desembarque_codigo
        CHECK (anomalia IN (
            'gaiola_buraco_estrado_solto', 'porteiras_nao_abrem', 'uso_abusivo_choque',
            'problema_mecanico_veiculo', 'outros_atos_abuso'
        ));
