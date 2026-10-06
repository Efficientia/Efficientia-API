package com.example.efficientia.caminhao.service;

import com.example.efficientia.cadastrobase.api.CadastroDuplicadoException;
import com.example.efficientia.cadastrobase.api.CadastroInvalidoException;
import com.example.efficientia.cadastrobase.persistence.UsuarioEntity;
import com.example.efficientia.cadastrobase.persistence.UsuarioRepository;
import com.example.efficientia.cadastrobase.persistence.VeiculoCarretaEntity;
import com.example.efficientia.cadastrobase.persistence.VeiculoCarretaRepository;
import com.example.efficientia.cadastrobase.persistence.VeiculoCavaloEntity;
import com.example.efficientia.cadastrobase.persistence.VeiculoCavaloRepository;
import com.example.efficientia.caminhao.api.CaminhaoContracts.AtualizarCaminhaoRequest;
import com.example.efficientia.caminhao.api.CaminhaoContracts.CaminhaoAppResponse;
import com.example.efficientia.caminhao.api.CaminhaoContracts.CaminhaoRelatorioResponse;
import com.example.efficientia.caminhao.api.CaminhaoContracts.CaminhaoResponse;
import com.example.efficientia.caminhao.api.CaminhaoContracts.CriarCaminhaoRequest;
import com.example.efficientia.caminhao.api.CaminhaoContracts.TipoVeiculo;
import com.example.efficientia.caminhao.api.CaminhaoContracts.VincularCaminhaoRelatorioRequest;
import com.example.efficientia.caminhao.api.CaminhaoNotFoundException;
import com.example.efficientia.caminhao.api.InspecaoVencidaException;
import com.example.efficientia.relatorioviagem.api.RelatorioViagemNotFoundException;
import com.example.efficientia.relatorioviagem.persistence.RelatorioViagemEntity;
import com.example.efficientia.relatorioviagem.persistence.RelatorioViagemRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CaminhaoServiceTest {

    private VeiculoCavaloRepository veiculoCavaloRepository;
    private VeiculoCarretaRepository veiculoCarretaRepository;
    private RelatorioViagemRepository relatorioViagemRepository;
    private UsuarioRepository usuarioRepository;
    private CaminhaoService service;

    @BeforeEach
    void setUp() {
        veiculoCavaloRepository = mock(VeiculoCavaloRepository.class);
        veiculoCarretaRepository = mock(VeiculoCarretaRepository.class);
        relatorioViagemRepository = mock(RelatorioViagemRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);

        service = new CaminhaoService(
                veiculoCavaloRepository,
                veiculoCarretaRepository,
                relatorioViagemRepository,
                usuarioRepository
        );
    }

    @Test
    @DisplayName("Deve cadastrar cavalo mecânico com sucesso")
    void deveCriarCavaloComSucesso() {
        CriarCaminhaoRequest req = new CriarCaminhaoRequest(
                TipoVeiculo.CAVALO, "ABC1D23", null, 1, null, 12000,
                "Scania", "R450", 2022, null, LocalDate.now().plusMonths(6), true
        );

        when(veiculoCavaloRepository.existsByPlaca("ABC1D23")).thenReturn(false);
        when(veiculoCavaloRepository.save(any(VeiculoCavaloEntity.class))).thenAnswer(inv -> {
            VeiculoCavaloEntity e = inv.getArgument(0);
            e.setId(10);
            return e;
        });

        CaminhaoResponse res = service.criarCaminhao(req);

        assertNotNull(res);
        assertEquals(10, res.id());
        assertEquals("CAVALO", res.tipo());
        assertEquals("ABC1D23", res.placa());
        assertEquals("DISPONIVEL", res.statusUso());
        assertEquals(12000, res.kmAcumulado());
        verify(veiculoCavaloRepository).save(any(VeiculoCavaloEntity.class));
    }

    @Test
    @DisplayName("Deve rejeitar cavalo mecânico com placa duplicada")
    void deveRejeitarCavaloPlacaDuplicada() {
        CriarCaminhaoRequest req = new CriarCaminhaoRequest(
                TipoVeiculo.CAVALO, "ABC1D23", null, 1, null, 12000,
                "Scania", "R450", 2022, null, LocalDate.now().plusMonths(6), true
        );

        when(veiculoCavaloRepository.existsByPlaca("ABC1D23")).thenReturn(true);

        assertThrows(CadastroDuplicadoException.class, () -> service.criarCaminhao(req));
        verify(veiculoCavaloRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve cadastrar carreta com sucesso")
    void deveCriarCarretaComSucesso() {
        CriarCaminhaoRequest req = new CriarCaminhaoRequest(
                TipoVeiculo.CARRETA, "XYZ9W87", null, 1, 45, null,
                "Randon", "Gaiola Boiadeira", 2023, "Boiadeira", LocalDate.now().plusMonths(4), true
        );

        when(veiculoCarretaRepository.existsByPlaca("XYZ9W87")).thenReturn(false);
        when(veiculoCarretaRepository.save(any(VeiculoCarretaEntity.class))).thenAnswer(inv -> {
            VeiculoCarretaEntity e = inv.getArgument(0);
            e.setId(20);
            return e;
        });

        CaminhaoResponse res = service.criarCaminhao(req);

        assertNotNull(res);
        assertEquals(20, res.id());
        assertEquals("CARRETA", res.tipo());
        assertEquals("XYZ9W87", res.placa());
        assertEquals(45, res.capacidadeCabecas());
        assertEquals("DISPONIVEL", res.statusUso());
        verify(veiculoCarretaRepository).save(any(VeiculoCarretaEntity.class));
    }

    @Test
    @DisplayName("Deve rejeitar carreta sem capacidade positiva de cabeças")
    void deveRejeitarCarretaSemCapacidade() {
        CriarCaminhaoRequest req = new CriarCaminhaoRequest(
                TipoVeiculo.CARRETA, "XYZ9W87", null, 1, 0, null,
                "Randon", "Gaiola", 2023, "Boiadeira", null, true
        );

        assertThrows(CadastroInvalidoException.class, () -> service.criarCaminhao(req));
    }

    @Test
    @DisplayName("Deve cadastrar conjunto cavalo + carreta simultaneamente")
    void deveCriarConjuntoComSucesso() {
        CriarCaminhaoRequest req = new CriarCaminhaoRequest(
                TipoVeiculo.CONJUNTO, "ABC1D23", "XYZ9W87", 1, 50, 10000,
                "Volvo", "FH 540", 2021, "Boiadeira Dupla", null, true
        );

        when(veiculoCavaloRepository.existsByPlaca("ABC1D23")).thenReturn(false);
        when(veiculoCarretaRepository.existsByPlaca("XYZ9W87")).thenReturn(false);
        when(veiculoCavaloRepository.save(any(VeiculoCavaloEntity.class))).thenAnswer(inv -> {
            VeiculoCavaloEntity e = inv.getArgument(0);
            e.setId(10);
            return e;
        });
        when(veiculoCarretaRepository.save(any(VeiculoCarretaEntity.class))).thenAnswer(inv -> {
            VeiculoCarretaEntity e = inv.getArgument(0);
            e.setId(20);
            return e;
        });

        CaminhaoResponse res = service.criarCaminhao(req);

        assertNotNull(res);
        assertEquals(10, res.id());
        assertEquals("CAVALO", res.tipo());
        verify(veiculoCavaloRepository).save(any(VeiculoCavaloEntity.class));
        verify(veiculoCarretaRepository).save(any(VeiculoCarretaEntity.class));
    }

    @Test
    @DisplayName("Deve listar caminhões identificando se estão DISPONÍVEL ou EM_USO por relatório ativo")
    void deveListarCaminhoesComStatusUsoCorreto() {
        VeiculoCavaloEntity c1 = new VeiculoCavaloEntity();
        c1.setId(1);
        c1.setPlaca("AAA1A11");
        c1.setEmpresaId(100);
        c1.setAtivo(true);

        VeiculoCavaloEntity c2 = new VeiculoCavaloEntity();
        c2.setId(2);
        c2.setPlaca("BBB2B22");
        c2.setEmpresaId(100);
        c2.setAtivo(true);

        VeiculoCarretaEntity r1 = new VeiculoCarretaEntity();
        r1.setId(3);
        r1.setPlaca("CCC3C33");
        r1.setCapacidadeCabecas(40);
        r1.setEmpresaId(100);
        r1.setAtivo(true);

        when(veiculoCavaloRepository.findAll()).thenReturn(List.of(c1, c2));
        when(veiculoCarretaRepository.findAll()).thenReturn(List.of(r1));

        RelatorioViagemEntity relAtivo = new RelatorioViagemEntity();
        relAtivo.setId(500);
        relAtivo.setCavaloId(1);
        relAtivo.setCarretaId(3);
        relAtivo.setMotoristaId(88);
        relAtivo.setStatus("pendente");

        when(relatorioViagemRepository.findByStatusIn(any())).thenReturn(List.of(relAtivo));

        UsuarioEntity motorista = new UsuarioEntity();
        motorista.setId(88);
        motorista.setNome("Carlos Silva");
        when(usuarioRepository.findAllById(any())).thenReturn(List.of(motorista));

        List<CaminhaoResponse> lista = service.listarCaminhoes(100, true, "TODOS");

        assertEquals(3, lista.size());

        CaminhaoResponse c1Res = lista.stream().filter(c -> c.id().equals(1)).findFirst().orElseThrow();
        assertEquals("EM_USO", c1Res.statusUso());
        assertEquals(500, c1Res.relatorioAtualId());
        assertEquals(88, c1Res.motoristaAtualId());
        assertEquals("Carlos Silva", c1Res.motoristaAtualNome());

        CaminhaoResponse c2Res = lista.stream().filter(c -> c.id().equals(2)).findFirst().orElseThrow();
        assertEquals("DISPONIVEL", c2Res.statusUso());
        assertNull(c2Res.relatorioAtualId());

        CaminhaoResponse r1Res = lista.stream().filter(c -> c.id().equals(3)).findFirst().orElseThrow();
        assertEquals("EM_USO", r1Res.statusUso());
        assertEquals(500, r1Res.relatorioAtualId());
    }

    @Test
    @DisplayName("Deve listar caminhões para a tela do app mobile com validação de inspeção")
    void deveListarCaminhoesApp() {
        VeiculoCavaloEntity c = new VeiculoCavaloEntity();
        c.setId(1);
        c.setPlaca("AAA1A11");
        c.setAtivo(true);
        c.setDataVencimentoInspecao(LocalDate.now().plusDays(15));

        when(veiculoCavaloRepository.findAll()).thenReturn(List.of(c));
        when(veiculoCarretaRepository.findAll()).thenReturn(List.of());
        when(relatorioViagemRepository.findByStatusIn(any())).thenReturn(List.of());

        List<CaminhaoAppResponse> appList = service.listarCaminhoesApp(null);

        assertEquals(1, appList.size());
        assertTrue(appList.get(0).inspecaoValida());
        assertEquals(15L, appList.get(0).diasParaVencerInspecao());
        assertEquals("DISPONIVEL", appList.get(0).statusUso());
    }

    @Test
    @DisplayName("Deve listar apenas caminhões disponíveis e com inspeção em dia")
    void deveListarApenasDisponiveis() {
        VeiculoCavaloEntity c1 = new VeiculoCavaloEntity();
        c1.setId(1);
        c1.setPlaca("AAA1A11");
        c1.setAtivo(true);
        c1.setDataVencimentoInspecao(LocalDate.now().plusDays(20));

        VeiculoCavaloEntity c2Vencido = new VeiculoCavaloEntity();
        c2Vencido.setId(2);
        c2Vencido.setPlaca("BBB2B22");
        c2Vencido.setAtivo(true);
        c2Vencido.setDataVencimentoInspecao(LocalDate.now().minusDays(5));

        when(veiculoCavaloRepository.findAll()).thenReturn(List.of(c1, c2Vencido));
        when(veiculoCarretaRepository.findAll()).thenReturn(List.of());
        when(relatorioViagemRepository.findByStatusIn(any())).thenReturn(List.of());

        List<CaminhaoResponse> disponiveis = service.listarCaminhoesDisponiveis(null, "CAVALO");

        assertEquals(1, disponiveis.size());
        assertEquals("AAA1A11", disponiveis.get(0).placa());
    }

    @Test
    @DisplayName("Deve buscar caminhão por placa em cavalo ou carreta")
    void deveBuscarPorPlaca() {
        VeiculoCarretaEntity carreta = new VeiculoCarretaEntity();
        carreta.setId(5);
        carreta.setPlaca("XYZ9W87");
        carreta.setCapacidadeCabecas(42);

        when(veiculoCavaloRepository.findByPlaca("XYZ9W87")).thenReturn(Optional.empty());
        when(veiculoCarretaRepository.findByPlaca("XYZ9W87")).thenReturn(Optional.of(carreta));
        when(relatorioViagemRepository.findFirstByCarretaIdAndStatusInOrderByCriadoEmDesc(eq(5), any()))
                .thenReturn(Optional.empty());

        CaminhaoResponse res = service.buscarCaminhaoPorPlaca("xyz9w87");

        assertNotNull(res);
        assertEquals(5, res.id());
        assertEquals("CARRETA", res.tipo());
        assertEquals("XYZ9W87", res.placa());
    }

    @Test
    @DisplayName("Deve lançar CaminhaoNotFoundException se placa não existir")
    void deveFalharBuscaPlacaInexistente() {
        when(veiculoCavaloRepository.findByPlaca("ZZZ9Z99")).thenReturn(Optional.empty());
        when(veiculoCarretaRepository.findByPlaca("ZZZ9Z99")).thenReturn(Optional.empty());

        assertThrows(CaminhaoNotFoundException.class, () -> service.buscarCaminhaoPorPlaca("ZZZ9Z99"));
    }

    @Test
    @DisplayName("Deve atualizar caminhão com sucesso")
    void deveAtualizarCaminhao() {
        VeiculoCavaloEntity cavalo = new VeiculoCavaloEntity();
        cavalo.setId(10);
        cavalo.setPlaca("AAA1A11");
        cavalo.setKmAcumulado(5000);

        when(veiculoCavaloRepository.findById(10)).thenReturn(Optional.of(cavalo));
        when(veiculoCavaloRepository.save(any(VeiculoCavaloEntity.class))).thenAnswer(i -> i.getArgument(0));

        AtualizarCaminhaoRequest req = new AtualizarCaminhaoRequest(
                "AAA1A11", 1, null, 7500, "Scania", "R500", 2023, null, null, true
        );

        CaminhaoResponse res = service.atualizarCaminhao("CAVALO", 10, req);

        assertEquals(7500, res.kmAcumulado());
        assertEquals("Scania", res.marca());
        assertEquals("R500", res.modelo());
        verify(veiculoCavaloRepository).save(cavalo);
    }

    @Test
    @DisplayName("Deve remover caminhão: soft-delete se vinculado a relatório, hard delete se não vinculado")
    void deveRemoverCaminhao() {
        VeiculoCavaloEntity cavaloSemRel = new VeiculoCavaloEntity();
        cavaloSemRel.setId(1);
        when(veiculoCavaloRepository.findById(1)).thenReturn(Optional.of(cavaloSemRel));
        when(relatorioViagemRepository.findByCavaloId(1)).thenReturn(List.of());

        service.removerCaminhao("CAVALO", 1);
        verify(veiculoCavaloRepository).delete(cavaloSemRel);

        VeiculoCavaloEntity cavaloComRel = new VeiculoCavaloEntity();
        cavaloComRel.setId(2);
        cavaloComRel.setAtivo(true);
        when(veiculoCavaloRepository.findById(2)).thenReturn(Optional.of(cavaloComRel));
        when(relatorioViagemRepository.findByCavaloId(2)).thenReturn(List.of(new RelatorioViagemEntity()));

        service.removerCaminhao("CAVALO", 2);
        assertFalse(cavaloComRel.getAtivo());
        verify(veiculoCavaloRepository).save(cavaloComRel);
    }

    @Test
    @DisplayName("Deve puxar o caminhão (cavalo + carreta) vinculado a um relatório de viagem")
    void deveBuscarCaminhaoDoRelatorio() {
        RelatorioViagemEntity rel = new RelatorioViagemEntity();
        rel.setId(100);
        rel.setCavaloId(1);
        rel.setCarretaId(2);
        rel.setMotoristaId(50);
        rel.setStatus("rascunho");

        VeiculoCavaloEntity cavalo = new VeiculoCavaloEntity();
        cavalo.setId(1);
        cavalo.setPlaca("CAV1111");

        VeiculoCarretaEntity carreta = new VeiculoCarretaEntity();
        carreta.setId(2);
        carreta.setPlaca("CAR2222");
        carreta.setCapacidadeCabecas(48);

        UsuarioEntity motorista = new UsuarioEntity();
        motorista.setId(50);
        motorista.setNome("Joao Silva");

        when(relatorioViagemRepository.findById(100)).thenReturn(Optional.of(rel));
        when(veiculoCavaloRepository.findById(1)).thenReturn(Optional.of(cavalo));
        when(veiculoCarretaRepository.findById(2)).thenReturn(Optional.of(carreta));
        when(usuarioRepository.findById(50)).thenReturn(Optional.of(motorista));

        CaminhaoRelatorioResponse res = service.buscarCaminhaoDoRelatorio(100);

        assertNotNull(res);
        assertEquals(100, res.relatorioId());
        assertEquals("CAV1111", res.placaCavalo());
        assertEquals("CAR2222", res.placaCarreta());
        assertEquals("Joao Silva", res.motoristaNome());
        assertTrue(res.emUso());
    }

    @Test
    @DisplayName("Deve buscar caminhão ativo do motorista a partir do seu relatório aberto")
    void deveBuscarCaminhaoAtivoDoMotorista() {
        RelatorioViagemEntity rel = new RelatorioViagemEntity();
        rel.setId(200);
        rel.setCavaloId(1);
        rel.setCarretaId(2);
        rel.setMotoristaId(50);
        rel.setStatus("pendente");

        when(relatorioViagemRepository.findFirstByMotoristaIdAndStatusInOrderByCriadoEmDesc(eq(50), any()))
                .thenReturn(Optional.of(rel));
        when(relatorioViagemRepository.findById(200)).thenReturn(Optional.of(rel));

        VeiculoCavaloEntity cavalo = new VeiculoCavaloEntity();
        cavalo.setId(1);
        cavalo.setPlaca("CAV1111");
        VeiculoCarretaEntity carreta = new VeiculoCarretaEntity();
        carreta.setId(2);
        carreta.setPlaca("CAR2222");

        when(veiculoCavaloRepository.findById(1)).thenReturn(Optional.of(cavalo));
        when(veiculoCarretaRepository.findById(2)).thenReturn(Optional.of(carreta));

        CaminhaoRelatorioResponse res = service.buscarCaminhaoAtivoDoMotorista(50);

        assertNotNull(res);
        assertEquals(200, res.relatorioId());
        assertEquals("CAV1111", res.placaCavalo());
    }

    @Test
    @DisplayName("Deve vincular caminhão ao relatório informando as placas do cavalo e da carreta")
    void deveVincularCaminhaoAoRelatorioPorPlacas() {
        RelatorioViagemEntity rel = new RelatorioViagemEntity();
        rel.setId(300);
        rel.setCavaloId(99);
        rel.setCarretaId(98);
        rel.setMotoristaId(50);
        rel.setStatus("rascunho");

        VeiculoCavaloEntity novoCavalo = new VeiculoCavaloEntity();
        novoCavalo.setId(10);
        novoCavalo.setPlaca("NEW1C11");
        novoCavalo.setDataVencimentoInspecao(LocalDate.now().plusDays(30));

        VeiculoCarretaEntity novaCarreta = new VeiculoCarretaEntity();
        novaCarreta.setId(20);
        novaCarreta.setPlaca("NEW2C22");
        novaCarreta.setDataVencimentoInspecao(LocalDate.now().plusDays(30));

        when(relatorioViagemRepository.findById(300)).thenReturn(Optional.of(rel));
        when(veiculoCavaloRepository.findByPlaca("NEW1C11")).thenReturn(Optional.of(novoCavalo));
        when(veiculoCarretaRepository.findByPlaca("NEW2C22")).thenReturn(Optional.of(novaCarreta));
        when(veiculoCavaloRepository.findById(10)).thenReturn(Optional.of(novoCavalo));
        when(veiculoCarretaRepository.findById(20)).thenReturn(Optional.of(novaCarreta));
        when(usuarioRepository.findById(50)).thenReturn(Optional.of(new UsuarioEntity()));

        VincularCaminhaoRelatorioRequest req = new VincularCaminhaoRelatorioRequest(
                "NEW1C11", "NEW2C22", null, null, null
        );

        CaminhaoRelatorioResponse res = service.vincularCaminhaoAoRelatorio(300, req);

        assertNotNull(res);
        assertEquals(10, rel.getCavaloId());
        assertEquals(20, rel.getCarretaId());
        assertEquals("NEW1C11", res.placaCavalo());
        assertEquals("NEW2C22", res.placaCarreta());
        verify(relatorioViagemRepository).save(rel);
    }

    @Test
    @DisplayName("Deve bloquear vinculação se inspeção do cavalo estiver vencida (fn_validar_alocacao_viagem)")
    void deveBloquearVinculacaoComInspecaoVencida() {
        RelatorioViagemEntity rel = new RelatorioViagemEntity();
        rel.setId(300);

        VeiculoCavaloEntity cavaloVencido = new VeiculoCavaloEntity();
        cavaloVencido.setId(10);
        cavaloVencido.setPlaca("NEW1C11");
        cavaloVencido.setDataVencimentoInspecao(LocalDate.now().minusDays(1));

        VeiculoCarretaEntity carreta = new VeiculoCarretaEntity();
        carreta.setId(20);
        carreta.setPlaca("NEW2C22");
        carreta.setDataVencimentoInspecao(LocalDate.now().plusDays(30));

        when(relatorioViagemRepository.findById(300)).thenReturn(Optional.of(rel));
        when(veiculoCavaloRepository.findByPlaca("NEW1C11")).thenReturn(Optional.of(cavaloVencido));
        when(veiculoCarretaRepository.findByPlaca("NEW2C22")).thenReturn(Optional.of(carreta));

        VincularCaminhaoRelatorioRequest req = new VincularCaminhaoRelatorioRequest(
                "NEW1C11", "NEW2C22", null, null, null
        );

        assertThrows(InspecaoVencidaException.class, () -> service.vincularCaminhaoAoRelatorio(300, req));
        verify(relatorioViagemRepository, never()).save(any());
    }

    @Test
    void deveAtualizarCarretaEValidarCapacidadeEPlacaDuplicada() {
        VeiculoCarretaEntity carreta = new VeiculoCarretaEntity();
        carreta.setId(30);
        when(veiculoCarretaRepository.findById(30)).thenReturn(Optional.of(carreta));
        when(veiculoCarretaRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(veiculoCarretaRepository.existsByPlacaAndIdNot("NEW1C11", 30)).thenReturn(true);

        var atualizada = service.atualizarCaminhao("CARRETA", 30,
                new AtualizarCaminhaoRequest("XYZ9W87", 4, 38, null, "Facchini", "Randon", null,
                        "baixa", LocalDate.now().plusDays(20), true));
        assertEquals(38, atualizada.capacidadeCabecas());
        assertEquals("baixa", atualizada.tipoCarreta());

        assertThrows(CadastroInvalidoException.class, () -> service.atualizarCaminhao("CARRETA", 30,
                new AtualizarCaminhaoRequest(null, null, 0, null, null, null, null, null, null, null)));
        assertThrows(CadastroDuplicadoException.class, () -> service.atualizarCaminhao("CARRETA", 30,
                new AtualizarCaminhaoRequest("new1c11", null, null, null, null, null, null, null, null, null)));
        assertThrows(CaminhaoNotFoundException.class, () -> service.atualizarCaminhao("CARRETA", 404,
                new AtualizarCaminhaoRequest(null, null, null, null, null, null, null, null, null, null)));
    }

    @Test
    void deveValidarCadastroDeConjuntoECapacidadeDaCarreta() {
        assertThrows(CadastroInvalidoException.class, () -> service.criarCaminhao(
                new CriarCaminhaoRequest(TipoVeiculo.CARRETA, "ABC1D23", null, 1, null, null,
                        null, null, null, null, null, true)));
        assertThrows(CadastroInvalidoException.class, () -> service.criarCaminhao(
                new CriarCaminhaoRequest(TipoVeiculo.CONJUNTO, "ABC1D23", " ", 1, 40, null,
                        null, null, null, null, null, true)));
        assertThrows(CadastroInvalidoException.class, () -> service.criarCaminhao(
                new CriarCaminhaoRequest(null, "ABC1D23", null, 1, null, null,
                        null, null, null, null, null, true)));
    }

    @Test
    void deveExcluirCarretaFisicamenteOuDesativarQuandoVinculada() {
        VeiculoCarretaEntity semViagem = new VeiculoCarretaEntity();
        semViagem.setId(40);
        VeiculoCarretaEntity vinculada = new VeiculoCarretaEntity();
        vinculada.setId(41);
        vinculada.setAtivo(true);
        when(veiculoCarretaRepository.findById(40)).thenReturn(Optional.of(semViagem));
        when(veiculoCarretaRepository.findById(41)).thenReturn(Optional.of(vinculada));
        when(relatorioViagemRepository.findByCarretaId(40)).thenReturn(List.of());
        when(relatorioViagemRepository.findByCarretaId(41)).thenReturn(List.of(new RelatorioViagemEntity()));

        service.removerCaminhao("CARRETA", 40);
        service.removerCaminhao("CARRETA", 41);

        verify(veiculoCarretaRepository).delete(semViagem);
        verify(veiculoCarretaRepository).save(vinculada);
        assertFalse(vinculada.getAtivo());
        assertThrows(CaminhaoNotFoundException.class, () -> service.removerCaminhao("CARRETA", 404));
    }

    @Test
    void deveResolverBuscaPorIdSemTipoEInformarAusencias() {
        VeiculoCarretaEntity carreta = new VeiculoCarretaEntity();
        carreta.setId(50);
        carreta.setPlaca("XYZ9W87");
        when(veiculoCavaloRepository.findById(50)).thenReturn(Optional.empty());
        when(veiculoCarretaRepository.findById(50)).thenReturn(Optional.of(carreta));
        when(relatorioViagemRepository.findFirstByCarretaIdAndStatusInOrderByCriadoEmDesc(eq(50), any()))
                .thenReturn(Optional.empty());

        assertEquals("CARRETA", service.buscarCaminhaoPorId(null, 50).tipo());
        assertThrows(CaminhaoNotFoundException.class, () -> service.buscarCaminhaoPorId("CARRETA", 404));
        assertThrows(CaminhaoNotFoundException.class, () -> service.buscarCaminhaoPorId("outro", 404));
    }

    @Test
    void deveAplicarFiltrosDeEmpresaAtividadeETipoNaListagem() {
        RelatorioViagemEntity rel = new RelatorioViagemEntity();
        rel.setId(70);
        rel.setCavaloId(71);
        rel.setMotoristaId(72);
        VeiculoCavaloEntity emUso = new VeiculoCavaloEntity();
        emUso.setId(71);
        emUso.setEmpresaId(1);
        emUso.setAtivo(true);
        emUso.setPlaca("ABC1D23");
        VeiculoCavaloEntity outraEmpresa = new VeiculoCavaloEntity();
        outraEmpresa.setId(73);
        outraEmpresa.setEmpresaId(2);
        outraEmpresa.setAtivo(true);
        VeiculoCavaloEntity inativo = new VeiculoCavaloEntity();
        inativo.setId(74);
        inativo.setEmpresaId(1);
        inativo.setAtivo(false);
        VeiculoCarretaEntity carreta = new VeiculoCarretaEntity();
        carreta.setId(75);
        carreta.setEmpresaId(1);
        carreta.setAtivo(true);
        UsuarioEntity motorista = new UsuarioEntity();
        motorista.setId(72);
        motorista.setNome("Motorista");
        when(relatorioViagemRepository.findByStatusIn(any())).thenReturn(List.of(rel), List.of());
        when(usuarioRepository.findAllById(any())).thenReturn(List.of(motorista));
        when(veiculoCavaloRepository.findAll()).thenReturn(List.of(emUso, outraEmpresa, inativo));
        when(veiculoCarretaRepository.findAll()).thenReturn(List.of(carreta));

        var cavalos = service.listarCaminhoes(1, true, "CAVALO");
        assertEquals(1, cavalos.size());
        assertEquals("EM_USO", cavalos.get(0).statusUso());
        assertEquals("Motorista", cavalos.get(0).motoristaAtualNome());
        assertTrue(service.listarCaminhoes(1, false, "CARRETA").isEmpty());
        assertTrue(service.listarCaminhoes(null, null, "DESCONHECIDO").isEmpty());
    }

    @Test
    void deveInformarCaminhaoDisponivelECondicaoSemMotorista() {
        RelatorioViagemEntity rel = new RelatorioViagemEntity();
        rel.setId(80);
        rel.setCavaloId(81);
        rel.setCarretaId(82);
        rel.setStatus("aprovado");
        VeiculoCavaloEntity cavalo = new VeiculoCavaloEntity();
        cavalo.setId(81);
        cavalo.setPlaca("ABC1D23");
        VeiculoCarretaEntity carreta = new VeiculoCarretaEntity();
        carreta.setId(82);
        carreta.setPlaca("XYZ9W87");
        when(relatorioViagemRepository.findById(80)).thenReturn(Optional.of(rel));
        when(veiculoCavaloRepository.findById(81)).thenReturn(Optional.of(cavalo));
        when(veiculoCarretaRepository.findById(82)).thenReturn(Optional.of(carreta));
        when(usuarioRepository.findById(null)).thenReturn(Optional.empty());

        var response = service.buscarCaminhaoDoRelatorio(80);

        assertFalse(response.emUso());
        assertEquals("Desconhecido", response.motoristaNome());
        assertEquals("DISPONIVEL", response.cavalo().statusUso());
    }
}
