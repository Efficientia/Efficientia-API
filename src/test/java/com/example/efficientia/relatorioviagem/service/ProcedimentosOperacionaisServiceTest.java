package com.example.efficientia.relatorioviagem.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.jdbc.BadSqlGrammarException;
import org.springframework.jdbc.core.JdbcTemplate;

import java.sql.SQLException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProcedimentosOperacionaisServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    private ProcedimentosOperacionaisService service;

    @BeforeEach
    void setUp() {
        service = new ProcedimentosOperacionaisService(jdbcTemplate);
    }

    @Test
    @DisplayName("executarAprovacaoRelatorio com sucesso executa jdbcTemplate.update e retorna true")
    void deveExecutarAprovacaoRelatorioComSucesso() {
        when(jdbcTemplate.update("CALL sc_operacao.sp_aprovar_relatorio(?, ?, ?)", 10, 20, "Parecer favoravel"))
                .thenReturn(1);

        boolean sucesso = service.executarAprovacaoRelatorio(10, 20, "Parecer favoravel");

        assertThat(sucesso).isTrue();
        verify(jdbcTemplate).update("CALL sc_operacao.sp_aprovar_relatorio(?, ?, ?)", 10, 20, "Parecer favoravel");
    }

    @Test
    @DisplayName("executarAprovacaoRelatorio com relatorioId nulo lança IllegalArgumentException")
    void deveLancarExcecaoQuandoRelatorioIdNuloNaAprovacao() {
        assertThatThrownBy(() -> service.executarAprovacaoRelatorio(null, 20, "Parecer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("relatorioId e analistaId são obrigatórios");

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("executarAprovacaoRelatorio com analistaId nulo lança IllegalArgumentException")
    void deveLancarExcecaoQuandoAnalistaIdNuloNaAprovacao() {
        assertThatThrownBy(() -> service.executarAprovacaoRelatorio(10, null, "Parecer"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("relatorioId e analistaId são obrigatórios");

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("executarAprovacaoRelatorio quando ocorre BadSqlGrammarException retorna false (fallback H2)")
    void deveRetornarFalseEmCasoDeBadSqlGrammarExceptionNaAprovacao() {
        SQLException sqlException = new SQLException("Procedure not found", "42883");
        BadSqlGrammarException ex = new BadSqlGrammarException("CALL sp_aprovar_relatorio", "CALL ...", sqlException);
        when(jdbcTemplate.update(anyString(), any(), any(), any())).thenThrow(ex);

        boolean sucesso = service.executarAprovacaoRelatorio(10, 20, "Parecer");

        assertThat(sucesso).isFalse();
        verify(jdbcTemplate).update("CALL sc_operacao.sp_aprovar_relatorio(?, ?, ?)", 10, 20, "Parecer");
    }

    @Test
    @DisplayName("executarAprovacaoRelatorio quando ocorre DataAccessException genérico relança exceção")
    void deveRelancarDataAccessExceptionGenericaNaAprovacao() {
        DataAccessResourceFailureException ex = new DataAccessResourceFailureException("DB connection refused");
        when(jdbcTemplate.update(anyString(), any(), any(), any())).thenThrow(ex);

        assertThatThrownBy(() -> service.executarAprovacaoRelatorio(10, 20, "Parecer"))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("DB connection refused");

        verify(jdbcTemplate).update("CALL sc_operacao.sp_aprovar_relatorio(?, ?, ?)", 10, 20, "Parecer");
    }

    @Test
    @DisplayName("executarRegistroAnomalia com sucesso para diferentes etapas e quantidades")
    void deveExecutarRegistroAnomaliaComSucesso() {
        when(jdbcTemplate.update(eq("CALL sc_operacao.sp_registrar_anomalia(?, ?, ?, ?, ?)"), any(), any(), any(), any(), any()))
                .thenReturn(1);

        // 1. Etapa 'EMBARQUE' com quantidade explícita
        boolean sucesso1 = service.executarRegistroAnomalia(1, "EMBARQUE", "PESO_DIVERGENTE", "Divergência", 5);
        assertThat(sucesso1).isTrue();
        verify(jdbcTemplate).update("CALL sc_operacao.sp_registrar_anomalia(?, ?, ?, ?, ?)", 1, "EMBARQUE", "PESO_DIVERGENTE", "Divergência", 5);

        // 2. Etapa 'ORIGEM' com quantidade nula -> fallback para 1
        boolean sucesso2 = service.executarRegistroAnomalia(2, "ORIGEM", "DOCUMENTACAO_INCORRETA", null, null);
        assertThat(sucesso2).isTrue();
        verify(jdbcTemplate).update("CALL sc_operacao.sp_registrar_anomalia(?, ?, ?, ?, ?)", 2, "ORIGEM", "DOCUMENTACAO_INCORRETA", null, 1);

        // 3. Etapa 'DESEMBARQUE' com quantidade <= 0 -> fallback para 1
        boolean sucesso3 = service.executarRegistroAnomalia(3, "DESEMBARQUE", "LESAO_ANIMAL", "Animal ferido", 0);
        assertThat(sucesso3).isTrue();
        verify(jdbcTemplate).update("CALL sc_operacao.sp_registrar_anomalia(?, ?, ?, ?, ?)", 3, "DESEMBARQUE", "LESAO_ANIMAL", "Animal ferido", 1);

        // 4. Etapa 'DESTINO'
        boolean sucesso4 = service.executarRegistroAnomalia(4, "DESTINO", "OUTROS", "Outro motivo", 2);
        assertThat(sucesso4).isTrue();
        verify(jdbcTemplate).update("CALL sc_operacao.sp_registrar_anomalia(?, ?, ?, ?, ?)", 4, "DESTINO", "OUTROS", "Outro motivo", 2);
    }

    @Test
    @DisplayName("executarRegistroAnomalia com parâmetros obrigatórios nulos lança IllegalArgumentException")
    void deveLancarExcecaoQuandoParametrosObrigatoriosNulosNoRegistroAnomalia() {
        // relatorioId nulo
        assertThatThrownBy(() -> service.executarRegistroAnomalia(null, "EMBARQUE", "COD1", null, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("relatorioId, tipoEtapa e codigoAnomalia são obrigatórios");

        // tipoEtapa nula
        assertThatThrownBy(() -> service.executarRegistroAnomalia(1, null, "COD1", null, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("relatorioId, tipoEtapa e codigoAnomalia são obrigatórios");

        // codigoAnomalia nulo
        assertThatThrownBy(() -> service.executarRegistroAnomalia(1, "EMBARQUE", null, null, 1))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("relatorioId, tipoEtapa e codigoAnomalia são obrigatórios");

        verifyNoInteractions(jdbcTemplate);
    }

    @Test
    @DisplayName("executarRegistroAnomalia quando ocorre BadSqlGrammarException retorna false (fallback H2)")
    void deveRetornarFalseEmCasoDeBadSqlGrammarExceptionNoRegistroAnomalia() {
        SQLException sqlException = new SQLException("Procedure not found", "42883");
        BadSqlGrammarException ex = new BadSqlGrammarException("CALL sp_registrar_anomalia", "CALL ...", sqlException);
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any())).thenThrow(ex);

        boolean sucesso = service.executarRegistroAnomalia(10, "EMBARQUE", "COD", "Desc", 2);

        assertThat(sucesso).isFalse();
        verify(jdbcTemplate).update("CALL sc_operacao.sp_registrar_anomalia(?, ?, ?, ?, ?)", 10, "EMBARQUE", "COD", "Desc", 2);
    }

    @Test
    @DisplayName("executarRegistroAnomalia quando ocorre DataAccessException genérico relança exceção")
    void deveRelancarDataAccessExceptionGenericaNoRegistroAnomalia() {
        DataAccessResourceFailureException ex = new DataAccessResourceFailureException("Database offline");
        when(jdbcTemplate.update(anyString(), any(), any(), any(), any(), any())).thenThrow(ex);

        assertThatThrownBy(() -> service.executarRegistroAnomalia(10, "EMBARQUE", "COD", "Desc", 2))
                .isInstanceOf(DataAccessException.class)
                .hasMessageContaining("Database offline");

        verify(jdbcTemplate).update("CALL sc_operacao.sp_registrar_anomalia(?, ?, ?, ?, ?)", 10, "EMBARQUE", "COD", "Desc", 2);
    }
}
