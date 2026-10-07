package com.example.efficientia.empresa.persistence;

import com.example.efficientia.empresa.domain.EmpresaAdmin;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryEmpresaAdminRepositoryTest {

    @Test
    @DisplayName("Deve atribuir IDs sequenciais e substituir o registro ao salvar ID existente")
    void deveAtribuirIdsSequenciaisEAtualizarRegistroExistente() {
        InMemoryEmpresaAdminRepository repository = new InMemoryEmpresaAdminRepository();
        EmpresaAdmin primeiro = admin(null, 1L, "EMP123", "Ana", "ana@empresa.com", "11122233344");
        EmpresaAdmin segundo = admin(null, 1L, "EMP123", "Bia", "bia@empresa.com", "22233344455");

        assertSame(primeiro, repository.salvar(primeiro));
        assertSame(segundo, repository.salvar(segundo));
        assertEquals(1L, primeiro.getId());
        assertEquals(2L, segundo.getId());

        EmpresaAdmin atualizado = admin(1L, 2L, "OUT456", "Ana Atualizada", "ana.nova@empresa.com", "33344455566");
        assertSame(atualizado, repository.salvar(atualizado));
        assertSame(atualizado, repository.buscarPorId(1L).orElseThrow());
        assertEquals(1, repository.listarPorEmpresaId(1L).size());
        assertEquals(1, repository.listarPorEmpresaId(2L).size());
    }

    @Test
    @DisplayName("Deve manter ID explícito e buscar por e-mail e CPF sem diferenciar caixa")
    void deveBuscarPorEmailCpfECodigoIgnorandoCaixa() {
        InMemoryEmpresaAdminRepository repository = new InMemoryEmpresaAdminRepository();
        EmpresaAdmin cadastrado = repository.salvar(admin(50L, 7L, "AbC123", "João", "Joao@Empresa.com", "12345678901"));

        assertEquals(50L, cadastrado.getId());
        assertSame(cadastrado, repository.buscarPorEmail("JOAO@EMPRESA.COM").orElseThrow());
        assertSame(cadastrado, repository.buscarPorCpf("12345678901").orElseThrow());
        assertSame(cadastrado, repository.listarPorCodigoEmpresa("abc123").get(0));
        assertTrue(repository.existePorEmail("JOAO@EMPRESA.COM"));
        assertTrue(repository.existePorCpf("12345678901"));
        assertEquals(1L, repository.contarPorCodigoEmpresa("ABC123"));
    }

    @Test
    @DisplayName("Deve retornar vazio e falso para buscas por chaves nulas")
    void deveTratarChavesNulasNasConsultas() {
        InMemoryEmpresaAdminRepository repository = new InMemoryEmpresaAdminRepository();
        repository.salvar(admin(null, 1L, "EMP123", "Ana", "ana@empresa.com", "11122233344"));

        assertTrue(repository.buscarPorId(null).isEmpty());
        assertTrue(repository.buscarPorEmail(null).isEmpty());
        assertTrue(repository.buscarPorCpf(null).isEmpty());
        assertTrue(repository.listarPorEmpresaId(null).isEmpty());
        assertTrue(repository.listarPorCodigoEmpresa(null).isEmpty());
        assertFalse(repository.existePorEmail(null));
        assertFalse(repository.existePorCpf(null));
        assertEquals(0L, repository.contarPorEmpresaId(null));
        assertEquals(0L, repository.contarPorCodigoEmpresa(null));
    }

    @Test
    @DisplayName("Deve retornar vazio, falso ou zero quando a chave não corresponde a um registro")
    void deveRetornarResultadosVaziosParaChavesInexistentes() {
        InMemoryEmpresaAdminRepository repository = new InMemoryEmpresaAdminRepository();
        repository.salvar(admin(null, 1L, "EMP123", "Ana", "ana@empresa.com", "11122233344"));

        assertTrue(repository.buscarPorId(99L).isEmpty());
        assertTrue(repository.buscarPorEmail("nao@empresa.com").isEmpty());
        assertTrue(repository.buscarPorCpf("99999999999").isEmpty());
        assertTrue(repository.listarPorEmpresaId(2L).isEmpty());
        assertTrue(repository.listarPorCodigoEmpresa("INEXISTENTE").isEmpty());
        assertFalse(repository.existePorEmail("nao@empresa.com"));
        assertFalse(repository.existePorCpf("99999999999"));
        assertEquals(0L, repository.contarPorEmpresaId(2L));
        assertEquals(0L, repository.contarPorCodigoEmpresa("INEXISTENTE"));
    }

    @Test
    @DisplayName("Deve considerar múltiplos administradores ao listar e contar por empresa e código")
    void deveListarEContarPorEmpresaEPorCodigo() {
        InMemoryEmpresaAdminRepository repository = new InMemoryEmpresaAdminRepository();
        EmpresaAdmin ana = repository.salvar(admin(null, 1L, "AbC123", "Ana", "ana@empresa.com", "11122233344"));
        EmpresaAdmin bia = repository.salvar(admin(null, 1L, "abc123", "Bia", "bia@empresa.com", "22233344455"));
        EmpresaAdmin caio = repository.salvar(admin(null, 2L, "OUT456", "Caio", "caio@empresa.com", "33344455566"));

        List<EmpresaAdmin> porEmpresa = repository.listarPorEmpresaId(1L);
        List<EmpresaAdmin> porCodigo = repository.listarPorCodigoEmpresa("ABC123");

        assertEquals(2, porEmpresa.size());
        assertTrue(porEmpresa.containsAll(List.of(ana, bia)));
        assertEquals(2, porCodigo.size());
        assertTrue(porCodigo.containsAll(List.of(ana, bia)));
        assertEquals(2L, repository.contarPorEmpresaId(1L));
        assertEquals(2L, repository.contarPorCodigoEmpresa("aBc123"));
        assertEquals(1L, repository.contarPorEmpresaId(2L));
        assertEquals(1L, repository.contarPorCodigoEmpresa("out456"));
        assertSame(caio, repository.buscarPorId(caio.getId()).orElseThrow());
    }

    private static EmpresaAdmin admin(
            Long id,
            Long empresaId,
            String codigoEmpresa,
            String nome,
            String email,
            String cpf
    ) {
        Instant agora = Instant.now();
        return new EmpresaAdmin(
                id, empresaId, codigoEmpresa, "12345678000195", nome, email, cpf,
                null, "Administrador", "hash", true, agora, agora
        );
    }
}
