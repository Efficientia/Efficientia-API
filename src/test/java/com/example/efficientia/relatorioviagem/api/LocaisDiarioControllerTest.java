package com.example.efficientia.relatorioviagem.api;

import com.example.efficientia.cadastrobase.persistence.EnderecoEntity;
import com.example.efficientia.cadastrobase.persistence.EnderecoRepository;
import com.example.efficientia.cadastrobase.persistence.FazendaEntity;
import com.example.efficientia.cadastrobase.persistence.FazendaRepository;
import com.example.efficientia.cadastrobase.persistence.UnidadeFrigorificaEntity;
import com.example.efficientia.cadastrobase.persistence.UnidadeFrigorificaRepository;
import jakarta.persistence.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class LocaisDiarioControllerTest {

    private FazendaRepository fazendas;
    private UnidadeFrigorificaRepository unidades;
    private EnderecoRepository enderecos;
    private LocaisDiarioController controller;
    private MockMvc mockMvc;
    private Authentication auth;

    @BeforeEach
    void setUp() {
        fazendas = mock(FazendaRepository.class);
        unidades = mock(UnidadeFrigorificaRepository.class);
        enderecos = mock(EnderecoRepository.class);
        controller = new LocaisDiarioController(fazendas, unidades, enderecos);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        auth = new UsernamePasswordAuthenticationToken("motorista", "token", List.of());
    }

    @Test
    void locaisDoDiarioUsamTabelasDeIdsInteirosDaApi() {
        assertTable(EnderecoEntity.class, "tb_endereco");
        assertTable(FazendaEntity.class, "tb_fazenda");
        assertTable(UnidadeFrigorificaEntity.class, "tb_unidade_frigorifica");
    }

    private static void assertTable(Class<?> entityType, String expectedName) {
        Table table = entityType.getAnnotation(Table.class);
        assertEquals("sc_corporativo", table.schema());
        assertEquals(expectedName, table.name());
    }

    @Test
    void buscaFazendasPorNomeComPaginacaoEId() {
        FazendaEntity fazenda = new FazendaEntity();
        fazenda.setNome("Fazenda Aurora");
        when(fazendas.findByNomeContainingIgnoreCase(eq("Aurora"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(fazenda)));
        when(enderecos.findAllById(any())).thenReturn(List.of());

        var response = controller.listarFazendas(" Aurora ", 0, 20, auth);

        assertThat(response.itens()).hasSize(1);
        assertThat(response.itens().get(0).nome()).isEqualTo("Fazenda Aurora");
        assertThat(response.total()).isEqualTo(1L);
    }

    @Test
    void rejeitaCatalogoSemAutenticacao() {
        assertThrows(ResponseStatusException.class,
                () -> controller.listarFazendas("", 0, 20, null));
        assertThrows(ResponseStatusException.class,
                () -> controller.listarUnidades("", 0, 20, null));
    }

    @Test
    @DisplayName("Rejeita requisição quando objeto Authentication não está autenticado")
    void deveRejeitarCatalogoQuandoNaoEstiverAutenticado() {
        Authentication naoAutenticado = mock(Authentication.class);
        when(naoAutenticado.isAuthenticated()).thenReturn(false);

        assertThatThrownBy(() -> controller.listarFazendas("", 0, 20, naoAutenticado))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(rse.getReason()).isEqualTo("Autenticação necessária.");
                });

        assertThatThrownBy(() -> controller.listarUnidades("", 0, 20, naoAutenticado))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
                    assertThat(rse.getReason()).isEqualTo("Autenticação necessária.");
                });
    }

    @Test
    @DisplayName("listarUnidadesFrigorificas com termo de busca normalizado")
    void deveListarUnidadesFrigorificasComTermoDeBusca() {
        UnidadeFrigorificaEntity unidade = mock(UnidadeFrigorificaEntity.class);
        when(unidade.getId()).thenReturn(10);
        when(unidade.getNome()).thenReturn("Frigorífico JBS");
        when(unidade.getEnderecoId()).thenReturn(100);

        EnderecoEntity endereco = mock(EnderecoEntity.class);
        when(endereco.getId()).thenReturn(100);
        when(endereco.getCidade()).thenReturn("Campo Grande");
        when(endereco.getEstado()).thenReturn("MS");

        when(unidades.findByNomeContainingIgnoreCase(eq("JBS"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(unidade), PageRequest.of(0, 20), 1));
        when(enderecos.findAllById(List.of(100))).thenReturn(List.of(endereco));

        var response = controller.listarUnidades(" JBS ", 0, 20, auth);

        assertThat(response).isNotNull();
        assertThat(response.itens()).hasSize(1);
        assertThat(response.pagina()).isEqualTo(0);
        assertThat(response.tamanho()).isEqualTo(20);
        assertThat(response.total()).isEqualTo(1L);
        assertThat(response.totalPaginas()).isEqualTo(1);

        var item = response.itens().get(0);
        assertThat(item.id()).isEqualTo(10);
        assertThat(item.nome()).isEqualTo("Frigorífico JBS");
        assertThat(item.cidade()).isEqualTo("Campo Grande");
        assertThat(item.uf()).isEqualTo("MS");
    }

    @Test
    @DisplayName("listarUnidadesFrigorificas sem termo de busca (vazio ou apenas espaços)")
    void deveListarUnidadesFrigorificasSemTermoDeBusca() {
        when(unidades.findByNomeContainingIgnoreCase(eq(""), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        var responseVazio = controller.listarUnidades("", 0, 20, auth);
        assertThat(responseVazio.itens()).isEmpty();
        assertThat(responseVazio.total()).isZero();

        var responseEspacos = controller.listarUnidades("   ", 0, 20, auth);
        assertThat(responseEspacos.itens()).isEmpty();
        assertThat(responseEspacos.total()).isZero();
    }

    @Test
    @DisplayName("listarUnidadesFrigorificas com múltiplos resultados e endereços associados")
    void deveListarUnidadesFrigorificasComResultadosELocais() {
        UnidadeFrigorificaEntity u1 = mock(UnidadeFrigorificaEntity.class);
        when(u1.getId()).thenReturn(1);
        when(u1.getNome()).thenReturn("Unidade Sul");
        when(u1.getEnderecoId()).thenReturn(101);

        UnidadeFrigorificaEntity u2 = mock(UnidadeFrigorificaEntity.class);
        when(u2.getId()).thenReturn(2);
        when(u2.getNome()).thenReturn("Unidade Norte");
        when(u2.getEnderecoId()).thenReturn(102);

        EnderecoEntity e1 = mock(EnderecoEntity.class);
        when(e1.getId()).thenReturn(101);
        when(e1.getCidade()).thenReturn("Dourados");
        when(e1.getEstado()).thenReturn("MS");

        EnderecoEntity e2 = mock(EnderecoEntity.class);
        when(e2.getId()).thenReturn(102);
        when(e2.getCidade()).thenReturn("Três Lagoas");
        when(e2.getEstado()).thenReturn("MS");

        when(unidades.findByNomeContainingIgnoreCase(eq(""), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(u1, u2), PageRequest.of(0, 20), 2));
        when(enderecos.findAllById(any())).thenReturn(List.of(e1, e2));

        var response = controller.listarUnidades("", 0, 20, auth);

        assertThat(response.itens()).hasSize(2);
        assertThat(response.total()).isEqualTo(2L);
        assertThat(response.itens()).extracting(LocaisDiarioController.LocalDiarioResponse::cidade)
                .containsExactly("Dourados", "Três Lagoas");
        assertThat(response.itens()).extracting(LocaisDiarioController.LocalDiarioResponse::uf)
                .containsExactly("MS", "MS");
    }

    @Test
    @DisplayName("listarUnidadesFrigorificas com lista vazia")
    void deveListarUnidadesFrigorificasComListaVazia() {
        when(unidades.findByNomeContainingIgnoreCase(eq("Inexistente"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        var response = controller.listarUnidades("Inexistente", 0, 20, auth);

        assertThat(response.itens()).isEmpty();
        assertThat(response.total()).isZero();
        assertThat(response.totalPaginas()).isZero();
    }

    @Test
    @DisplayName("listarUnidadesFrigorificas com unidade sem endereço correspondente")
    void deveListarUnidadesFrigorificasQuandoEnderecoNaoForEncontrado() {
        UnidadeFrigorificaEntity unidade = mock(UnidadeFrigorificaEntity.class);
        when(unidade.getId()).thenReturn(5);
        when(unidade.getNome()).thenReturn("Unidade Sem Endereço");
        when(unidade.getEnderecoId()).thenReturn(999);

        when(unidades.findByNomeContainingIgnoreCase(eq("Sem"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(unidade), PageRequest.of(0, 20), 1));
        when(enderecos.findAllById(List.of(999))).thenReturn(List.of());

        var response = controller.listarUnidades("Sem", 0, 20, auth);

        assertThat(response.itens()).hasSize(1);
        assertThat(response.itens().get(0).cidade()).isNull();
        assertThat(response.itens().get(0).uf()).isNull();
    }

    @Test
    @DisplayName("listarEnderecos com busca por logradouro e cidade vinculados à fazenda")
    void deveListarEnderecosComBuscaPorLogradouroECidadeEmFazendas() {
        FazendaEntity fazenda = new FazendaEntity();
        fazenda.setNome("Fazenda Primavera");
        fazenda.setEnderecoId(200);

        EnderecoEntity endereco = mock(EnderecoEntity.class);
        when(endereco.getId()).thenReturn(200);
        when(endereco.getLogradouro()).thenReturn("Rodovia BR-163, Km 50");
        when(endereco.getCidade()).thenReturn("Rio Verde");
        when(endereco.getEstado()).thenReturn("GO");
        when(fazendas.findByNomeContainingIgnoreCase(eq("Primavera"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(fazenda), PageRequest.of(0, 20), 1));
        when(enderecos.findAllById(any())).thenReturn(List.of(endereco));

        var response = controller.listarFazendas("Primavera", 0, 20, auth);

        assertThat(response.itens()).hasSize(1);
        var item = response.itens().get(0);
        assertThat(item.nome()).isEqualTo("Fazenda Primavera");
        assertThat(item.cidade()).isEqualTo("Rio Verde");
        assertThat(item.uf()).isEqualTo("GO");
    }

    @Test
    @DisplayName("listarEnderecos com paginação personalizada")
    void deveListarEnderecosComPaginacao() {
        FazendaEntity f = new FazendaEntity();
        f.setNome("Fazenda Modelo");
        f.setEnderecoId(300);

        when(fazendas.findByNomeContainingIgnoreCase(eq(""), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(f), PageRequest.of(1, 10), 25));
        when(enderecos.findAllById(any())).thenReturn(List.of());

        var response = controller.listarFazendas("", 1, 10, auth);

        assertThat(response.pagina()).isEqualTo(1);
        assertThat(response.tamanho()).isEqualTo(10);
        assertThat(response.total()).isEqualTo(25L);
        assertThat(response.totalPaginas()).isEqualTo(3);
    }

    @Test
    @DisplayName("listarEnderecos com resultados vazios em fazendas")
    void deveListarEnderecosComResultadosVaziosEmFazendas() {
        when(fazendas.findByNomeContainingIgnoreCase(eq("NenhumLocal"), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 20), 0));

        var response = controller.listarFazendas("NenhumLocal", 0, 20, auth);

        assertThat(response.itens()).isEmpty();
        assertThat(response.total()).isZero();
        assertThat(response.totalPaginas()).isZero();
    }

    @Test
    @DisplayName("Filtra IDs nulos e deduplica ao buscar endereços")
    void deveFiltrarIdsNulosEDeduplicarAoBuscarEnderecos() {
        FazendaEntity f1 = new FazendaEntity();
        f1.setNome("Fazenda 1");
        f1.setEnderecoId(50);

        FazendaEntity f2 = new FazendaEntity();
        f2.setNome("Fazenda 2");
        f2.setEnderecoId(50);

        FazendaEntity f3 = new FazendaEntity();
        f3.setNome("Fazenda 3");
        f3.setEnderecoId(null);

        when(fazendas.findByNomeContainingIgnoreCase(eq(""), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(f1, f2, f3)));

        EnderecoEntity endereco = mock(EnderecoEntity.class);
        when(endereco.getId()).thenReturn(50);
        when(endereco.getCidade()).thenReturn("Corumbá");
        when(endereco.getEstado()).thenReturn("MS");
        when(enderecos.findAllById(any())).thenReturn(List.of(endereco));

        var response = controller.listarFazendas("", 0, 20, auth);

        assertThat(response.itens()).hasSize(3);
        assertThat(response.itens().get(0).cidade()).isEqualTo("Corumbá");
        assertThat(response.itens().get(1).cidade()).isEqualTo("Corumbá");
        assertThat(response.itens().get(2).cidade()).isNull();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Integer>> captor = ArgumentCaptor.forClass(List.class);
        verify(enderecos).findAllById(captor.capture());
        assertThat(captor.getValue()).containsExactly(50);
    }

    @Test
    @DisplayName("Mapeia LocalDiarioResponse corretamente quando endereço é nulo ou presente")
    void deveMapearLocalDiarioResponseComOuSemEndereco() {
        EnderecoEntity end = mock(EnderecoEntity.class);
        when(end.getCidade()).thenReturn("Bonito");
        when(end.getEstado()).thenReturn("MS");

        var comEndereco = LocaisDiarioController.LocalDiarioResponse.of(1, "Fazenda Bonita", end);
        assertThat(comEndereco.id()).isEqualTo(1);
        assertThat(comEndereco.nome()).isEqualTo("Fazenda Bonita");
        assertThat(comEndereco.cidade()).isEqualTo("Bonito");
        assertThat(comEndereco.uf()).isEqualTo("MS");

        var semEndereco = LocaisDiarioController.LocalDiarioResponse.of(2, "Fazenda Sem Endereço", null);
        assertThat(semEndereco.id()).isEqualTo(2);
        assertThat(semEndereco.nome()).isEqualTo("Fazenda Sem Endereço");
        assertThat(semEndereco.cidade()).isNull();
        assertThat(semEndereco.uf()).isNull();
    }

    @Test
    @DisplayName("Rejeita requisição quando pagina < 0")
    void deveLancarExcecaoQuandoPaginaForNegativa() {
        assertThatThrownBy(() -> controller.listarFazendas("", -1, 20, auth))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).isEqualTo("Paginação inválida.");
                });

        assertThatThrownBy(() -> controller.listarUnidades("", -1, 20, auth))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).isEqualTo("Paginação inválida.");
                });
    }

    @Test
    @DisplayName("Rejeita requisição quando tamanho <= 0")
    void deveLancarExcecaoQuandoTamanhoForMenorOuIgualAZero() {
        assertThatThrownBy(() -> controller.listarFazendas("", 0, 0, auth))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).isEqualTo("Paginação inválida.");
                });

        assertThatThrownBy(() -> controller.listarUnidades("", 0, 0, auth))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).isEqualTo("Paginação inválida.");
                });

        assertThatThrownBy(() -> controller.listarFazendas("", 0, -5, auth))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        assertThatThrownBy(() -> controller.listarUnidades("", 0, -5, auth))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @DisplayName("Rejeita requisição quando tamanho > 50")
    void deveLancarExcecaoQuandoTamanhoForMaiorQueCinquenta() {
        assertThatThrownBy(() -> controller.listarFazendas("", 0, 51, auth))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).isEqualTo("Paginação inválida.");
                });

        assertThatThrownBy(() -> controller.listarUnidades("", 0, 51, auth))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).isEqualTo("Paginação inválida.");
                });
    }

    @Test
    @DisplayName("Rejeita requisição quando termo de busca excede 100 caracteres")
    void deveLancarExcecaoQuandoBuscaExcederCemCaracteres() {
        String buscaLonga = "a".repeat(101);

        assertThatThrownBy(() -> controller.listarFazendas(buscaLonga, 0, 20, auth))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).isEqualTo("Busca deve ter até 100 caracteres.");
                });

        assertThatThrownBy(() -> controller.listarUnidades(buscaLonga, 0, 20, auth))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> {
                    ResponseStatusException rse = (ResponseStatusException) ex;
                    assertThat(rse.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(rse.getReason()).isEqualTo("Busca deve ter até 100 caracteres.");
                });
    }

    @Test
    @DisplayName("MockMvc GET /api/v1/locais-diario/unidades-frigorificas deve retornar 200 com itens")
    void mockMvcDeveListarUnidadesComSucesso() throws Exception {
        UnidadeFrigorificaEntity unidade = mock(UnidadeFrigorificaEntity.class);
        when(unidade.getId()).thenReturn(1);
        when(unidade.getNome()).thenReturn("Frigorífico Central");
        when(unidade.getEnderecoId()).thenReturn(null);

        when(unidades.findByNomeContainingIgnoreCase(eq(""), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(unidade), PageRequest.of(0, 20), 1));
        when(enderecos.findAllById(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/locais-diario/unidades-frigorificas")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens").isArray())
                .andExpect(jsonPath("$.itens[0].id").value(1))
                .andExpect(jsonPath("$.itens[0].nome").value("Frigorífico Central"))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(20))
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.totalPaginas").value(1));
    }

    @Test
    @DisplayName("MockMvc GET /api/v1/locais-diario/fazendas deve retornar 200 com itens")
    void mockMvcDeveListarFazendasComSucesso() throws Exception {
        FazendaEntity fazenda = new FazendaEntity();
        fazenda.setNome("Fazenda Alvorada");
        fazenda.setEnderecoId(null);

        when(fazendas.findByNomeContainingIgnoreCase(eq(""), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(fazenda), PageRequest.of(0, 20), 1));
        when(enderecos.findAllById(any())).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/locais-diario/fazendas")
                        .principal(auth))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens").isArray())
                .andExpect(jsonPath("$.itens[0].nome").value("Fazenda Alvorada"))
                .andExpect(jsonPath("$.pagina").value(0))
                .andExpect(jsonPath("$.tamanho").value(20))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    @DisplayName("MockMvc GET /api/v1/locais-diario/unidades-frigorificas sem autenticação deve retornar 401")
    void mockMvcDeveRetornar401QuandoNaoAutenticado() throws Exception {
        mockMvc.perform(get("/api/v1/locais-diario/unidades-frigorificas"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("MockMvc GET /api/v1/locais-diario/unidades-frigorificas com pagina < 0 deve retornar 400")
    void mockMvcDeveRetornar400QuandoPaginaInvalida() throws Exception {
        mockMvc.perform(get("/api/v1/locais-diario/unidades-frigorificas")
                        .param("pagina", "-1")
                        .principal(auth))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("MockMvc GET /api/v1/locais-diario/unidades-frigorificas com tamanho <= 0 deve retornar 400")
    void mockMvcDeveRetornar400QuandoTamanhoInvalido() throws Exception {
        mockMvc.perform(get("/api/v1/locais-diario/unidades-frigorificas")
                        .param("tamanho", "0")
                        .principal(auth))
                .andExpect(status().isBadRequest());
    }
}
