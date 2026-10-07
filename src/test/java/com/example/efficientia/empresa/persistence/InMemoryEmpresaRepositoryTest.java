package com.example.efficientia.empresa.persistence;

import com.example.efficientia.empresa.domain.Empresa;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InMemoryEmpresaRepositoryTest {

    private final InMemoryEmpresaRepository repository = new InMemoryEmpresaRepository();

    @Test
    void salvaAtribuiIdsEAtualizaUmaEmpresaExistente() {
        Empresa primeira = empresa(null, "ABC12345", "12345678000199", "contato@abc.com");
        Empresa segunda = empresa(null, "XYZ54321", "98765432000199", "contato@xyz.com");

        repository.salvar(primeira);
        repository.salvar(segunda);

        assertEquals(1L, primeira.getId());
        assertEquals(2L, segunda.getId());
        assertSame(primeira, repository.buscarPorId(1L).orElseThrow());

        Empresa atualizada = empresa(1L, "ABC00001", "12345678000199", "novo@abc.com");
        repository.salvar(atualizada);
        assertSame(atualizada, repository.buscarPorId(1L).orElseThrow());
        assertEquals(2, repository.listarTodas().size());
    }

    @Test
    void consultasDeCodigoCnpjEEmailIgnoramMaiusculasEMinusculas() {
        Empresa cadastrada = repository.salvar(empresa(null, "AbC12345", "12345678000199", "Contato@abc.com"));

        assertSame(cadastrada, repository.buscarPorCodigo("abc12345").orElseThrow());
        assertSame(cadastrada, repository.buscarPorCnpj("12345678000199").orElseThrow());
        assertSame(cadastrada, repository.buscarPorEmail("contato@ABC.COM").orElseThrow());
        assertTrue(repository.existePorCodigo("ABC12345"));
        assertTrue(repository.existePorCnpj("12345678000199"));
        assertTrue(repository.existePorEmail("CONTATO@abc.com"));
    }

    @Test
    void consultasAusentesOuNulasRetornamVazioOuFalse() {
        repository.salvar(empresa(null, "ABC12345", "12345678000199", "contato@abc.com"));

        assertTrue(repository.buscarPorId(null).isEmpty());
        assertTrue(repository.buscarPorId(99L).isEmpty());
        assertTrue(repository.buscarPorCodigo(null).isEmpty());
        assertTrue(repository.buscarPorCodigo("NAOEXISTE").isEmpty());
        assertTrue(repository.buscarPorCnpj(null).isEmpty());
        assertTrue(repository.buscarPorCnpj("00000000000000").isEmpty());
        assertTrue(repository.buscarPorEmail(null).isEmpty());
        assertTrue(repository.buscarPorEmail("nao@existe.com").isEmpty());

        assertFalse(repository.existePorCodigo(null));
        assertFalse(repository.existePorCodigo("NAOEXISTE"));
        assertFalse(repository.existePorCnpj(null));
        assertFalse(repository.existePorCnpj("00000000000000"));
        assertFalse(repository.existePorEmail(null));
        assertFalse(repository.existePorEmail("nao@existe.com"));
    }

    @Test
    void listarTodasDevolveCopiaMutavelDaColecao() {
        Empresa cadastrada = repository.salvar(empresa(null, "ABC12345", "12345678000199", "contato@abc.com"));

        List<Empresa> empresas = repository.listarTodas();
        assertEquals(List.of(cadastrada), empresas);
        assertNotSame(empresas, repository.listarTodas());
        empresas.clear();
        assertEquals(1, repository.listarTodas().size());
    }

    private static Empresa empresa(Long id, String codigo, String cnpj, String email) {
        Instant agora = Instant.now();
        return new Empresa(id, codigo, "Empresa Teste", cnpj, email, null, true, agora, agora);
    }
}
