-- Adequa as tabelas persistidas aos schemas e nomes definidos no modelo relacional.
-- A V9 pode ter criado views com estes nomes; removemos só essas views antes
-- de migrar as tabelas. Tabelas físicas existentes nos schemas de destino são
-- preservadas e não são substituídas.
DO $$
DECLARE
    item RECORD;
    relacao_origem OID;
    tipo_relacao_origem "char";
    relacao_destino OID;
BEGIN
    FOR item IN
        SELECT * FROM (VALUES
            ('usuario', 'sc_corporativo', 'tb_usuario'),
            ('endereco', 'sc_corporativo', 'tb_endereco'),
            ('fazenda', 'sc_corporativo', 'tb_fazenda'),
            ('veiculo_carreta', 'sc_frota', 'tb_veiculo_carreta'),
            ('veiculo_cavalo', 'sc_frota', 'tb_veiculo_cavalo'),
            ('relatorio_viagem', 'sc_operacao', 'tb_relatorio_viagem'),
            ('parada_imprevista', 'sc_operacao', 'tb_parada_imprevista'),
            ('anomalia_embarque', 'sc_operacao', 'tb_anomalia_embarque'),
            ('anomalia_desembarque', 'sc_operacao', 'tb_anomalia_desembarque')
        ) AS m(tabela_origem, schema_destino, tabela_destino)
    LOOP
        relacao_destino := to_regclass(format('%I.%I', item.schema_destino, item.tabela_destino));

        IF relacao_destino IS NOT NULL AND EXISTS (
            SELECT 1 FROM pg_class WHERE oid = relacao_destino AND relkind = 'v'
        ) THEN
            EXECUTE format('DROP VIEW %I.%I', item.schema_destino, item.tabela_destino);
        END IF;
    END LOOP;

    FOR item IN
        SELECT * FROM (VALUES
            ('usuario', 'sc_corporativo', 'tb_usuario'),
            ('endereco', 'sc_corporativo', 'tb_endereco'),
            ('fazenda', 'sc_corporativo', 'tb_fazenda'),
            ('veiculo_carreta', 'sc_frota', 'tb_veiculo_carreta'),
            ('veiculo_cavalo', 'sc_frota', 'tb_veiculo_cavalo'),
            ('relatorio_viagem', 'sc_operacao', 'tb_relatorio_viagem'),
            ('parada_imprevista', 'sc_operacao', 'tb_parada_imprevista'),
            ('anomalia_embarque', 'sc_operacao', 'tb_anomalia_embarque'),
            ('anomalia_desembarque', 'sc_operacao', 'tb_anomalia_desembarque')
        ) AS m(tabela_origem, schema_destino, tabela_destino)
    LOOP
        relacao_origem := to_regclass(format('%I.%I', 'public', item.tabela_origem));

        IF relacao_origem IS NULL THEN
            CONTINUE;
        END IF;

        SELECT relkind INTO tipo_relacao_origem FROM pg_class WHERE oid = relacao_origem;
        IF tipo_relacao_origem NOT IN ('r', 'p') THEN
            RAISE NOTICE 'Preservando public.%: a relação não é uma tabela física', item.tabela_origem;
            CONTINUE;
        END IF;

        -- Se o schema já contém uma tabela física com o nome antigo ou final,
        -- mantenha ambas as relações em segurança em vez de sobrescrever dados.
        IF to_regclass(format('%I.%I', item.schema_destino, item.tabela_origem)) IS NOT NULL
           OR to_regclass(format('%I.%I', item.schema_destino, item.tabela_destino)) IS NOT NULL THEN
            RAISE NOTICE 'Preservando relações existentes para %.%; public.% não será movida',
                item.schema_destino, item.tabela_destino, item.tabela_origem;
            CONTINUE;
        END IF;

        EXECUTE format('ALTER TABLE %I.%I SET SCHEMA %I', 'public', item.tabela_origem, item.schema_destino);
        EXECUTE format('ALTER TABLE %I.%I RENAME TO %I', item.schema_destino, item.tabela_origem, item.tabela_destino);
    END LOOP;
END $$;
