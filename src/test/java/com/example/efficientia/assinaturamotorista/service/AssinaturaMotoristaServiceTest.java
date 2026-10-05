package com.example.efficientia.assinaturamotorista.service;

import com.example.efficientia.assinaturamotorista.api.AssinaturaFormatoInvalidoException;
import com.example.efficientia.assinaturamotorista.api.AssinaturaMotoristaContracts.AssinaturaMetadadosRequest;
import com.example.efficientia.assinaturamotorista.api.AssinaturaMotoristaContracts.AssinaturaMotoristaResponse;
import com.example.efficientia.assinaturamotorista.api.AssinaturaNaoEncontradaException;
import com.example.efficientia.assinaturamotorista.api.MotoristaInvalidoException;
import com.example.efficientia.assinaturamotorista.domain.ModalidadeAssinaturaMotorista;
import com.example.efficientia.assinaturamotorista.persistence.AssinaturaMotoristaEntity;
import com.example.efficientia.assinaturamotorista.persistence.AssinaturaMotoristaRepository;
import com.example.efficientia.cadastrobase.domain.TipoUsuario;
import com.example.efficientia.cadastrobase.persistence.UsuarioEntity;
import com.example.efficientia.cadastrobase.persistence.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@DisplayName("AssinaturaMotoristaService Unit Tests")
class AssinaturaMotoristaServiceTest {

    private static final byte[] VALID_PNG = new byte[]{
            (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47,
            (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A,
            0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52
    };

    private AssinaturaMotoristaRepository assinaturaRepository;
    private UsuarioRepository usuarioRepository;
    private AssinaturaMotoristaService service;

    @BeforeEach
    void setUp() {
        assinaturaRepository = mock(AssinaturaMotoristaRepository.class);
        usuarioRepository = mock(UsuarioRepository.class);
        service = new AssinaturaMotoristaService(assinaturaRepository, usuarioRepository);
    }

    @Test
    @DisplayName("salvarOuAtualizarAssinatura: lança AssinaturaFormatoInvalidoException quando Idempotency-Key for nula")
    void salvarOuAtualizarAssinatura_quandoIdempotencyKeyNula_deveLancarExcecao() {
        AssinaturaMetadadosRequest metadados = new AssinaturaMetadadosRequest(ModalidadeAssinaturaMotorista.DESENHO, null);

        AssinaturaFormatoInvalidoException exception = assertThrows(
                AssinaturaFormatoInvalidoException.class,
                () -> service.salvarOuAtualizarAssinatura(1, 1, null, metadados, VALID_PNG, true)
        );

        assertTrue(exception.getMessage().contains("Idempotency-Key é obrigatório"));
        verifyNoInteractions(usuarioRepository);
        verifyNoInteractions(assinaturaRepository);
    }

    @Test
    @DisplayName("salvarOuAtualizarAssinatura: retorna entidade existente quando Idempotency-Key já foi processada")
    void salvarOuAtualizarAssinatura_quandoIdempotencyKeyExiste_deveRetornarExistenteSemPersistir() {
        UUID idempotencyKey = UUID.randomUUID();
        AssinaturaMetadadosRequest metadados = new AssinaturaMetadadosRequest(ModalidadeAssinaturaMotorista.DESENHO, null);

        AssinaturaMotoristaEntity existente = new AssinaturaMotoristaEntity();
        existente.setId(UUID.randomUUID());
        existente.setMotoristaId(10);
        existente.setModalidade("DESENHO");
        existente.setMimeType("image/png");
        existente.setTamanhoBytes(16L);
        existente.setSha256("fake-hash");
        existente.setIdempotencyKey(idempotencyKey);
        existente.setAtiva(true);
        existente.setCriadoPor(10);
        existente.setCriadoEm(Instant.now());
        existente.setAtualizadoEm(Instant.now());
        existente.setVersao(0L);

        when(assinaturaRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.of(existente));

        AssinaturaMotoristaResponse response = service.salvarOuAtualizarAssinatura(10, 10, idempotencyKey, metadados, VALID_PNG, true);

        assertNotNull(response);
        assertEquals(existente.getId(), response.id());
        assertEquals(10, response.usuarioId());
        assertEquals("DESENHO", response.modalidade());
        assertEquals("/api/v1/usuarios/me/assinatura/conteudo", response.conteudoUrl());

        verify(assinaturaRepository).findByIdempotencyKey(idempotencyKey);
        verify(assinaturaRepository, never()).save(any());
        verifyNoInteractions(usuarioRepository);
    }

    @Test
    @DisplayName("salvarOuAtualizarAssinatura: lança AssinaturaNaoEncontradaException quando motorista não for encontrado")
    void salvarOuAtualizarAssinatura_quandoMotoristaNaoEncontrado_deveLancarExcecao() {
        UUID idempotencyKey = UUID.randomUUID();
        AssinaturaMetadadosRequest metadados = new AssinaturaMetadadosRequest(ModalidadeAssinaturaMotorista.DESENHO, null);

        when(assinaturaRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(usuarioRepository.findById(999)).thenReturn(Optional.empty());

        AssinaturaNaoEncontradaException exception = assertThrows(
                AssinaturaNaoEncontradaException.class,
                () -> service.salvarOuAtualizarAssinatura(999, 1, idempotencyKey, metadados, VALID_PNG, false)
        );

        assertTrue(exception.getMessage().contains("Motorista não encontrado com id: 999"));
        verify(assinaturaRepository, never()).save(any());
    }

    @Test
    @DisplayName("salvarOuAtualizarAssinatura: lança MotoristaInvalidoException quando tipo do usuário não for motorista")
    void salvarOuAtualizarAssinatura_quandoUsuarioNaoForMotorista_deveLancarExcecao() {
        UUID idempotencyKey = UUID.randomUUID();
        AssinaturaMetadadosRequest metadados = new AssinaturaMetadadosRequest(ModalidadeAssinaturaMotorista.DESENHO, null);

        UsuarioEntity usuarioAdmin = new UsuarioEntity();
        usuarioAdmin.setId(2);
        usuarioAdmin.setTipo(TipoUsuario.administrador);

        when(assinaturaRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(usuarioRepository.findById(2)).thenReturn(Optional.of(usuarioAdmin));

        MotoristaInvalidoException exception = assertThrows(
                MotoristaInvalidoException.class,
                () -> service.salvarOuAtualizarAssinatura(2, 2, idempotencyKey, metadados, VALID_PNG, false)
        );

        assertTrue(exception.getMessage().contains("não possui o perfil de motorista"));
        verify(assinaturaRepository, never()).save(any());
    }

    @Test
    @DisplayName("salvarOuAtualizarAssinatura: lança AssinaturaFormatoInvalidoException quando metadados ou modalidade forem nulos")
    void salvarOuAtualizarAssinatura_quandoMetadadosOuModalidadeNulos_deveLancarExcecao() {
        UUID idempotencyKey = UUID.randomUUID();
        UsuarioEntity motorista = new UsuarioEntity();
        motorista.setId(1);
        motorista.setTipo(TipoUsuario.motorista);

        when(assinaturaRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(usuarioRepository.findById(1)).thenReturn(Optional.of(motorista));

        // Metadados nulos
        assertThrows(
                AssinaturaFormatoInvalidoException.class,
                () -> service.salvarOuAtualizarAssinatura(1, 1, idempotencyKey, null, VALID_PNG, true)
        );

        // Modalidade nula dentro dos metadados
        AssinaturaMetadadosRequest semModalidade = new AssinaturaMetadadosRequest(null, "Fulano");
        assertThrows(
                AssinaturaFormatoInvalidoException.class,
                () -> service.salvarOuAtualizarAssinatura(1, 1, idempotencyKey, semModalidade, VALID_PNG, true)
        );

        verify(assinaturaRepository, never()).save(any());
    }

    @Test
    @DisplayName("salvarOuAtualizarAssinatura: primeiro upload cria nova entidade com versao=0 e ativa=true")
    void salvarOuAtualizarAssinatura_primeiroUpload_deveCriarComVersaoZeroEAtiva() {
        UUID idempotencyKey = UUID.randomUUID();
        AssinaturaMetadadosRequest metadados = new AssinaturaMetadadosRequest(ModalidadeAssinaturaMotorista.NOME_DIGITADO, "João Silva");

        UsuarioEntity motorista = new UsuarioEntity();
        motorista.setId(5);
        motorista.setTipo(TipoUsuario.motorista);

        when(assinaturaRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(usuarioRepository.findById(5)).thenReturn(Optional.of(motorista));
        when(assinaturaRepository.findByMotoristaIdAndAtivaTrue(5)).thenReturn(Optional.empty());
        when(assinaturaRepository.save(any(AssinaturaMotoristaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssinaturaMotoristaResponse response = service.salvarOuAtualizarAssinatura(5, 5, idempotencyKey, metadados, VALID_PNG, true);

        assertNotNull(response);
        assertEquals(5, response.usuarioId());
        assertEquals("NOME_DIGITADO", response.modalidade());
        assertEquals(0L, response.versao());
        assertEquals("/api/v1/usuarios/me/assinatura/conteudo", response.conteudoUrl());

        ArgumentCaptor<AssinaturaMotoristaEntity> captor = ArgumentCaptor.forClass(AssinaturaMotoristaEntity.class);
        verify(assinaturaRepository, times(1)).save(captor.capture());

        AssinaturaMotoristaEntity salva = captor.getValue();
        assertEquals(5, salva.getMotoristaId());
        assertEquals("NOME_DIGITADO", salva.getModalidade());
        assertEquals("João Silva", salva.getTextoOrigem());
        assertEquals("image/png", salva.getMimeType());
        assertEquals(0L, salva.getVersao());
        assertTrue(salva.getAtiva());
        assertEquals(idempotencyKey, salva.getIdempotencyKey());
        assertArrayEquals(VALID_PNG, salva.getConteudo());
        assertNotNull(salva.getSha256());
    }

    @Test
    @DisplayName("salvarOuAtualizarAssinatura: substituição desativa assinatura anterior e cria nova com versao=anterior.versao+1")
    void salvarOuAtualizarAssinatura_substituicao_deveDesativarAnteriorECriarNovaVersao() {
        UUID idempotencyKey = UUID.randomUUID();
        AssinaturaMetadadosRequest metadados = new AssinaturaMetadadosRequest(ModalidadeAssinaturaMotorista.DESENHO, null);

        UsuarioEntity motorista = new UsuarioEntity();
        motorista.setId(7);
        motorista.setTipo(TipoUsuario.motorista);

        AssinaturaMotoristaEntity anterior = new AssinaturaMotoristaEntity();
        anterior.setId(UUID.randomUUID());
        anterior.setMotoristaId(7);
        anterior.setVersao(2L);
        anterior.setAtiva(true);

        when(assinaturaRepository.findByIdempotencyKey(idempotencyKey)).thenReturn(Optional.empty());
        when(usuarioRepository.findById(7)).thenReturn(Optional.of(motorista));
        when(assinaturaRepository.findByMotoristaIdAndAtivaTrue(7)).thenReturn(Optional.of(anterior));
        when(assinaturaRepository.save(any(AssinaturaMotoristaEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AssinaturaMotoristaResponse response = service.salvarOuAtualizarAssinatura(7, 1, idempotencyKey, metadados, VALID_PNG, false);

        assertNotNull(response);
        assertEquals(7, response.usuarioId());
        assertEquals("DESENHO", response.modalidade());
        assertEquals(3L, response.versao());
        assertEquals("/api/v1/usuarios/7/assinatura/conteudo", response.conteudoUrl());

        ArgumentCaptor<AssinaturaMotoristaEntity> captor = ArgumentCaptor.forClass(AssinaturaMotoristaEntity.class);
        verify(assinaturaRepository, times(2)).save(captor.capture());

        // Primeira chamada: salvando anterior desativada
        AssinaturaMotoristaEntity anteriorSalva = captor.getAllValues().get(0);
        assertEquals(anterior.getId(), anteriorSalva.getId());
        assertFalse(anteriorSalva.getAtiva());

        // Segunda chamada: salvando nova ativa
        AssinaturaMotoristaEntity novaSalva = captor.getAllValues().get(1);
        assertEquals(7, novaSalva.getMotoristaId());
        assertEquals(1, novaSalva.getCriadoPor());
        assertEquals(3L, novaSalva.getVersao());
        assertTrue(novaSalva.getAtiva());
    }

    @Test
    @DisplayName("buscarAssinaturaAtiva: retorna resposta quando motorista e assinatura ativa existem")
    void buscarAssinaturaAtiva_quandoExiste_deveRetornarResposta() {
        AssinaturaMotoristaEntity entity = new AssinaturaMotoristaEntity();
        entity.setId(UUID.randomUUID());
        entity.setMotoristaId(10);
        entity.setModalidade("DESENHO");
        entity.setMimeType("image/png");
        entity.setTamanhoBytes(16L);
        entity.setSha256("hash123");
        entity.setAtiva(true);
        entity.setCriadoPor(10);
        entity.setCriadoEm(Instant.now());
        entity.setAtualizadoEm(Instant.now());
        entity.setVersao(1L);

        when(usuarioRepository.existsById(10)).thenReturn(true);
        when(assinaturaRepository.findByMotoristaIdAndAtivaTrue(10)).thenReturn(Optional.of(entity));

        AssinaturaMotoristaResponse response = service.buscarAssinaturaAtiva(10, true);

        assertNotNull(response);
        assertEquals(entity.getId(), response.id());
        assertEquals(10, response.usuarioId());
        assertEquals(1L, response.versao());
        assertEquals("/api/v1/usuarios/me/assinatura/conteudo", response.conteudoUrl());
    }

    @Test
    @DisplayName("buscarAssinaturaAtiva: lança AssinaturaNaoEncontradaException quando motorista não existe")
    void buscarAssinaturaAtiva_quandoMotoristaNaoExiste_deveLancarExcecao() {
        when(usuarioRepository.existsById(99)).thenReturn(false);

        AssinaturaNaoEncontradaException ex = assertThrows(
                AssinaturaNaoEncontradaException.class,
                () -> service.buscarAssinaturaAtiva(99, false)
        );

        assertTrue(ex.getMessage().contains("Motorista não encontrado com id: 99"));
        verify(assinaturaRepository, never()).findByMotoristaIdAndAtivaTrue(any());
    }

    @Test
    @DisplayName("buscarAssinaturaAtiva: lança AssinaturaNaoEncontradaException quando não há assinatura cadastrada")
    void buscarAssinaturaAtiva_quandoSemAssinaturaAtiva_deveLancarExcecao() {
        when(usuarioRepository.existsById(10)).thenReturn(true);
        when(assinaturaRepository.findByMotoristaIdAndAtivaTrue(10)).thenReturn(Optional.empty());

        AssinaturaNaoEncontradaException ex = assertThrows(
                AssinaturaNaoEncontradaException.class,
                () -> service.buscarAssinaturaAtiva(10, false)
        );

        assertTrue(ex.getMessage().contains("Nenhuma assinatura fixa cadastrada para o motorista 10"));
    }

    @Test
    @DisplayName("buscarConteudoAssinaturaAtiva: retorna bytes da imagem quando ativa existe")
    void buscarConteudoAssinaturaAtiva_quandoExiste_deveRetornarBytes() {
        AssinaturaMotoristaEntity entity = new AssinaturaMotoristaEntity();
        entity.setId(UUID.randomUUID());
        entity.setMotoristaId(10);
        entity.setConteudo(VALID_PNG);
        entity.setAtiva(true);

        when(usuarioRepository.existsById(10)).thenReturn(true);
        when(assinaturaRepository.findByMotoristaIdAndAtivaTrue(10)).thenReturn(Optional.of(entity));

        byte[] conteudo = service.buscarConteudoAssinaturaAtiva(10);

        assertNotNull(conteudo);
        assertArrayEquals(VALID_PNG, conteudo);
    }

    @Test
    @DisplayName("buscarConteudoAssinaturaAtiva: lança AssinaturaNaoEncontradaException quando motorista não existe ou sem assinatura")
    void buscarConteudoAssinaturaAtiva_cenariosDeExcecao() {
        when(usuarioRepository.existsById(99)).thenReturn(false);

        assertThrows(
                AssinaturaNaoEncontradaException.class,
                () -> service.buscarConteudoAssinaturaAtiva(99)
        );

        when(usuarioRepository.existsById(10)).thenReturn(true);
        when(assinaturaRepository.findByMotoristaIdAndAtivaTrue(10)).thenReturn(Optional.empty());

        assertThrows(
                AssinaturaNaoEncontradaException.class,
                () -> service.buscarConteudoAssinaturaAtiva(10)
        );
    }

    @Test
    @DisplayName("buscarAtivaPorMotoristaId e buscarPorId: delegam corretamente para o repositório")
    void buscarAtivaPorMotoristaIdEBuscarPorId_deveDelegarAoRepositorio() {
        UUID id = UUID.randomUUID();
        AssinaturaMotoristaEntity entity = new AssinaturaMotoristaEntity();
        entity.setId(id);
        entity.setMotoristaId(5);

        when(assinaturaRepository.findByMotoristaIdAndAtivaTrue(5)).thenReturn(Optional.of(entity));
        when(assinaturaRepository.findById(id)).thenReturn(Optional.of(entity));

        Optional<AssinaturaMotoristaEntity> ativa = service.buscarAtivaPorMotoristaId(5);
        Optional<AssinaturaMotoristaEntity> porId = service.buscarPorId(id);

        assertTrue(ativa.isPresent());
        assertEquals(id, ativa.get().getId());
        assertTrue(porId.isPresent());
        assertEquals(id, porId.get().getId());

        verify(assinaturaRepository).findByMotoristaIdAndAtivaTrue(5);
        verify(assinaturaRepository).findById(id);
    }
}
