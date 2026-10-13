-- As etapas do diário são salvas antes de todos os dados da viagem existirem.
-- A completude continua obrigatória na transição para pendente/concluído, no serviço.
ALTER TABLE sc_operacao.tb_relatorio_viagem
    ALTER COLUMN fazenda_id DROP NOT NULL,
    ALTER COLUMN manobrista_id DROP NOT NULL,
    ALTER COLUMN curraleiro_id DROP NOT NULL,
    ALTER COLUMN cavalo_id DROP NOT NULL,
    ALTER COLUMN carreta_id DROP NOT NULL,
    ALTER COLUMN numero_gta DROP NOT NULL,
    ALTER COLUMN numero_nota_fiscal DROP NOT NULL,
    ALTER COLUMN data_embarque DROP NOT NULL,
    ALTER COLUMN horario_embarque DROP NOT NULL,
    ALTER COLUMN horario_saida_propriedade DROP NOT NULL,
    ALTER COLUMN km_saida_embarcadouro DROP NOT NULL,
    ALTER COLUMN data_chegada_unidade DROP NOT NULL,
    ALTER COLUMN horario_chegada_unidade DROP NOT NULL,
    ALTER COLUMN horario_desembarque DROP NOT NULL,
    ALTER COLUMN km_chegada_desembarcadouro DROP NOT NULL,
    ALTER COLUMN numero_curral DROP NOT NULL,
    ALTER COLUMN sirene_re_funcionou DROP NOT NULL,
    ALTER COLUMN qtd_machos DROP NOT NULL,
    ALTER COLUMN qtd_femeas DROP NOT NULL,
    ALTER COLUMN qtd_marrucos DROP NOT NULL,
    ALTER COLUMN qtd_em_pe DROP NOT NULL,
    ALTER COLUMN qtd_deitado DROP NOT NULL,
    ALTER COLUMN qtd_morto DROP NOT NULL,
    ALTER COLUMN qtd_emergencia DROP NOT NULL,
    ALTER COLUMN url_assinatura_pecuarista DROP NOT NULL,
    ALTER COLUMN url_assinatura_motorista DROP NOT NULL,
    ALTER COLUMN url_assinatura_manobrista DROP NOT NULL,
    ALTER COLUMN url_assinatura_curraleiro DROP NOT NULL;

ALTER TABLE sc_operacao.tb_relatorio_viagem
    ADD COLUMN IF NOT EXISTS nome_origem VARCHAR(150),
    ADD COLUMN IF NOT EXISTS nome_destino VARCHAR(150);

-- Um motivo livre digitado no app não pode ser descartado ao normalizar para "outro".
ALTER TABLE sc_operacao.tb_parada_imprevista
    ADD COLUMN IF NOT EXISTS descricao_motivo VARCHAR(255);
