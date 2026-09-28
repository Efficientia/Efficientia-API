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
        assertNull(empresa.getRazaoSocial());
    }
}
