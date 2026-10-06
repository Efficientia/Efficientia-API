-- Os valores continuam restritos aos mesmos códigos da enumeração original,
-- mas os campos passam a VARCHAR para corresponder às entidades JPA String.
ALTER TABLE public.parada_imprevista
    ALTER COLUMN motivo TYPE VARCHAR(50) USING motivo::text;

ALTER TABLE public.anomalia_embarque
    ALTER COLUMN anomalia TYPE VARCHAR(50) USING anomalia::text;

ALTER TABLE public.anomalia_desembarque
    ALTER COLUMN anomalia TYPE VARCHAR(50) USING anomalia::text;

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
