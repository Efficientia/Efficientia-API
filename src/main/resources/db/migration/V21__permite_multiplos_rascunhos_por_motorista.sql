-- Um rascunho e um relatorio enviado nao reservam motorista nem veiculo.
-- A regra de alocacao se aplica somente a uma viagem realmente em andamento.
CREATE OR REPLACE FUNCTION sc_operacao.fn_validar_alocacao_viagem()
RETURNS TRIGGER AS $$
BEGIN
    IF NEW.status IS DISTINCT FROM 'em_andamento' THEN
        RETURN NEW;
    END IF;

    IF NEW.motorista_id IS NOT NULL AND EXISTS (
        SELECT 1 FROM sc_operacao.tb_relatorio_viagem
         WHERE motorista_id = NEW.motorista_id
           AND id <> COALESCE(NEW.id, -1)
           AND status = 'em_andamento'
    ) THEN
        RAISE EXCEPTION 'Motorista % ja possui uma viagem em andamento.', NEW.motorista_id
            USING ERRCODE = '23505';
    END IF;

    IF NEW.cavalo_id IS NOT NULL AND EXISTS (
        SELECT 1 FROM sc_operacao.tb_relatorio_viagem
         WHERE cavalo_id = NEW.cavalo_id
           AND id <> COALESCE(NEW.id, -1)
           AND status = 'em_andamento'
    ) THEN
        RAISE EXCEPTION 'Cavalo % ja esta alocado em uma viagem em andamento.', NEW.cavalo_id
            USING ERRCODE = '23505';
    END IF;

    IF NEW.carreta_id IS NOT NULL AND EXISTS (
        SELECT 1 FROM sc_operacao.tb_relatorio_viagem
         WHERE carreta_id = NEW.carreta_id
           AND id <> COALESCE(NEW.id, -1)
           AND status = 'em_andamento'
    ) THEN
        RAISE EXCEPTION 'Carreta % ja esta alocada em uma viagem em andamento.', NEW.carreta_id
            USING ERRCODE = '23505';
    END IF;

    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS tg_validar_alocacao_viagem ON sc_operacao.tb_relatorio_viagem;
CREATE TRIGGER tg_validar_alocacao_viagem
    BEFORE INSERT OR UPDATE OF motorista_id, cavalo_id, carreta_id, status
    ON sc_operacao.tb_relatorio_viagem
    FOR EACH ROW EXECUTE FUNCTION sc_operacao.fn_validar_alocacao_viagem();
