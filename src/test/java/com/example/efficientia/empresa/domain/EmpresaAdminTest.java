package com.example.efficientia.empresa.domain;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import java.time.Instant;
import static org.junit.jupiter.api.Assertions.*;

class EmpresaAdminTest {
    @Test
    @DisplayName("Deve usar getters e setters de EmpresaAdmin")
    void gettersESettersEmpresaAdmin() {
        EmpresaAdmin admin = new EmpresaAdmin();
        Instant agora = Instant.now();

        admin.setId(100L);
        admin.setEmpresaId(200L);
        admin.setCodigoEmpresa("EMP");
        admin.setCnpjEmpresa("000");
        admin.setNome("Admin Teste");
        admin.setEmail("admin@teste.com");
        admin.setCpf("111");
        admin.setTelefone("222");
        admin.setCargo("Chefe");
        admin.setSenhaHash("hash");
        admin.setAtivo(false);
        admin.setCriadoEm(agora);
        admin.setAtualizadoEm(agora);

        assertEquals(100L, admin.getId());
        assertEquals(200L, admin.getEmpresaId());
        assertEquals("EMP", admin.getCodigoEmpresa());
        assertEquals("000", admin.getCnpjEmpresa());
        assertEquals("Admin Teste", admin.getNome());
        assertEquals("admin@teste.com", admin.getEmail());
        assertEquals("111", admin.getCpf());
        assertEquals("222", admin.getTelefone());
        assertEquals("Chefe", admin.getCargo());
        assertEquals("hash", admin.getSenhaHash());
        assertFalse(admin.getAtivo());
        assertEquals(agora, admin.getCriadoEm());
        assertEquals(agora, admin.getAtualizadoEm());
    }
}
