-- A V11 preserva tabelas físicas preexistentes nos schemas de destino. Em
-- alguns bancos essas tabelas usam UUID, enquanto o modelo relacional legado
-- e as entidades JPA usam IDs inteiros. Preserve as tabelas incompatíveis em
-- um schema separado e promova as tabelas legadas de public para os schemas
-- esperados pela aplicação.
CREATE SCHEMA IF NOT EXISTS sc_legacy_render;

DO $$
DECLARE
    item RECORD;
    destino OID;
    origem OID;
    tipo_destino "char";
    tipo_origem "char";
    tipo_id_destino TEXT;
    tipo_id_origem TEXT;
    nome_arquivo TEXT;
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
        origem := to_regclass(format('%I.%I', 'public', item.tabela_origem));
        IF origem IS NULL THEN
            CONTINUE;
        END IF;

        SELECT relkind INTO tipo_origem FROM pg_class WHERE oid = origem;
        IF tipo_origem NOT IN ('r', 'p') THEN
            CONTINUE;
        END IF;

        SELECT data_type INTO tipo_id_origem
          FROM information_schema.columns
         WHERE table_schema = 'public'
           AND table_name = item.tabela_origem
           AND column_name = 'id';
        IF tipo_id_origem IS DISTINCT FROM 'integer' THEN
            RAISE EXCEPTION 'public.% possui id do tipo %, esperado integer; migração abortada sem alterar tabelas',
                item.tabela_origem, tipo_id_origem;
        END IF;

        -- Remova do caminho da aplicação somente relações físicas incompatíveis.
        -- Seu OID, dados, índices e dependências continuam preservados no schema legado.
        FOREACH nome_arquivo IN ARRAY ARRAY[item.tabela_destino, item.tabela_origem]
        LOOP
            destino := to_regclass(format('%I.%I', item.schema_destino, nome_arquivo));
            IF destino IS NULL THEN
                CONTINUE;
            END IF;

            SELECT relkind INTO tipo_destino FROM pg_class WHERE oid = destino;
            IF tipo_destino NOT IN ('r', 'p') THEN
                CONTINUE;
            END IF;

            SELECT data_type INTO tipo_id_destino
              FROM information_schema.columns
             WHERE table_schema = item.schema_destino
               AND table_name = nome_arquivo
               AND column_name = 'id';
            IF tipo_id_destino IS DISTINCT FROM 'integer' THEN
                IF to_regclass(format('%I.%I', 'sc_legacy_render', nome_arquivo)) IS NOT NULL THEN
                    RAISE EXCEPTION 'sc_legacy_render.% já existe; migração abortada para preservar dados', nome_arquivo;
                END IF;
                EXECUTE format('ALTER TABLE %I.%I SET SCHEMA sc_legacy_render', item.schema_destino, nome_arquivo);
            END IF;
        END LOOP;

        destino := to_regclass(format('%I.%I', item.schema_destino, item.tabela_destino));
        IF destino IS NULL THEN
            EXECUTE format('ALTER TABLE public.%I SET SCHEMA %I', item.tabela_origem, item.schema_destino);
            EXECUTE format('ALTER TABLE %I.%I RENAME TO %I', item.schema_destino, item.tabela_origem, item.tabela_destino);
        END IF;
    END LOOP;
END $$;
