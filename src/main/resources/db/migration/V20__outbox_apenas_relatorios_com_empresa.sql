-- Relatorios de motoristas legados podem nao ter empresa vinculada.
-- A outbox canonica exige empresa_id valido; o UUID zero usado pela funcao
-- anterior viola fk_outbox_empresa e desfaz a criacao do rascunho.
-- Gere eventos apenas quando o relatorio possuir uma empresa real.
DROP TRIGGER IF EXISTS tg_outbox_relatorio_viagem ON sc_operacao.tb_relatorio_viagem;

CREATE TRIGGER tg_outbox_relatorio_viagem
    AFTER INSERT OR UPDATE OF status ON sc_operacao.tb_relatorio_viagem
    FOR EACH ROW
    WHEN (NEW.empresa_id IS NOT NULL)
    EXECUTE FUNCTION sc_operacao.fn_gerar_outbox_relatorio();
