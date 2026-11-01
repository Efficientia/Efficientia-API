-- Corrige o vínculo do motorista sintético de homologação sem recriar seus dados.
-- Os diários existentes são associados à mesma empresa a partir do motorista_id.
DO $$
DECLARE
    empresa_homologacao_id INTEGER;
    motorista_homologacao_id INTEGER;
    usuarios_atualizados INTEGER := 0;
    relatorios_atualizados INTEGER := 0;
BEGIN
    SELECT id
      INTO empresa_homologacao_id
      FROM public.empresa
     WHERE cnpj = '12345678000195';

    IF empresa_homologacao_id IS NULL THEN
        RAISE NOTICE 'Empresa de homologação com CNPJ 12345678000195 não encontrada; vínculo não alterado.';
        RETURN;
    END IF;

    SELECT id
      INTO motorista_homologacao_id
      FROM sc_corporativo.tb_usuario
     WHERE lower(email) = 'motorista.mobile.teste@efficientia.app'
       AND codigo_interno = 'EMP-MOBILE-001';

    IF motorista_homologacao_id IS NULL THEN
        RAISE NOTICE 'Motorista EMP-MOBILE-001 não encontrado; vínculo não alterado.';
        RETURN;
    END IF;

    UPDATE sc_corporativo.tb_usuario
       SET empresa_id = empresa_homologacao_id
     WHERE id = motorista_homologacao_id
       AND empresa_id IS DISTINCT FROM empresa_homologacao_id;
    GET DIAGNOSTICS usuarios_atualizados = ROW_COUNT;

    UPDATE sc_operacao.tb_relatorio_viagem
       SET empresa_id = empresa_homologacao_id
     WHERE motorista_id = motorista_homologacao_id
       AND empresa_id IS DISTINCT FROM empresa_homologacao_id;
    GET DIAGNOSTICS relatorios_atualizados = ROW_COUNT;

    RAISE NOTICE 'Homologação vinculada: % usuário(s), % relatório(s) preservados e associados à empresa %.',
        usuarios_atualizados, relatorios_atualizados, empresa_homologacao_id;
END $$;
