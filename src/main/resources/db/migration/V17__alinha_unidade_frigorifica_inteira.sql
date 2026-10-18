-- A V11 moveu endereco e fazenda para sc_corporativo, mas deixou a unidade
-- frigorifica INTEGER em public. Em bancos legados, o nome de destino pode
-- estar ocupado por uma tabela UUID; preservamos essa tabela sem apagar dados.
CREATE SCHEMA IF NOT EXISTS sc_legacy_render;

DO $$
DECLARE
    origem OID := to_regclass('public.unidade_frigorifica');
    destino OID := to_regclass('sc_corporativo.tb_unidade_frigorifica');
    tipo_destino "char";
    tipo_id_origem TEXT;
    tipo_id_destino TEXT;
BEGIN
    IF origem IS NOT NULL THEN
        SELECT data_type INTO tipo_id_origem
          FROM information_schema.columns
         WHERE table_schema = 'public'
           AND table_name = 'unidade_frigorifica'
           AND column_name = 'id';
        IF tipo_id_origem IS DISTINCT FROM 'integer' THEN
            RAISE EXCEPTION 'public.unidade_frigorifica possui id %, esperado integer', tipo_id_origem;
        END IF;
    END IF;

    IF destino IS NOT NULL THEN
        SELECT relkind INTO tipo_destino FROM pg_class WHERE oid = destino;
        IF tipo_destino = 'v' THEN
            EXECUTE 'DROP VIEW sc_corporativo.tb_unidade_frigorifica';
            destino := NULL;
        ELSIF tipo_destino IN ('r', 'p') THEN
            SELECT data_type INTO tipo_id_destino
              FROM information_schema.columns
             WHERE table_schema = 'sc_corporativo'
               AND table_name = 'tb_unidade_frigorifica'
               AND column_name = 'id';
            IF tipo_id_destino IS DISTINCT FROM 'integer' THEN
                IF origem IS NULL THEN
                    RAISE EXCEPTION 'Unidade frigorifica UUID sem tabela INTEGER em public para promover';
                END IF;
                IF to_regclass('sc_legacy_render.tb_unidade_frigorifica') IS NOT NULL THEN
                    RAISE EXCEPTION 'sc_legacy_render.tb_unidade_frigorifica ja existe; preservando ambas as tabelas';
                END IF;
                EXECUTE 'ALTER TABLE sc_corporativo.tb_unidade_frigorifica SET SCHEMA sc_legacy_render';
                destino := NULL;
            END IF;
        ELSE
            RAISE EXCEPTION 'sc_corporativo.tb_unidade_frigorifica possui tipo de relacao nao suportado: %', tipo_destino;
        END IF;
    END IF;

    IF destino IS NULL THEN
        IF origem IS NULL THEN
            RAISE EXCEPTION 'Tabela INTEGER de unidade frigorifica nao encontrada';
        END IF;
        EXECUTE 'ALTER TABLE public.unidade_frigorifica SET SCHEMA sc_corporativo';
        EXECUTE 'ALTER TABLE sc_corporativo.unidade_frigorifica RENAME TO tb_unidade_frigorifica';
    END IF;
END $$;
