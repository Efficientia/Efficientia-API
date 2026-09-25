package com.example.efficientia.empresa.service;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CodigoEmpresaGeneratorTest {

    private final CodigoEmpresaGenerator generator = new CodigoEmpresaGenerator();
    private static final Pattern PADRAO_CODIGO = Pattern.compile("^[A-Z]{3}[0-9]{5}$");

    @Test
    @DisplayName("Deve gerar código com 8 dígitos (3 primeiras letras e 5 números aleatórios)")
    void deveGerarCodigoCorretamente() {
        String codigo = generator.gerarCodigo("Friboi");

        assertNotNull(codigo);
        assertEquals(8, codigo.length());
        assertTrue(codigo.startsWith("FRI"));
        assertTrue(PADRAO_CODIGO.matcher(codigo).matches());
    }

    @Test
    @DisplayName("Deve remover acentos e caracteres especiais ao extrair prefixo")
    void deveRemoverAcentosEEspeciais() {
        String codigo = generator.gerarCodigo("Agropecuária Santa Fé S/A");

        assertNotNull(codigo);
        assertEquals(8, codigo.length());
        assertTrue(codigo.startsWith("AGR"));
        assertTrue(PADRAO_CODIGO.matcher(codigo).matches());
    }

    @Test
    @DisplayName("Deve gerar código válido quando o nome tiver menos de 3 letras")
    void deveGerarCodigoComNomeCurto() {
        String codigoOi = generator.gerarCodigo("Oi");
        assertEquals(8, codigoOi.length());
        assertTrue(codigoOi.startsWith("OI"));
        assertTrue(PADRAO_CODIGO.matcher(codigoOi).matches());

        String codigo3M = generator.gerarCodigo("3M");
        assertEquals(8, codigo3M.length());
        assertTrue(codigo3M.startsWith("M"));
        assertTrue(PADRAO_CODIGO.matcher(codigo3M).matches());
    }

    @Test
    @DisplayName("Deve gerar código válido mesmo quando nome for nulo ou sem letras")
    void deveGerarCodigoQuandoNuloOuSemLetras() {
        String codigoNulo = generator.gerarCodigo(null);
        assertEquals(8, codigoNulo.length());
        assertTrue(PADRAO_CODIGO.matcher(codigoNulo).matches());

        String codigoNumerico = generator.gerarCodigo("12345");
        assertEquals(8, codigoNumerico.length());
        assertTrue(PADRAO_CODIGO.matcher(codigoNumerico).matches());
    }
}
