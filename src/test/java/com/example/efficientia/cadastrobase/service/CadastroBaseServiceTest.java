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
}
