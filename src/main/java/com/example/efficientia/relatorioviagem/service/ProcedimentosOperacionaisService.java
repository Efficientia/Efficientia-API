package com.example.efficientia.relatorioviagem.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Serviço responsável pela execução segura e transacional das Stored Procedures
 * operacionais do banco PostgreSQL (sc_operacao.sp_aprovar_relatorio e sc_operacao.sp_registrar_anomalia).
 *
 * Como as procedures foram refatoradas para remoção de comandos COMMIT internos,
 * as transações são integralmente coordenadas pelo Spring via @Transactional.
 * Falhas em bancos em memória ou onde as procedures não existam (ex: H2 nos testes)
 * são tratadas com degradação graciosa.
 */
@Service
public class ProcedimentosOperacionaisService {

    private static final Logger log = LoggerFactory.getLogger(ProcedimentosOperacionaisService.class);

    private final JdbcTemplate jdbcTemplate;

    public ProcedimentosOperacionaisService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * Executa a procedure sc_operacao.sp_aprovar_relatorio.
     *
     * @param relatorioId ID do relatório de viagem a ser aprovado.
     * @param analistaId  ID do analista responsável pela aprovação.
     * @param parecer     Parecer ou observação opcional do analista.
     * @return true se a procedure foi executada com sucesso, false caso tenha ocorrido fallback/ignorado.
     */
    @Transactional
    public boolean executarAprovacaoRelatorio(Integer relatorioId, Integer analistaId, String parecer) {
        if (relatorioId == null || analistaId == null) {
            throw new IllegalArgumentException("relatorioId e analistaId são obrigatórios para aprovar relatório via procedure.");
        }

        try {
            log.info("Invocando procedure sc_operacao.sp_aprovar_relatorio para relatorioId: {}, analistaId: {}",
                    relatorioId, analistaId);
            jdbcTemplate.update("CALL sc_operacao.sp_aprovar_relatorio(?, ?, ?)", relatorioId, analistaId, parecer);
            log.info("Procedure sc_operacao.sp_aprovar_relatorio executada com sucesso para relatorioId: {}", relatorioId);
            return true;
        } catch (BadSqlGrammarException ex) {
            log.warn("Procedure sc_operacao.sp_aprovar_relatorio não encontrada ou incompatível no schema atual (ambiente de testes/H2?). Detalhes: {}",
                    ex.getMessage());
            return false;
        } catch (DataAccessException ex) {
            log.error("Erro de acesso a dados ao executar sc_operacao.sp_aprovar_relatorio: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Executa a procedure sc_operacao.sp_registrar_anomalia.
     *
     * @param relatorioId       ID do relatório de viagem vinculado à anomalia.
     * @param tipoEtapa         Etapa da anomalia (ex: 'EMBARQUE', 'DESEMBARQUE').
     * @param codigoAnomalia    Código da anomalia de acordo com o domínio.
     * @param descricaoOutros   Descrição textual adicional caso o código seja 'OUTROS' ou detalhamento.
     * @param quantidadeAnimais Quantidade de animais envolvidos na anomalia (mínimo 1).
     * @return true se a procedure foi executada com sucesso, false caso tenha ocorrido fallback/ignorado.
     */
    @Transactional
    public boolean executarRegistroAnomalia(Integer relatorioId, String tipoEtapa, String codigoAnomalia,
                                           String descricaoOutros, Integer quantidadeAnimais) {
        if (relatorioId == null || tipoEtapa == null || codigoAnomalia == null) {
            throw new IllegalArgumentException("relatorioId, tipoEtapa e codigoAnomalia são obrigatórios para registrar anomalia via procedure.");
        }

        int quantidade = (quantidadeAnimais != null && quantidadeAnimais > 0) ? quantidadeAnimais : 1;

        try {
            log.info("Invocando procedure sc_operacao.sp_registrar_anomalia para relatorioId: {}, etapa: {}, codigo: {}, qtd: {}",
                    relatorioId, tipoEtapa, codigoAnomalia, quantidade);
            jdbcTemplate.update("CALL sc_operacao.sp_registrar_anomalia(?, ?, ?, ?, ?)",
                    relatorioId, tipoEtapa, codigoAnomalia, descricaoOutros, quantidade);
            log.info("Procedure sc_operacao.sp_registrar_anomalia executada com sucesso para relatorioId: {}", relatorioId);
            return true;
        } catch (BadSqlGrammarException ex) {
            log.warn("Procedure sc_operacao.sp_registrar_anomalia não encontrada ou incompatível no schema atual (ambiente de testes/H2?). Detalhes: {}",
                    ex.getMessage());
            return false;
        } catch (DataAccessException ex) {
            log.error("Erro de acesso a dados ao executar sc_operacao.sp_registrar_anomalia: {}", ex.getMessage(), ex);
            throw ex;
        }
    }
}
