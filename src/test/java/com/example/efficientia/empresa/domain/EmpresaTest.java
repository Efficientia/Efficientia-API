package com.example.efficientia.empresa.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class EmpresaTest {
    @Test
    @DisplayName("Deve usar getters e setters da Empresa")
    void gettersESettersEmpresa() {
        Empresa empresa = new Empresa();
        Instant agora = Instant.now();

        empresa.setId(1L);
        empresa.setEnderecoId(10);
        empresa.setCodigoEmpresa("COD123");
        empresa.setNomeEmpresa("Nome");
        empresa.setRazaoSocial("Razao");
        empresa.setCnpj("12345678000199");
        empresa.setEmailCorporativo("email@teste.com");
        empresa.setNomeFantasia("Nome Fantasia");
        empresa.setTelefone("+55 67 99999-2048");
        empresa.setLogoUrl("/api/v1/empresas/1/logo/conteudo");
        empresa.setEtapaCadastro(2);
        empresa.setCadastroCompleto(true);
        empresa.setSenhaHash("hash");
        empresa.setAtivo(true);
        empresa.setCriadoEm(agora);
        empresa.setAtualizadoEm(agora);
        assertEquals(1L, empresa.getId());
        assertEquals(10, empresa.getEnderecoId());
        assertEquals("COD123", empresa.getCodigoEmpresa());
        assertEquals("Nome", empresa.getNomeEmpresa());
        assertEquals("Razao", empresa.getRazaoSocial());
        assertEquals("12345678000199", empresa.getCnpj());
        assertEquals("email@teste.com", empresa.getEmailCorporativo());
        assertEquals("Nome Fantasia", empresa.getNomeFantasia());
        assertEquals("+55 67 99999-2048", empresa.getTelefone());
        assertEquals("/api/v1/empresas/1/logo/conteudo", empresa.getLogoUrl());
        assertEquals(2, empresa.getEtapaCadastro());
        assertTrue(empresa.getCadastroCompleto());
        assertEquals("hash", empresa.getSenhaHash());
        assertTrue(empresa.getAtivo());
        assertEquals(agora, empresa.getCriadoEm());
        assertEquals(agora, empresa.getAtualizadoEm());
    }

    @Test
    @DisplayName("Deve testar construtor alternativo Empresa")
    void construtorAlternativoEmpresa() {
        Empresa empresa = new Empresa(2L, "COD456", "Nome Alt", "99999999000199", "alt@teste.com", "senha", false, null, null);
        assertEquals(2L, empresa.getId());
        assertEquals("COD456", empresa.getCodigoEmpresa());
        assertNull(empresa.getEnderecoId());
        assertEquals("Nome Alt", empresa.getRazaoSocial());
    }

    @Test
    @DisplayName("Deve testar construtor completo com campos complementares")
    void construtorCompletoEmpresa() {
        Instant agora = Instant.now();
        Empresa empresa = new Empresa(
                3L,
                50,
                "EFF12345",
                "Efficientia Transportes",
                "Efficientia",
                "Efficientia Transportes Ltda.",
                "12345678000190",
                "operacao@efficientia.com.br",
                "+55 67 99999-2048",
                "https://cdn.efficientia.com/logo.png",
                2,
                false,
                "hash_senha",
                true,
                agora,
                agora
        );

        assertEquals(3L, empresa.getId());
        assertEquals(50, empresa.getEnderecoId());
        assertEquals("EFF12345", empresa.getCodigoEmpresa());
        assertEquals("Efficientia Transportes", empresa.getNomeEmpresa());
        assertEquals("Efficientia", empresa.getNomeFantasia());
        assertEquals("Efficientia Transportes Ltda.", empresa.getRazaoSocial());
        assertEquals("12345678000190", empresa.getCnpj());
        assertEquals("operacao@efficientia.com.br", empresa.getEmailCorporativo());
        assertEquals("+55 67 99999-2048", empresa.getTelefone());
        assertEquals("https://cdn.efficientia.com/logo.png", empresa.getLogoUrl());
        assertEquals(2, empresa.getEtapaCadastro());
        assertFalse(empresa.getCadastroCompleto());
    }
}
