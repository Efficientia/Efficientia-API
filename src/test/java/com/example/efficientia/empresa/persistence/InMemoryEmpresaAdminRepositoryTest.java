package com.example.efficientia.empresa.persistence;

import com.example.efficientia.empresa.domain.EmpresaAdmin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class InMemoryEmpresaAdminRepositoryTest {

    @Test
    @DisplayName("Deve cobrir operacoes basicas de salvar, buscar e listar")
    void cobrirRepositorioMemoria() {
        InMemoryEmpresaAdminRepository repo = new InMemoryEmpresaAdminRepository();
        
        EmpresaAdmin admin = new EmpresaAdmin(
                null, 1L, "EMP123", "0000", "Joao", "joao@teste.com", 
                "11122233344", null, "Gerente", "hash", true, 
                Instant.now(), Instant.now()
        );

        EmpresaAdmin salvo = repo.salvar(admin);
        assertNotNull(salvo.getId());

        assertTrue(repo.existePorEmail("joao@teste.com"));
        assertFalse(repo.existePorEmail("outro@teste.com"));
        
        assertTrue(repo.existePorCpf("11122233344"));
        assertFalse(repo.existePorCpf("00000000000"));

        Optional<EmpresaAdmin> buscaId = repo.buscarPorId(salvo.getId());
        assertTrue(buscaId.isPresent());
        assertEquals("Joao", buscaId.get().getNome());
        
        assertFalse(repo.buscarPorId(999L).isPresent());

        Optional<EmpresaAdmin> buscaEmail = repo.buscarPorEmail("joao@teste.com");
        assertTrue(buscaEmail.isPresent());

        Optional<EmpresaAdmin> buscaCpf = repo.buscarPorCpf("11122233344");
        assertTrue(buscaCpf.isPresent());

        List<EmpresaAdmin> admins = repo.listarPorEmpresaId(1L);
        assertEquals(1, admins.size());
        
        assertEquals(1L, repo.contarPorEmpresaId(1L));
        assertEquals(0L, repo.contarPorEmpresaId(2L));
        
        // Testa atualizacao (quando salva de novo com mesmo id)
        salvo.setNome("Joao Silva");
        repo.salvar(salvo);
        assertEquals("Joao Silva", repo.buscarPorId(salvo.getId()).get().getNome());
    }
}
