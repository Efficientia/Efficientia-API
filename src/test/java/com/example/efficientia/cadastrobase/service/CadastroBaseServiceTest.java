package com.example.efficientia.cadastrobase.service;

import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.CriarFazendaRequest;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.CriarUsuarioRequest;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.CriarCavaloRequest;
import com.example.efficientia.cadastrobase.api.CadastroDuplicadoException;
import com.example.efficientia.cadastrobase.api.CadastroInvalidoException;
import com.example.efficientia.cadastrobase.domain.TipoUsuario;
import com.example.efficientia.cadastrobase.persistence.EnderecoRepository;
import com.example.efficientia.cadastrobase.persistence.FazendaRepository;
import com.example.efficientia.cadastrobase.persistence.UsuarioEntity;
import com.example.efficientia.cadastrobase.persistence.UsuarioRepository;
import com.example.efficientia.cadastrobase.persistence.VeiculoCarretaRepository;
import com.example.efficientia.cadastrobase.persistence.VeiculoCavaloEntity;
import com.example.efficientia.cadastrobase.persistence.VeiculoCavaloRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CadastroBaseServiceTest {

    private UsuarioRepository usuarioRepository;
    private EnderecoRepository enderecoRepository;
    private FazendaRepository fazendaRepository;
    private VeiculoCavaloRepository cavaloRepository;
    private VeiculoCarretaRepository carretaRepository;
    private CadastroBaseService service;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        enderecoRepository = mock(EnderecoRepository.class);
        fazendaRepository = mock(FazendaRepository.class);
        cavaloRepository = mock(VeiculoCavaloRepository.class);
        carretaRepository = mock(VeiculoCarretaRepository.class);
        service = new CadastroBaseService(
                usuarioRepository,
                enderecoRepository,
                fazendaRepository,
                cavaloRepository,
                carretaRepository
        );
    }

    @Test
    void deveCriarUsuarioComSenhaCriptografada() {
        when(usuarioRepository.save(any(UsuarioEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var request = new CriarUsuarioRequest(
                TipoUsuario.motorista,
                "00000000002",
                "DEV-MOT-01",
                "Motorista Teste",
                LocalDate.of(1985, 1, 1),
                "motorista@efficientia.dev",
                "11999990002",
                "Senha@123"
        );

        var response = service.criarUsuario(request);
        ArgumentCaptor<UsuarioEntity> captor = ArgumentCaptor.forClass(UsuarioEntity.class);
        verify(usuarioRepository).save(captor.capture());

        assertEquals(TipoUsuario.motorista, response.tipo());
        assertEquals("00000000002", response.cpf());
        assertTrue(new BCryptPasswordEncoder().matches("Senha@123", captor.getValue().getSenhaHash()));
    }

    @Test
    void deveRejeitarFazendaQuandoUsuarioNaoForPecuarista() {
        when(usuarioRepository.existsByIdAndTipo(1, TipoUsuario.pecuarista)).thenReturn(false);

        assertThrows(
                CadastroInvalidoException.class,
                () -> service.criarFazenda(new CriarFazendaRequest(1, 1, "Fazenda Teste"))
        );
        verify(fazendaRepository, never()).save(any());
    }

    @Test
    void deveRejeitarPlacaDeCavaloDuplicada() {
        when(cavaloRepository.existsByPlaca("TST1A23")).thenReturn(true);

        assertThrows(
                CadastroDuplicadoException.class,
                () -> service.criarCavalo(new CriarCavaloRequest("tst1a23", true))
        );
        verify(cavaloRepository, never()).save(any(VeiculoCavaloEntity.class));
    }

    @Test
    void deveRejeitarUsuarioMenorDeIdade() {
        var request = new CriarUsuarioRequest(TipoUsuario.motorista, "1", "1", "1", LocalDate.now().minusYears(10), "a@a.com", "1", "1");
        assertThrows(CadastroInvalidoException.class, () -> service.criarUsuario(request));
    }

    @Test
    void deveRejeitarUsuarioCpfDuplicado() {
        when(usuarioRepository.existsByCpf(any())).thenReturn(true);
        var request = new CriarUsuarioRequest(TipoUsuario.motorista, "1", "1", "1", LocalDate.now().minusYears(20), "a@a.com", "1", "1");
        assertThrows(CadastroDuplicadoException.class, () -> service.criarUsuario(request));
    }

    @Test
    void deveRejeitarUsuarioEmailDuplicado() {
        when(usuarioRepository.existsByCpf(any())).thenReturn(false);
        when(usuarioRepository.existsByEmail(any())).thenReturn(true);
        var request = new CriarUsuarioRequest(TipoUsuario.motorista, "1", "1", "1", LocalDate.now().minusYears(20), "a@a.com", "1", "1");
        assertThrows(CadastroDuplicadoException.class, () -> service.criarUsuario(request));
    }

    @Test
    void deveCriarUsuarioCodigoInternoNuloEVazio() {
        when(usuarioRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        var request = new CriarUsuarioRequest(TipoUsuario.motorista, "1", null, "1", LocalDate.now().minusYears(20), "a@a.com", "1", "1");
        var resp1 = service.criarUsuario(request);
        
        var request2 = new CriarUsuarioRequest(TipoUsuario.motorista, "2", "   ", "1", LocalDate.now().minusYears(20), "b@b.com", "1", "1");
        var resp2 = service.criarUsuario(request2);
    }

    @Test
    void deveRejeitarFazendaSemEndereco() {
        when(usuarioRepository.existsByIdAndTipo(1, TipoUsuario.pecuarista)).thenReturn(true);
        when(enderecoRepository.existsById(1)).thenReturn(false);
        assertThrows(CadastroInvalidoException.class, () -> service.criarFazenda(new CriarFazendaRequest(1, 1, "Fazenda")));
    }

    @Test
    void deveCriarFazendaComSucesso() {
        when(usuarioRepository.existsByIdAndTipo(1, TipoUsuario.pecuarista)).thenReturn(true);
        when(enderecoRepository.existsById(1)).thenReturn(true);
        when(fazendaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.criarFazenda(new CriarFazendaRequest(1, 1, "Fazenda"));
    }

    @Test
    void deveCriarCavaloComSucesso() {
        when(cavaloRepository.existsByPlaca(any())).thenReturn(false);
        when(cavaloRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.criarCavalo(new CriarCavaloRequest("abc", true));
    }

    @Test
    void deveRejeitarCarretaPlacaDuplicada() {
        when(carretaRepository.existsByPlaca(any())).thenReturn(true);
        assertThrows(CadastroDuplicadoException.class, () -> service.criarCarreta(new com.example.efficientia.cadastrobase.api.CadastroBaseContracts.CriarCarretaRequest("abc", 50)));
    }

    @Test
    void deveCriarCarretaComSucesso() {
        when(carretaRepository.existsByPlaca(any())).thenReturn(false);
        when(carretaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.criarCarreta(new com.example.efficientia.cadastrobase.api.CadastroBaseContracts.CriarCarretaRequest("abc", 50));
    }

    @Test
    void deveCriarEnderecoComSucesso() {
        when(enderecoRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.criarEndereco(new com.example.efficientia.cadastrobase.api.CadastroBaseContracts.CriarEnderecoRequest("123", "rua", "1", "cid", "sp"));
    }

    @Test
    void deveListarBuscarAtualizarERemoverCavalos() {
        VeiculoCavaloEntity cavalo = new VeiculoCavaloEntity();
        cavalo.setId(3);
        cavalo.setPlaca("ABC1D23");
        cavalo.setAtivo(true);
        when(cavaloRepository.findAll()).thenReturn(java.util.List.of(cavalo));
        when(cavaloRepository.findById(3)).thenReturn(java.util.Optional.of(cavalo));
        when(cavaloRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(1, service.listarCavalos().size());
        assertEquals(3, service.buscarCavalo(3).id());
        var request = new com.example.efficientia.cadastrobase.api.CadastroBaseContracts.AtualizarCavaloRequest(
                "DEF2E34", false, 9, LocalDate.of(2027, 1, 1), 1200, "Scania", "R450", 2022
        );
        var atualizado = service.atualizarCavalo(3, request);
        assertEquals("DEF2E34", atualizado.placa());
        assertEquals(9, atualizado.empresaId());
        service.removerCavalo(3);
        verify(cavaloRepository).delete(cavalo);
    }

    @Test
    void deveRejeitarCavaloInexistenteOuPlacaDuplicadaNaAtualizacao() {
        VeiculoCavaloEntity cavalo = new VeiculoCavaloEntity();
        cavalo.setId(4);
        when(cavaloRepository.findById(4)).thenReturn(java.util.Optional.of(cavalo));
        when(cavaloRepository.findById(99)).thenReturn(java.util.Optional.empty());
        when(cavaloRepository.existsByPlacaAndIdNot("ABC1D23", 4)).thenReturn(true);

        assertThrows(CadastroInvalidoException.class, () -> service.buscarCavalo(99));
        assertThrows(CadastroInvalidoException.class, () -> service.removerCavalo(99));
        assertThrows(CadastroInvalidoException.class, () -> service.atualizarCavalo(99,
                new com.example.efficientia.cadastrobase.api.CadastroBaseContracts.AtualizarCavaloRequest(null, null, null, null, null, null, null, null)));
        assertThrows(CadastroDuplicadoException.class, () -> service.atualizarCavalo(4,
                new com.example.efficientia.cadastrobase.api.CadastroBaseContracts.AtualizarCavaloRequest("abc1d23", null, null, null, null, null, null, null)));
    }

    @Test
    void deveListarBuscarAtualizarERemoverCarretas() {
        com.example.efficientia.cadastrobase.persistence.VeiculoCarretaEntity carreta =
                new com.example.efficientia.cadastrobase.persistence.VeiculoCarretaEntity();
        carreta.setId(5);
        carreta.setPlaca("XYZ9W87");
        carreta.setCapacidadeCabecas(40);
        when(carretaRepository.findAll()).thenReturn(java.util.List.of(carreta));
        when(carretaRepository.findById(5)).thenReturn(java.util.Optional.of(carreta));
        when(carretaRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        assertEquals(1, service.listarCarretas().size());
        assertEquals(5, service.buscarCarreta(5).id());
        var request = new com.example.efficientia.cadastrobase.api.CadastroBaseContracts.AtualizarCarretaRequest(
                "QWE1R23", 35, false, 9, LocalDate.of(2027, 1, 1), "Facchini", "3E", "grade baixa"
        );
        assertEquals("QWE1R23", service.atualizarCarreta(5, request).placa());
        service.removerCarreta(5);
        verify(carretaRepository).delete(carreta);
    }

    @Test
    void deveRejeitarCarretaInexistenteCapacidadeInvalidaEPlacaDuplicada() {
        com.example.efficientia.cadastrobase.persistence.VeiculoCarretaEntity carreta =
                new com.example.efficientia.cadastrobase.persistence.VeiculoCarretaEntity();
        when(carretaRepository.findById(6)).thenReturn(java.util.Optional.of(carreta));
        when(carretaRepository.findById(99)).thenReturn(java.util.Optional.empty());
        when(carretaRepository.existsByPlacaAndIdNot("XYZ9W87", 6)).thenReturn(true);

        assertThrows(CadastroInvalidoException.class, () -> service.buscarCarreta(99));
        assertThrows(CadastroInvalidoException.class, () -> service.removerCarreta(99));
        assertThrows(CadastroInvalidoException.class, () -> service.atualizarCarreta(6,
                new com.example.efficientia.cadastrobase.api.CadastroBaseContracts.AtualizarCarretaRequest(null, 0, null, null, null, null, null, null)));
        assertThrows(CadastroDuplicadoException.class, () -> service.atualizarCarreta(6,
                new com.example.efficientia.cadastrobase.api.CadastroBaseContracts.AtualizarCarretaRequest("xyz9w87", null, null, null, null, null, null, null)));
    }
}
