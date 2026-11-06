-- codigo_interno identifies a company and is shared by its employees.
-- CPF and email remain globally unique; only single-column UNIQUE constraints
-- and indexes on codigo_interno are removed.
DO $$
DECLARE
    tabela_usuario OID := to_regclass('sc_corporativo.tb_usuario');
    coluna_codigo SMALLINT;
    objeto RECORD;
BEGIN
    IF tabela_usuario IS NULL OR NOT EXISTS (
        SELECT 1 FROM pg_class
        WHERE oid = tabela_usuario AND relkind IN ('r', 'p')
    ) THEN
        RAISE EXCEPTION 'Tabela sc_corporativo.tb_usuario não encontrada como tabela física';
    END IF;

    SELECT attnum
      INTO coluna_codigo
      FROM pg_attribute
     WHERE attrelid = tabela_usuario
       AND attname = 'codigo_interno'
       AND NOT attisdropped;

    IF coluna_codigo IS NULL THEN
        RAISE EXCEPTION 'Coluna sc_corporativo.tb_usuario.codigo_interno não encontrada';
    END IF;

    FOR objeto IN
        SELECT conname
          FROM pg_constraint
         WHERE conrelid = tabela_usuario
           AND contype = 'u'
           AND conkey = ARRAY[coluna_codigo]::SMALLINT[]
    LOOP
        EXECUTE format(
            'ALTER TABLE sc_corporativo.tb_usuario DROP CONSTRAINT %I',
            objeto.conname
        );
    END LOOP;

    FOR objeto IN
        SELECT ns.nspname AS schema_nome, idx.relname AS indice_nome
          FROM pg_index ind
          JOIN pg_class idx ON idx.oid = ind.indexrelid
          JOIN pg_namespace ns ON ns.oid = idx.relnamespace
         WHERE ind.indrelid = tabela_usuario
           AND ind.indisunique
           AND NOT ind.indisprimary
           AND ind.indnkeyatts = 1
           AND ind.indkey[0] = coluna_codigo
           AND NOT EXISTS (
               SELECT 1
                 FROM pg_constraint c
                WHERE c.conindid = ind.indexrelid
           )
    LOOP
        EXECUTE format('DROP INDEX %I.%I', objeto.schema_nome, objeto.indice_nome);
    END LOOP;

    CREATE INDEX IF NOT EXISTS idx_tb_usuario_empresa_id
        ON sc_corporativo.tb_usuario (empresa_id);
END $$;
