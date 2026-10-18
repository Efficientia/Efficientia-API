package com.example.efficientia.relatorioviagem.api;

import com.example.efficientia.cadastrobase.persistence.EnderecoRepository;
import com.example.efficientia.cadastrobase.persistence.EnderecoEntity;
import com.example.efficientia.cadastrobase.persistence.FazendaEntity;
import com.example.efficientia.cadastrobase.persistence.FazendaRepository;
import com.example.efficientia.cadastrobase.persistence.UnidadeFrigorificaEntity;
import com.example.efficientia.cadastrobase.persistence.UnidadeFrigorificaRepository;
import jakarta.persistence.Table;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class LocaisDiarioControllerTest {
    @Test
    void locaisDoDiarioUsamTabelasDeIdsInteirosDaApi() {
        assertTable(EnderecoEntity.class, "endereco");
        assertTable(FazendaEntity.class, "fazenda");
        assertTable(UnidadeFrigorificaEntity.class, "unidade_frigorifica");
    }

    private static void assertTable(Class<?> entityType, String expectedName) {
        Table table = entityType.getAnnotation(Table.class);
        assertEquals("public", table.schema());
        assertEquals(expectedName, table.name());
    }

    @Test
    void buscaFazendasPorNomeComPaginacaoEId() {
        FazendaRepository fazendas = mock(FazendaRepository.class);
        EnderecoRepository enderecos = mock(EnderecoRepository.class);
        LocaisDiarioController controller = new LocaisDiarioController(
                fazendas, mock(UnidadeFrigorificaRepository.class), enderecos);
        FazendaEntity fazenda = new FazendaEntity();
        fazenda.setNome("Fazenda Aurora");
        when(fazendas.findByNomeContainingIgnoreCase(eq("Aurora"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(fazenda)));
        when(enderecos.findAllById(any())).thenReturn(List.of());
        var auth = new UsernamePasswordAuthenticationToken("motorista", "token", List.of());

        var response = controller.listarFazendas(" Aurora ", 0, 20, auth);

        assertEquals("Fazenda Aurora", response.itens().get(0).nome());
        assertEquals(1, response.total());
    }

    @Test
    void rejeitaCatalogoSemAutenticacao() {
        LocaisDiarioController controller = new LocaisDiarioController(
                mock(FazendaRepository.class), mock(UnidadeFrigorificaRepository.class),
                mock(EnderecoRepository.class));
        assertThrows(ResponseStatusException.class,
                () -> controller.listarFazendas("", 0, 20, null));
    }
}
