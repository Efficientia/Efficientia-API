-- Resolve the legacy integer company ID to the canonical UUID referenced by
-- sc_operacao.tb_outbox_evento. Never fabricate a UUID from an integer ID:
-- that UUID does not satisfy the outbox foreign key and rolls back the report.
CREATE OR REPLACE FUNCTION sc_operacao.fn_gerar_outbox_relatorio()
RETURNS TRIGGER AS $$
DECLARE
    v_tipo_evento VARCHAR(100);
    v_empresa_id UUID;
    v_agregado_id UUID;
    v_total_embarcados INTEGER;
    v_payload JSONB;
    v_fk_schema TEXT;
    v_fk_table TEXT;
    v_fk_id_column TEXT;
    v_fk_cnpj_column TEXT;
BEGIN
    IF TG_OP = 'UPDATE' THEN
        IF NEW.status IS NOT DISTINCT FROM OLD.status THEN
            RETURN NEW;
        END IF;
        v_tipo_evento := 'relatorio.status_alterado';
    ELSE
        v_tipo_evento := 'relatorio.criado';
    END IF;

    IF NEW.empresa_id IS NULL THEN
        RAISE WARNING 'Relatório % sem empresa; evento de outbox não gerado.', NEW.id;
        RETURN NEW;
    END IF;

    -- Discover the real UUID key target from the outbox FK, so this remains
    -- aligned with the canonical database schema rather than assuming a fake
    -- integer-to-UUID conversion.
    SELECT ns.nspname, rel.relname, att.attname
      INTO v_fk_schema, v_fk_table, v_fk_id_column
      FROM pg_constraint fk
      JOIN pg_class outbox ON outbox.oid = fk.conrelid
      JOIN pg_class rel ON rel.oid = fk.confrelid
      JOIN pg_namespace ns ON ns.oid = rel.relnamespace
      JOIN LATERAL unnest(fk.confkey) WITH ORDINALITY key_col(attnum, ord) ON TRUE
      JOIN pg_attribute att ON att.attrelid = rel.oid AND att.attnum = key_col.attnum
     WHERE fk.conname = 'fk_outbox_empresa'
       AND outbox.oid = 'sc_operacao.tb_outbox_evento'::regclass
     LIMIT 1;

    IF v_fk_schema IS NULL OR to_regclass('public.empresa') IS NULL THEN
        RAISE WARNING 'Não foi possível resolver a empresa canônica do relatório %; evento de outbox não gerado.', NEW.id;
        RETURN NEW;
    END IF;

    SELECT column_name
      INTO v_fk_cnpj_column
      FROM information_schema.columns
     WHERE table_schema = v_fk_schema
       AND table_name = v_fk_table
       AND lower(column_name) IN ('cnpj', 'cnpj_empresa')
     ORDER BY CASE lower(column_name) WHEN 'cnpj' THEN 0 ELSE 1 END
     LIMIT 1;

    IF v_fk_cnpj_column IS NULL
       OR NOT EXISTS (
            SELECT 1 FROM information_schema.columns
             WHERE table_schema = 'public'
               AND table_name = 'empresa'
               AND column_name = 'cnpj'
       ) THEN
        RAISE WARNING 'CNPJ indisponível para resolver a empresa canônica do relatório %; evento de outbox não gerado.', NEW.id;
        RETURN NEW;
    END IF;

    EXECUTE format(
        'SELECT canon.%I '
        'FROM %I.%I canon '
        'JOIN public.empresa legado '
        '  ON regexp_replace(canon.%I::text, ''\D'', '''', ''g'') = '
        '     regexp_replace(legado.cnpj::text, ''\D'', '''', ''g'') '
        'WHERE legado.id = $1 '
        'LIMIT 1',
        v_fk_id_column, v_fk_schema, v_fk_table, v_fk_cnpj_column
    ) INTO v_empresa_id USING NEW.empresa_id;

    IF v_empresa_id IS NULL THEN
        RAISE WARNING 'Empresa legada % não possui correspondência canônica; relatório % será salvo sem evento de outbox.', NEW.empresa_id, NEW.id;
        RETURN NEW;
    END IF;

    BEGIN
        v_agregado_id := NEW.id::text::uuid;
    EXCEPTION WHEN OTHERS THEN
        v_agregado_id := ('00000000-0000-0000-0000-' || LPAD(TO_HEX(COALESCE(NEW.id, 0)), 12, '0'))::uuid;
    END;

    v_total_embarcados := COALESCE(NEW.qtd_machos, 0)
                        + COALESCE(NEW.qtd_femeas, 0)
                        + COALESCE(NEW.qtd_marrucos, 0);

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

    INSERT INTO sc_operacao.tb_outbox_evento (
        empresa_id, tipo_evento, agregado_tipo, agregado_id, payload,
        criado_em, processado_em, erro_tentativas
    ) VALUES (
        v_empresa_id, v_tipo_evento, 'relatorio_viagem', v_agregado_id,
        v_payload, CURRENT_TIMESTAMP, NULL, 0
    );

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

-- Keep the existing guard for reports that do not yet have a company link.
DROP TRIGGER IF EXISTS tg_outbox_relatorio_viagem ON sc_operacao.tb_relatorio_viagem;
CREATE TRIGGER tg_outbox_relatorio_viagem
    AFTER INSERT OR UPDATE OF status ON sc_operacao.tb_relatorio_viagem
    FOR EACH ROW
    WHEN (NEW.empresa_id IS NOT NULL)
    EXECUTE FUNCTION sc_operacao.fn_gerar_outbox_relatorio();
