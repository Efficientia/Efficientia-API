package com.example.efficientia.relatorioviagem.service;

import com.example.efficientia.relatorioviagem.api.CriarRelatorioViagemRequest;
import com.example.efficientia.relatorioviagem.api.NumeroGtaDuplicadoException;
import com.example.efficientia.relatorioviagem.persistence.RelatorioViagemEntity;
import com.example.efficientia.relatorioviagem.persistence.RelatorioViagemRepository;
import com.example.efficientia.relatorioviagem.persistence.AnomaliaDesembarqueEntity;
import com.example.efficientia.relatorioviagem.persistence.AnomaliaEmbarqueEntity;
import com.example.efficientia.relatorioviagem.persistence.ParadaImprevistaEntity;
import com.example.efficientia.assinaturamotorista.persistence.AssinaturaMotoristaEntity;
import com.example.efficientia.assinaturamotorista.persistence.AssinaturaMotoristaRepository;
import com.example.efficientia.cadastrobase.persistence.UsuarioEntity;
import com.example.efficientia.cadastrobase.persistence.UsuarioRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;
import com.example.efficientia.relatorioviagem.api.AnomaliaItemDto;
import com.example.efficientia.relatorioviagem.api.ParadaImprevistaDto;
import com.example.efficientia.relatorioviagem.api.ValidacaoDiarioRotaException;
import com.example.efficientia.relatorioviagem.persistence.AnomaliaDesembarqueRepository;
import com.example.efficientia.relatorioviagem.persistence.AnomaliaEmbarqueRepository;
import com.example.efficientia.relatorioviagem.persistence.ParadaImprevistaRepository;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.ArrayList;
import java.util.Arrays;
import org.mockito.ArgumentCaptor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RelatorioViagemServiceTest {

    @Test
    void deveCriarRelatorioComDadosValidos() {
        RelatorioViagemRepository repository = mock(RelatorioViagemRepository.class);
        RelatorioViagemService service = new RelatorioViagemService(repository);
        CriarRelatorioViagemRequest request = requestValido();

        when(repository.existsByNumeroGta("GTA-1")).thenReturn(false);
        when(repository.save(any(RelatorioViagemEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.criar(request);

        assertEquals("GTA-1", response.numeroGta());
        assertEquals(200, response.kmChegadaDesembarcadouro());
        verify(repository).save(any(RelatorioViagemEntity.class));
    }

    @Test
    void deveRejeitarNumeroGtaDuplicado() {
        RelatorioViagemRepository repository = mock(RelatorioViagemRepository.class);
        RelatorioViagemService service = new RelatorioViagemService(repository);

        when(repository.existsByNumeroGta("GTA-1")).thenReturn(true);

        assertThrows(NumeroGtaDuplicadoException.class, () -> service.criar(requestValido()));
        verify(repository, never()).save(any(RelatorioViagemEntity.class));
    }

    @Test
    void deveLancarExcecaoAoFinalizarSemAssinaturasSuficientes() {
        RelatorioViagemRepository repository = mock(RelatorioViagemRepository.class);
        RelatorioViagemService service = new RelatorioViagemService(repository);

        RelatorioViagemEntity entity = relatorioCompleto();
        entity.setUrlAssinaturaMotorista("url-motorista");
        // apenas 1 de 4 assinaturas
        UUID key = UUID.randomUUID();

        when(repository.findById(1)).thenReturn(java.util.Optional.of(entity));
        when(repository.findByIdempotencyKey(key)).thenReturn(java.util.Optional.empty());

        com.example.efficientia.relatorioviagem.api.AssinaturasIncompletasException ex = assertThrows(
                com.example.efficientia.relatorioviagem.api.AssinaturasIncompletasException.class,
                () -> service.finalizar(1, key)
        );

        assertEquals(1, ex.getRelatorioId());
        assertEquals(4, ex.getQtdObrigatoria());
        assertEquals(1, ex.getQtdColetadas());
        assertEquals(3, ex.getPapeisFaltantes().size());
    }

    @Test
    void deveFinalizarComSucessoQuandoPossuiTodasAssinaturas() {
        RelatorioViagemRepository repository = mock(RelatorioViagemRepository.class);
        RelatorioViagemService service = new RelatorioViagemService(repository);

        RelatorioViagemEntity entity = relatorioCompleto();
        entity.setUrlAssinaturaPecuarista("url-pecuarista");
        entity.setUrlAssinaturaMotorista("url-motorista");
        entity.setUrlAssinaturaManobrista("url-manobrista");
        entity.setUrlAssinaturaCurraleiro("url-curraleiro");
        UUID key = UUID.randomUUID();

        when(repository.findById(1)).thenReturn(java.util.Optional.of(entity));
        when(repository.findByIdempotencyKey(key)).thenReturn(java.util.Optional.empty());
        when(repository.save(any(RelatorioViagemEntity.class))).thenAnswer(i -> i.getArgument(0));

        var response = service.finalizar(1, key);

        assertEquals("aprovado", response.status());
        assertEquals(4, response.totalAssinaturasColetadas());
        assertEquals(true, response.assinaturasCompletas());
    }

    @Test
    void deveRegistrarAssinaturasProgressivamente() {
        RelatorioViagemRepository repository = mock(RelatorioViagemRepository.class);
        RelatorioViagemService service = new RelatorioViagemService(repository);

        RelatorioViagemEntity entity = new RelatorioViagemEntity();
        entity.setId(1);
        entity.setUrlAssinaturaMotorista("url-motorista");

        when(repository.findById(1)).thenReturn(java.util.Optional.of(entity));
        when(repository.save(any(RelatorioViagemEntity.class))).thenAnswer(i -> i.getArgument(0));

        var request = new com.example.efficientia.relatorioviagem.api.RegistrarAssinaturasRequest(
                "url-pecuarista-nova",
                null,
                "url-manobrista-nova",
                null,
                false
        );

        var response = service.registrarAssinaturas(1, request);

        assertEquals("url-pecuarista-nova", response.urlAssinaturaPecuarista());
        assertEquals("url-motorista", response.urlAssinaturaMotorista());
        assertEquals("url-manobrista-nova", response.urlAssinaturaManobrista());
        assertEquals(3, response.totalAssinaturasColetadas());
    }

    @Test
    void deveRejeitarHorarioSaidaAnteriorAoEmbarque() {
        RelatorioViagemRepository repository = mock(RelatorioViagemRepository.class);
        RelatorioViagemService service = new RelatorioViagemService(repository);

        var requestInvalido = new CriarRelatorioViagemRequest(
                1, null, 2, 3, 4, 5, 6, null, null,
                "GTA-INVALIDA", "NF-1",
                LocalDate.of(2026, 8, 20),
                LocalTime.of(10, 0),
                LocalTime.of(9, 0), // saída anterior ao embarque
                100,
                LocalDate.of(2026, 8, 20),
                LocalTime.of(12, 0),
                LocalTime.of(12, 30),
                200, "C1", true, 10, 0, 0, 10, 0, 0, 0,
                null, null, null, null, null, null, null, null, "pendente", null, null, null
        );

        assertThrows(ValidacaoDiarioRotaException.class, () -> service.criar(requestInvalido));
    }

    @Test
    void deveRejeitarKmChegadaInferiorAoKmSaida() {
        RelatorioViagemRepository repository = mock(RelatorioViagemRepository.class);
        RelatorioViagemService service = new RelatorioViagemService(repository);

        var requestInvalido = new CriarRelatorioViagemRequest(
                1, null, 2, 3, 4, 5, 6, null, null,
                "GTA-INVALIDA", "NF-1",
                LocalDate.of(2026, 8, 20),
                LocalTime.of(8, 0),
                LocalTime.of(8, 30),
                300, // saída 300
                LocalDate.of(2026, 8, 20),
                LocalTime.of(12, 0),
                LocalTime.of(12, 30),
                200, // chegada 200 (menor)
                "C1", true, 10, 0, 0, 10, 0, 0, 0,
                null, null, null, null, null, null, null, null, "pendente", null, null, null
        );

        assertThrows(ValidacaoDiarioRotaException.class, () -> service.criar(requestInvalido));
    }

    @Test
    void deveAceitarRascunhoParcialSemCamposDoFormulario() {
        RelatorioViagemService service = new RelatorioViagemService(mock(RelatorioViagemRepository.class));
        service.validarCoerenciaDiarioRota(requestParcial(), false);
    }

    @Test
    void deveCriarRascunhoParcialSemIdentificadoresDoUsuario() {
        RelatorioViagemRepository repository = mock(RelatorioViagemRepository.class);
        RelatorioViagemService service = new RelatorioViagemService(repository);
        when(repository.save(any(RelatorioViagemEntity.class))).thenAnswer(invocation -> {
            RelatorioViagemEntity entity = invocation.getArgument(0);
            entity.setId(21);
            return entity;
        });

        var response = service.criar(requestParcial());

        assertEquals(21, response.id());
        assertEquals("rascunho", response.status());
        verify(repository).save(any(RelatorioViagemEntity.class));
    }

    @Test
    void deveAcumularInconsistenciasDeHorarioAnimaisEEmergencia() {
        RelatorioViagemService service = new RelatorioViagemService(mock(RelatorioViagemRepository.class));
        CriarRelatorioViagemRequest request = requestComStatusEValores(
                "pendente", LocalTime.of(10, 0), LocalTime.of(9, 0),
                LocalTime.of(8, 0), 300, 0, 0, 0, 0, 2, null
        );

        ValidacaoDiarioRotaException ex = assertThrows(
                ValidacaoDiarioRotaException.class,
                () -> service.validarCoerenciaDiarioRota(request, true)
        );

        assertEquals(Map.of(
                "horarioSaidaPropriedade", "O horário de saída da propriedade deve ser posterior ou igual ao horário de embarque.",
                "horarioChegadaUnidade", "A data/horário de chegada na unidade frigorífica deve ser posterior à saída da propriedade.",
                "horarioDesembarque", "O horário de desembarque deve ser posterior ou igual ao horário de chegada na unidade.",
                "kmChegadaDesembarcadouro", "O quilômetro de chegada não pode ser inferior ao quilômetro de saída.",
                "totalAnimais", "O total de animais transportados (machos + fêmeas + marrucos) deve ser maior que zero na submissão final.",
                "motivoEmergencia", "O motivo de emergência é obrigatório quando há animais em estado de emergência."
        ), ex.getFieldErrors());
    }

    @Test
    void deveRejeitarCondicoesDosAnimaisAcimaDoTotal() {
        RelatorioViagemService service = new RelatorioViagemService(mock(RelatorioViagemRepository.class));
        CriarRelatorioViagemRequest request = requestComStatusEValores(
                "pendente", LocalTime.of(8, 0), LocalTime.of(8, 30),
                LocalTime.of(12, 30), 100, 1, 0, 0, 2, 0, null
        );

        ValidacaoDiarioRotaException ex = assertThrows(
                ValidacaoDiarioRotaException.class,
                () -> service.validarCoerenciaDiarioRota(request, true)
        );

        assertTrue(ex.getFieldErrors().containsKey("condicaoAnimais"));
    }

    @Test
    void deveRejeitarStatusFinalQuandoFaltamCamposObrigatorios() {
        RelatorioViagemRepository repository = mock(RelatorioViagemRepository.class);
        RelatorioViagemService service = new RelatorioViagemService(repository);
        RelatorioViagemEntity entity = new RelatorioViagemEntity();
        entity.setId(8);
        UUID key = UUID.randomUUID();
        when(repository.findById(8)).thenReturn(java.util.Optional.of(entity));
        when(repository.findByIdempotencyKey(key)).thenReturn(java.util.Optional.empty());

        ValidacaoDiarioRotaException ex = assertThrows(
                ValidacaoDiarioRotaException.class,
                () -> service.atualizarStatus(8, "pendente", key)
        );

        assertTrue(ex.getFieldErrors().containsKey("fazendaId"));
        assertTrue(ex.getFieldErrors().containsKey("totalAnimais"));
        verify(repository, never()).save(any(RelatorioViagemEntity.class));
    }

    @Test
    void deveRejeitarTransicaoFinalSemChaveDeIdempotencia() {
        RelatorioViagemRepository repository = mock(RelatorioViagemRepository.class);
        RelatorioViagemService service = new RelatorioViagemService(repository);
        RelatorioViagemEntity entity = relatorioCompleto();
        when(repository.findById(1)).thenReturn(java.util.Optional.of(entity));

        ValidacaoDiarioRotaException ex = assertThrows(
                ValidacaoDiarioRotaException.class,
                () -> service.atualizarStatus(1, "pendente")
        );

        assertTrue(ex.getFieldErrors().containsKey("Idempotency-Key"));
    }

    @Test
    void deveRejeitarChaveImutavelEChaveJaUsadaPorOutroRelatorio() {
        RelatorioViagemRepository repository = mock(RelatorioViagemRepository.class);
        RelatorioViagemService service = new RelatorioViagemService(repository);
        UUID original = UUID.randomUUID();
        UUID nova = UUID.randomUUID();
        RelatorioViagemEntity entity = relatorioCompleto();
        entity.setIdempotencyKey(original);
        when(repository.findById(1)).thenReturn(java.util.Optional.of(entity));
        when(repository.findByIdempotencyKey(nova)).thenReturn(java.util.Optional.empty());

        assertThrows(ValidacaoDiarioRotaException.class,
                () -> service.atualizarStatus(1, "reprovado", nova));

        RelatorioViagemEntity outro = new RelatorioViagemEntity();
        outro.setId(2);
        when(repository.findByIdempotencyKey(nova)).thenReturn(java.util.Optional.of(outro));
        entity.setIdempotencyKey(null);
        assertThrows(ValidacaoDiarioRotaException.class,
                () -> service.atualizarStatus(1, "reprovado", nova));
    }

    @Test
    void deveAtualizarStatusNaoFinalSemChaveIdempotente() {
        RelatorioViagemRepository repository = mock(RelatorioViagemRepository.class);
        RelatorioViagemService service = new RelatorioViagemService(repository);
        RelatorioViagemEntity entity = relatorioCompleto();
        when(repository.findById(1)).thenReturn(java.util.Optional.of(entity));
        when(repository.save(any(RelatorioViagemEntity.class))).thenAnswer(i -> i.getArgument(0));

        assertEquals("reprovado", service.atualizarStatus(1, " reprovado ").status());
        assertThrows(ValidacaoDiarioRotaException.class, () -> service.atualizarStatus(1, "desconhecido"));
    }

    @Test
    void deveRejeitarStatusDesconhecidoENomeDeStatusVazio() {
        RelatorioViagemService service = new RelatorioViagemService(mock(RelatorioViagemRepository.class));
        assertThrows(ValidacaoDiarioRotaException.class, () -> service.criar(requestComStatus("invalido")));
        assertThrows(IllegalArgumentException.class, () -> service.atualizarStatus(1, " "));
    }

    @Test
    void deveValidarPaginacaoEBusca() {
        RelatorioViagemRepository repository = mock(RelatorioViagemRepository.class);
        RelatorioViagemService service = new RelatorioViagemService(repository);
        when(repository.findAll(any(org.springframework.data.domain.Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(relatorioCompleto()), PageRequest.of(0, 10), 1));

        assertEquals(1, service.listar(0, 10).itens().size());
        assertThrows(IllegalArgumentException.class, () -> service.listar(-1, 10));
        assertThrows(IllegalArgumentException.class, () -> service.listar(0, 0));
        assertThrows(IllegalArgumentException.class, () -> service.listar(0, 101));
        assertThrows(com.example.efficientia.relatorioviagem.api.RelatorioViagemNotFoundException.class,
                () -> service.buscar(999));
    }

    @Test
    void deveExtrairUsuarioEmpresaEPerfilDoJwtComFallback() {
        RelatorioViagemService service = new RelatorioViagemService(mock(RelatorioViagemRepository.class));
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(" 17 ")
                .claim("usuario_id", " 17 ")
                .claim("empresa_id", 9)
                .build();
        var authentication = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_MOTORISTA")));

        assertEquals(17, service.extrairUsuarioId(authentication));
        assertEquals(9, service.extrairEmpresaId(authentication, 17));
        assertEquals(null, service.extrairUsuarioId(null));
        assertEquals(null, service.extrairEmpresaId(null, null));
    }

    @Test
    void deveCriarRelatorioComMotoristaEEmpresaVindosDoJwt() {
        RelatorioViagemRepository repository = mock(RelatorioViagemRepository.class);
        RelatorioViagemService service = new RelatorioViagemService(repository);
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("99")
                .claim("usuario_id", 17)
                .claim("empresa_id", "23")
                .build();
        var authentication = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_MOTORISTA")));
        when(repository.save(any(RelatorioViagemEntity.class))).thenAnswer(i -> {
            RelatorioViagemEntity entity = i.getArgument(0);
            entity.setId(31);
            return entity;
        });

        service.criar(requestParcial(), authentication, null);

        org.mockito.ArgumentCaptor<RelatorioViagemEntity> captor =
                org.mockito.ArgumentCaptor.forClass(RelatorioViagemEntity.class);
        verify(repository).save(captor.capture());
        assertEquals(17, captor.getValue().getMotoristaId());
        assertEquals(23, captor.getValue().getEmpresaId());
    }

    @Test
    void deveBloquearMotoristaAoAtualizarRelatorioDeOutraPessoa() {
        RelatorioViagemRepository repository = mock(RelatorioViagemRepository.class);
        RelatorioViagemService service = new RelatorioViagemService(repository);
        RelatorioViagemEntity entity = relatorioCompleto();
        entity.setMotoristaId(99);
        when(repository.findById(1)).thenReturn(java.util.Optional.of(entity));
        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("usuario_id", "17")
                .build();
        var authentication = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_MOTORISTA")));

        ValidacaoDiarioRotaException ex = assertThrows(
                ValidacaoDiarioRotaException.class,
                () -> service.atualizar(1, requestParcial(), authentication, null)
        );

        assertTrue(ex.getFieldErrors().containsKey("motoristaId"));
        verify(repository, never()).save(any(RelatorioViagemEntity.class));
    }

    @Test
    void deveRegistrarPapeisDeAssinaturaERejeitarPapelInvalido() {
        RelatorioViagemRepository repository = mock(RelatorioViagemRepository.class);
        RelatorioViagemService service = new RelatorioViagemService(repository);
        RelatorioViagemEntity entity = relatorioCompleto();
        when(repository.findById(1)).thenReturn(java.util.Optional.of(entity));
        when(repository.save(any(RelatorioViagemEntity.class))).thenAnswer(i -> i.getArgument(0));

        service.registrarAssinaturaPapel(1, "pecuarista", " url-pec ");
        service.registrarAssinaturaPapel(1, "MANOBRISTA", "url-man");
        service.registrarAssinaturaPapel(1, "CURRALEIRO", "url-cur");
        service.registrarAssinaturaPapel(1, "MOTORISTA", "url-ignorada");

        assertEquals("url-pec", entity.getUrlAssinaturaPecuarista());
        assertEquals("url-man", entity.getUrlAssinaturaManobrista());
        assertEquals("url-cur", entity.getUrlAssinaturaCurraleiro());
        assertThrows(IllegalArgumentException.class, () -> service.registrarAssinaturaPapel(1, "DESCONHECIDO", null));
        assertThrows(IllegalArgumentException.class, () -> service.registrarAssinaturaPapel(1, " ", null));
        assertThrows(IllegalArgumentException.class, () -> service.registrarAssinaturas(1, null));
    }

    @Test
    void deveSalvarParadasImprevistasEAnomaliasComSucesso() {
        RelatorioViagemRepository repository = mock(RelatorioViagemRepository.class);
        ParadaImprevistaRepository paradaRepo = mock(ParadaImprevistaRepository.class);
        AnomaliaEmbarqueRepository anomaliaEmbRepo = mock(AnomaliaEmbarqueRepository.class);
        AnomaliaDesembarqueRepository anomaliaDesembRepo = mock(AnomaliaDesembarqueRepository.class);

        RelatorioViagemService service = new RelatorioViagemService(
                repository, null, null, paradaRepo, anomaliaEmbRepo, anomaliaDesembRepo
        );

        when(repository.save(any(RelatorioViagemEntity.class))).thenAnswer(i -> {
            RelatorioViagemEntity e = i.getArgument(0);
            e.setId(10);
            return e;
        });

        var request = new CriarRelatorioViagemRequest(
                1, 20, 2, 3, 4, 5, 6, "ABC1D23", "XYZ9W87",
                "GTA-COMPLETA", "NF-10",
                LocalDate.of(2026, 8, 20),
                LocalTime.of(8, 0),
                LocalTime.of(8, 30),
                100,
                LocalDate.of(2026, 8, 20),
                LocalTime.of(12, 0),
                LocalTime.of(12, 30),
                200, "C1", true, 10, 5, 2, 17, 0, 0, 0,
                null, "Sem incidentes", null, null, null, null, null, null, "rascunho",
                List.of(new ParadaImprevistaDto(null, "Pneu furado", LocalDateTime.of(2026, 8, 20, 9, 0), LocalDateTime.of(2026, 8, 20, 9, 30))),
                List.of(new AnomaliaItemDto(null, "mancando", "boi mancando", 1)),
                List.of(new AnomaliaItemDto(null, "outros_atos_abuso", "nenhum", 0))
        );

        var response = service.criar(request);

        assertEquals("GTA-COMPLETA", response.numeroGta());
        assertEquals(17, response.totalAnimais());
        assertEquals(100, response.distanciaPercorridaKm());
        verify(paradaRepo).save(any());
        verify(anomaliaEmbRepo).save(any());
        verify(anomaliaDesembRepo).save(any());
    }

    @Test
    void deveRespeitarIdempotencyKeyRetornandoRelatorioExistente() {
        RelatorioViagemRepository repository = mock(RelatorioViagemRepository.class);
        RelatorioViagemService service = new RelatorioViagemService(repository);
        UUID key = UUID.randomUUID();

        RelatorioViagemEntity existente = new RelatorioViagemEntity();
        existente.setId(55);
        existente.setNumeroGta("GTA-EXISTENTE");

        when(repository.findByIdempotencyKey(key)).thenReturn(java.util.Optional.of(existente));

        var response = service.criar(requestFinalValido(), null, key);

        assertEquals(55, response.id());
        assertEquals("GTA-EXISTENTE", response.numeroGta());
        verify(repository, never()).save(any());
    }

    @Test
    void deveAtualizarRelatorioPreservandoIdentidadeChaveEClaimsDaEmpresa() {
        RelatorioViagemRepository repository = mock(RelatorioViagemRepository.class);
        RelatorioViagemService service = new RelatorioViagemService(repository);
        RelatorioViagemEntity entity = relatorioCompleto();
        entity.setMotoristaId(17);
        entity.setEmpresaId(null);
        UUID idempotencyKey = UUID.randomUUID();
        entity.setIdempotencyKey(idempotencyKey);

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("99")
                .claim("usuario_id", "17")
                .claim("empresa_id", 23)
                .build();
        var authentication = new JwtAuthenticationToken(
                jwt, List.of(new SimpleGrantedAuthority("ROLE_FUNCIONARIO_FRIBOI"))
        );
        when(repository.findById(1)).thenReturn(java.util.Optional.of(entity));
        when(repository.save(any(RelatorioViagemEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.atualizar(1, requestComStatus("rascunho"), authentication, null);

        ArgumentCaptor<RelatorioViagemEntity> captor = ArgumentCaptor.forClass(RelatorioViagemEntity.class);
        verify(repository).save(captor.capture());
        assertEquals(1, response.id());
        assertEquals(17, response.motoristaId());
        assertEquals(23, response.empresaId());
        assertEquals(idempotencyKey, response.idempotencyKey());
        assertEquals("rascunho", response.status());
        assertEquals(17, captor.getValue().getMotoristaId());
        assertEquals(23, captor.getValue().getEmpresaId());
        assertEquals(idempotencyKey, captor.getValue().getIdempotencyKey());
    }

    @Test
    void deveSubstituirParadasEAnomaliasAoAtualizarIgnorandoIdsFornecidos() {
        RelatorioViagemRepository repository = mock(RelatorioViagemRepository.class);
        ParadaImprevistaRepository paradaRepository = mock(ParadaImprevistaRepository.class);
        AnomaliaEmbarqueRepository embarqueRepository = mock(AnomaliaEmbarqueRepository.class);
        AnomaliaDesembarqueRepository desembarqueRepository = mock(AnomaliaDesembarqueRepository.class);
        RelatorioViagemService service = new RelatorioViagemService(
                repository, null, null, paradaRepository, embarqueRepository, desembarqueRepository
        );
        RelatorioViagemEntity entity = relatorioCompleto();
        List<ParadaImprevistaEntity> paradasSalvas = new ArrayList<>();
        List<AnomaliaEmbarqueEntity> anomaliasEmbarqueSalvas = new ArrayList<>();
        List<AnomaliaDesembarqueEntity> anomaliasDesembarqueSalvas = new ArrayList<>();
        when(repository.findById(1)).thenReturn(java.util.Optional.of(entity));
        when(repository.save(any(RelatorioViagemEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(paradaRepository.save(any(ParadaImprevistaEntity.class))).thenAnswer(invocation -> {
            ParadaImprevistaEntity saved = invocation.getArgument(0);
            paradasSalvas.add(saved);
            return saved;
        });
        when(embarqueRepository.save(any(AnomaliaEmbarqueEntity.class))).thenAnswer(invocation -> {
            AnomaliaEmbarqueEntity saved = invocation.getArgument(0);
            anomaliasEmbarqueSalvas.add(saved);
            return saved;
        });
        when(desembarqueRepository.save(any(AnomaliaDesembarqueEntity.class))).thenAnswer(invocation -> {
            AnomaliaDesembarqueEntity saved = invocation.getArgument(0);
            anomaliasDesembarqueSalvas.add(saved);
            return saved;
        });
        when(paradaRepository.findByRelatorioId(1)).thenReturn(paradasSalvas);
        when(embarqueRepository.findByRelatorioId(1)).thenReturn(anomaliasEmbarqueSalvas);
        when(desembarqueRepository.findByRelatorioId(1)).thenReturn(anomaliasDesembarqueSalvas);

        LocalDateTime inicio = LocalDateTime.of(2026, 8, 20, 9, 0);
        LocalDateTime fim = inicio.plusMinutes(30);
        var request = requestComFilhos(
                requestComStatus("rascunho"),
                Arrays.asList(
                        new ParadaImprevistaDto(501, " Pneu furado ", inicio, fim),
                        null,
                        new ParadaImprevistaDto(502, null, inicio, fim),
                        new ParadaImprevistaDto(503, "Sem fim", inicio, null),
                        new ParadaImprevistaDto(504, "Sem início", null, fim)
                ),
                Arrays.asList(
                        new AnomaliaItemDto(601, " mancando ", "animal", 2),
                        null,
                        new AnomaliaItemDto(602, "   ", null, 1),
                        new AnomaliaItemDto(603, null, null, 1)
                ),
                Arrays.asList(
                        new AnomaliaItemDto(701, " outros_atos_abuso ", "observação", 1),
                        null,
                        new AnomaliaItemDto(702, "", null, 1),
                        new AnomaliaItemDto(703, null, null, 1)
                )
        );

        var response = service.atualizar(1, request, null, null);

        verify(paradaRepository).deleteByRelatorioId(1);
        verify(embarqueRepository).deleteByRelatorioId(1);
        verify(desembarqueRepository).deleteByRelatorioId(1);
        assertEquals(1, paradasSalvas.size());
        assertEquals(1, anomaliasEmbarqueSalvas.size());
        assertEquals(1, anomaliasDesembarqueSalvas.size());
        assertNull(paradasSalvas.get(0).getId());
        assertNull(anomaliasEmbarqueSalvas.get(0).getId());
        assertNull(anomaliasDesembarqueSalvas.get(0).getId());
        assertEquals(1, paradasSalvas.get(0).getRelatorioId());
        assertEquals(1, anomaliasEmbarqueSalvas.get(0).getRelatorioId());
        assertEquals(1, anomaliasDesembarqueSalvas.get(0).getRelatorioId());
        assertEquals("Pneu furado", paradasSalvas.get(0).getMotivo());
        assertEquals("mancando", anomaliasEmbarqueSalvas.get(0).getAnomalia());
        assertEquals("outros_atos_abuso", anomaliasDesembarqueSalvas.get(0).getAnomalia());
        assertEquals(1, response.paradasImprevistas().size());
        assertNull(response.paradasImprevistas().get(0).id());
        assertEquals(1, response.anomaliasEmbarque().size());
        assertNull(response.anomaliasEmbarque().get(0).id());
        assertEquals(1, response.anomaliasDesembarque().size());
        assertNull(response.anomaliasDesembarque().get(0).id());
    }

    @Test
    void deveAtualizarParaConcluidoERegistrarDatasDeEnvioEFinalizacao() {
        RelatorioViagemRepository repository = mock(RelatorioViagemRepository.class);
        RelatorioViagemService service = new RelatorioViagemService(repository);
        RelatorioViagemEntity entity = relatorioCompleto();
        UUID idempotencyKey = UUID.randomUUID();
        LocalDateTime criadoEm = LocalDateTime.of(2026, 8, 1, 10, 0);
        entity.setCriadoEm(criadoEm);
        entity.setIdempotencyKey(idempotencyKey);
        entity.setUrlAssinaturaMotorista("assinatura-fixa-motorista");
        when(repository.findById(1)).thenReturn(java.util.Optional.of(entity));
        when(repository.findByIdempotencyKey(idempotencyKey)).thenReturn(java.util.Optional.of(entity));
        when(repository.save(any(RelatorioViagemEntity.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.atualizar(
                1, requestComStatus("concluido"), null, idempotencyKey
        );

        assertEquals(1, response.id());
        assertEquals("concluido", response.status());
        assertEquals(idempotencyKey, response.idempotencyKey());
        assertEquals(criadoEm, entity.getCriadoEm());
        assertEquals("assinatura-fixa-motorista", response.urlAssinaturaMotorista());
        assertNotNull(response.enviadoEm());
        assertNotNull(response.finalizadoEm());
        assertTrue(!response.finalizadoEm().isBefore(response.enviadoEm()));
        verify(repository).save(entity);
    }

    @Test
    void deveCobrirCriacaoFinalComStatusEChavesDeIdempotencia() {
        RelatorioViagemRepository repository = mock(RelatorioViagemRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        RelatorioViagemService service = new RelatorioViagemService(repository, usuarioRepository, null);
        var request = requestComStatus("pendente");
        UUID key = UUID.randomUUID();

        UsuarioEntity motorista = new UsuarioEntity();
        motorista.setUrlAssinaturaGeral("assinatura-fixa-motorista");
        when(usuarioRepository.findById(3)).thenReturn(java.util.Optional.of(motorista));

        assertThrows(ValidacaoDiarioRotaException.class, () -> service.criar(request, null, null));
        assertThrows(ValidacaoDiarioRotaException.class, () -> service.criar(requestComStatus("rascunho"), null, key));
        verify(repository, never()).save(any(RelatorioViagemEntity.class));

        when(repository.findByIdempotencyKey(key)).thenReturn(java.util.Optional.empty());
        when(repository.existsByNumeroGta("GTA-EXISTENTE")).thenReturn(false);
        when(repository.save(any(RelatorioViagemEntity.class))).thenAnswer(invocation -> {
            RelatorioViagemEntity saved = invocation.getArgument(0);
            saved.setId(71);
            return saved;
        });
        var response = service.criar(requestComGta(request, "GTA-EXISTENTE"), null, key);

        assertEquals("pendente", response.status());
        assertEquals(key, response.idempotencyKey());
        assertNotNull(response.enviadoEm());
        assertNull(response.finalizadoEm());
        verify(repository).existsByNumeroGta("GTA-EXISTENTE");

        for (String finalStatus : List.of("aprovado", "concluido")) {
            RelatorioViagemRepository finalRepository = mock(RelatorioViagemRepository.class);
            UsuarioRepository finalUsuarioRepository = mock(UsuarioRepository.class);
            RelatorioViagemService finalService = new RelatorioViagemService(finalRepository, finalUsuarioRepository, null);
            UUID finalKey = UUID.randomUUID();
            UsuarioEntity finalMotorista = new UsuarioEntity();
            finalMotorista.setUrlAssinaturaGeral("assinatura-fixa-motorista");
            when(finalUsuarioRepository.findById(3)).thenReturn(java.util.Optional.of(finalMotorista));
            when(finalRepository.findByIdempotencyKey(finalKey)).thenReturn(java.util.Optional.empty());
            when(finalRepository.existsByNumeroGta("GTA-1")).thenReturn(false);
            when(finalRepository.save(any(RelatorioViagemEntity.class))).thenAnswer(invocation -> {
                RelatorioViagemEntity saved = invocation.getArgument(0);
                saved.setId(72);
                return saved;
            });

            var finalResponse = finalService.criar(requestComStatus(finalStatus), null, finalKey);
            assertEquals(finalStatus, finalResponse.status());
            assertNotNull(finalResponse.enviadoEm());
            assertNotNull(finalResponse.finalizadoEm());
        }
    }

    @Test
    void deveRejeitarGtaDuplicadaAoCriarEAtualizar() {
        RelatorioViagemRepository repository = mock(RelatorioViagemRepository.class);
        RelatorioViagemService service = new RelatorioViagemService(repository);
        when(repository.existsByNumeroGta("GTA-1")).thenReturn(true);
        assertThrows(NumeroGtaDuplicadoException.class, () -> service.criar(requestValido()));

        RelatorioViagemEntity entity = relatorioCompleto();
        when(repository.findById(1)).thenReturn(java.util.Optional.of(entity));
        when(repository.existsByNumeroGta("GTA-ALTERADA")).thenReturn(true);
        assertThrows(NumeroGtaDuplicadoException.class,
                () -> service.atualizar(1, requestComGta(requestComStatus("rascunho"), "GTA-ALTERADA"), null, null));
        verify(repository, never()).save(any(RelatorioViagemEntity.class));
    }

    @Test
    void deveAtualizarRascunhoPreservandoStatusQuandoCampoVemNuloOuEmBranco() {
        RelatorioViagemRepository repository = mock(RelatorioViagemRepository.class);
        RelatorioViagemService service = new RelatorioViagemService(repository);
        RelatorioViagemEntity entity = relatorioCompleto();
        entity.setNumeroGta("gta-1");
        entity.setStatus("rascunho");
        when(repository.findById(1)).thenReturn(java.util.Optional.of(entity));
        when(repository.save(any(RelatorioViagemEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var statusNulo = service.atualizar(1,
                requestComGta(requestComStatus(null), "GTA-1"), null, null);
        var statusEmBranco = service.atualizar(1,
                requestComGta(requestComStatus("   "), "GTA-1"), null, null);

        assertEquals("rascunho", statusNulo.status());
        assertEquals("rascunho", statusEmBranco.status());
        verify(repository, never()).existsByNumeroGta("GTA-1");
        verify(repository, org.mockito.Mockito.times(2)).save(entity);
    }

    @Test
    void deveRetornarTodosOsErrosObrigatoriosNaSubmissaoFinalParcial() {
        RelatorioViagemService service = new RelatorioViagemService(mock(RelatorioViagemRepository.class));

        ValidacaoDiarioRotaException ex = assertThrows(ValidacaoDiarioRotaException.class,
                () -> service.validarCoerenciaDiarioRota(requestParcial(), true));

        assertTrue(ex.getFieldErrors().containsKey("fazendaId"));
        assertTrue(ex.getFieldErrors().containsKey("numeroGta"));
        assertTrue(ex.getFieldErrors().containsKey("horarioDesembarque"));
        assertTrue(ex.getFieldErrors().containsKey("quantidadeEmergencia"));
        assertTrue(ex.getFieldErrors().containsKey("totalAnimais"));
    }

    @Test
    void deveInformarPapeisComAssinaturaEmBrancoComoPendentes() {
        RelatorioViagemService service = new RelatorioViagemService(mock(RelatorioViagemRepository.class));
        RelatorioViagemEntity entity = relatorioCompleto();
        entity.setUrlAssinaturaPecuarista(" ");
        entity.setUrlAssinaturaMotorista("\t");
        entity.setUrlAssinaturaManobrista("");
        entity.setUrlAssinaturaCurraleiro("  ");

        var ex = assertThrows(com.example.efficientia.relatorioviagem.api.AssinaturasIncompletasException.class,
                () -> service.validarAssinaturasObrigatorias(entity));

        assertEquals(List.of("Pecuarista", "Motorista", "Manobrista", "Curraleiro"), ex.getPapeisFaltantes());
    }

    @Test
    void deveCobrirTransicoesDeStatusPendenteEFinalizacaoComChaveVinculada() {
        RelatorioViagemRepository repository = mock(RelatorioViagemRepository.class);
        RelatorioViagemService service = new RelatorioViagemService(repository);
        RelatorioViagemEntity entity = relatorioCompleto();
        entity.setUrlAssinaturaPecuarista("pec");
        entity.setUrlAssinaturaMotorista("mot");
        entity.setUrlAssinaturaManobrista("man");
        entity.setUrlAssinaturaCurraleiro("cur");
        UUID key = UUID.randomUUID();
        entity.setIdempotencyKey(key);
        when(repository.findById(1)).thenReturn(java.util.Optional.of(entity));
        when(repository.findByIdempotencyKey(key)).thenReturn(java.util.Optional.of(entity));
        when(repository.save(any(RelatorioViagemEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        var enviado = service.enviarParaAnalise(1, key);
        assertEquals("pendente", enviado.status());
        assertNotNull(enviado.enviadoEm());
        var reenviado = service.atualizarStatus(1, " pendente ", key);
        assertEquals(enviado.enviadoEm(), reenviado.enviadoEm());
        var aprovado = service.atualizarStatus(1, "aprovado", key);
        assertNotNull(aprovado.finalizadoEm());
        assertEquals("aprovado", aprovado.status());
        verify(repository, org.mockito.Mockito.times(3)).save(entity);
    }

    @Test
    void deveCobrirFallbacksDoJwtEAutenticacaoSemClaimsValidas() {
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        RelatorioViagemService service = new RelatorioViagemService(
                mock(RelatorioViagemRepository.class), usuarioRepository, null
        );
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setEmpresaId(44);
        when(usuarioRepository.findById(23)).thenReturn(java.util.Optional.of(usuario));

        Jwt jwt = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject(" 23 ")
                .claim("usuario_id", "nao-numero")
                .claim("empresa_id", "tambem-nao-numero")
                .build();
        var authentication = new JwtAuthenticationToken(jwt, List.of(new SimpleGrantedAuthority("ROLE_FUNCIONARIO")));
        assertEquals(23, service.extrairUsuarioId(authentication));
        assertEquals(44, service.extrairEmpresaId(authentication, 23));

        var unsupportedAuthentication = new UsernamePasswordAuthenticationToken("usuario", "senha");
        assertNull(service.extrairUsuarioId(unsupportedAuthentication));
        assertEquals(44, service.extrairEmpresaId(unsupportedAuthentication, 23));
        assertNull(service.extrairEmpresaId(unsupportedAuthentication, null));

        Jwt semIds = Jwt.withTokenValue("token")
                .header("alg", "none")
                .subject("id-invalido")
                .claim("usuario_id", " ")
                .build();
        assertNull(service.extrairUsuarioId(new JwtAuthenticationToken(semIds)));
    }

    @Test
    void deveImpedirCriacaoAutenticadaSemIdEAtualizacaoDeRelatorioAprovado() {
        RelatorioViagemRepository repository = mock(RelatorioViagemRepository.class);
        RelatorioViagemService service = new RelatorioViagemService(repository);
        var jwtSemId = Jwt.withTokenValue("token").header("alg", "none").claim("irrelevante", "sem-id").build();
        var semId = new JwtAuthenticationToken(jwtSemId);
        ValidacaoDiarioRotaException ex = assertThrows(ValidacaoDiarioRotaException.class,
                () -> service.criar(requestParcial(), semId, null));
        assertTrue(ex.getFieldErrors().containsKey("motoristaId"));

        RelatorioViagemEntity aprovado = relatorioCompleto();
        aprovado.setStatus("aprovado");
        when(repository.findById(1)).thenReturn(java.util.Optional.of(aprovado));
        assertThrows(IllegalArgumentException.class,
                () -> service.atualizar(1, requestComStatus("rascunho"), null, null));
        verify(repository, never()).save(any(RelatorioViagemEntity.class));
    }

    @Test
    void deveUsarAssinaturaFixaDaContaOuFallbackDoRepositorio() {
        RelatorioViagemRepository repository = mock(RelatorioViagemRepository.class);
        UsuarioRepository usuarioRepository = mock(UsuarioRepository.class);
        AssinaturaMotoristaRepository assinaturaRepository = mock(AssinaturaMotoristaRepository.class);
        RelatorioViagemEntity entity = relatorioCompleto();
        entity.setUrlAssinaturaMotorista(null);
        when(repository.findById(1)).thenReturn(java.util.Optional.of(entity));
        when(repository.save(any(RelatorioViagemEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        UsuarioEntity usuario = new UsuarioEntity();
        usuario.setUrlAssinaturaGeral("   ");
        when(usuarioRepository.findById(3)).thenReturn(java.util.Optional.of(usuario));
        AssinaturaMotoristaEntity assinatura = new AssinaturaMotoristaEntity();
        when(assinaturaRepository.findByMotoristaIdAndAtivaTrue(3)).thenReturn(java.util.Optional.of(assinatura));

        var response = new RelatorioViagemService(repository, usuarioRepository, assinaturaRepository)
                .registrarAssinaturas(1, new com.example.efficientia.relatorioviagem.api.RegistrarAssinaturasRequest(
                        " ", null, " ", " ", false
                ));
        assertEquals("/api/v1/usuarios/3/assinatura/conteudo", response.urlAssinaturaMotorista());

        RelatorioViagemEntity withoutDriverId = relatorioCompleto();
        withoutDriverId.setId(3);
        withoutDriverId.setMotoristaId(null);
        withoutDriverId.setUrlAssinaturaMotorista(null);
        when(repository.findById(3)).thenReturn(java.util.Optional.of(withoutDriverId));
        var noSignature = new RelatorioViagemService(repository, null, null)
                .registrarAssinaturas(3, new com.example.efficientia.relatorioviagem.api.RegistrarAssinaturasRequest(
                        null, null, null, null, false
                ));
        assertNull(noSignature.urlAssinaturaMotorista());

        RelatorioViagemEntity entityWithFixedSignature = relatorioCompleto();
        entityWithFixedSignature.setUrlAssinaturaMotorista(null);
        when(repository.findById(2)).thenReturn(java.util.Optional.of(entityWithFixedSignature));
        UsuarioEntity usuarioWithSignature = new UsuarioEntity();
        usuarioWithSignature.setUrlAssinaturaGeral("assinatura-conta");
        when(usuarioRepository.findById(3)).thenReturn(java.util.Optional.of(usuarioWithSignature));
        var fromAccount = new RelatorioViagemService(repository, usuarioRepository, assinaturaRepository)
                .registrarAssinaturaPapel(2, "MOTORISTA", "cliente-nao-pode-substituir");
        assertEquals("assinatura-conta", fromAccount.urlAssinaturaMotorista());
        verify(assinaturaRepository).findByMotoristaIdAndAtivaTrue(3);
    }

    @Test
    void deveCobrirGuardasDeCoerenciaComCamposTemporaisOpcionais() {
        RelatorioViagemService service = new RelatorioViagemService(mock(RelatorioViagemRepository.class));
        CriarRelatorioViagemRequest base = requestComStatus("rascunho");
        LocalDate data = LocalDate.of(2026, 8, 20);
        LocalTime embarque = LocalTime.of(8, 0);
        LocalTime saida = LocalTime.of(8, 30);
        LocalTime chegada = LocalTime.of(12, 0);
        LocalTime desembarque = LocalTime.of(12, 30);

        List<CriarRelatorioViagemRequest> requests = List.of(
                requestComTempos(base, null, embarque, saida, data, chegada, desembarque, 100, 200),
                requestComTempos(base, data, null, saida, data, chegada, desembarque, 100, 200),
                requestComTempos(base, data, embarque, null, data, chegada, desembarque, 100, 200),
                requestComTempos(base, null, embarque, saida, data, chegada, desembarque, null, 200),
                requestComTempos(base, data, embarque, saida, null, chegada, desembarque, 100, null),
                requestComTempos(base, data, embarque, saida, data, null, desembarque, 100, 200),
                requestComTempos(base, data, embarque, saida, data, chegada, null, 100, 200)
        );
        requests.forEach(request -> service.validarCoerenciaDiarioRota(request, false));
    }

    @Test
    void deveAceitarEmergenciaComMotivoEValidarIgualdadeDeHorarioEQuilometragem() {
        RelatorioViagemService service = new RelatorioViagemService(mock(RelatorioViagemRepository.class));
        CriarRelatorioViagemRequest request = requestComValores(
                "pendente", LocalTime.of(8, 0), LocalTime.of(8, 0), LocalTime.of(12, 0),
                100, 10, 8, 0, 17, 0, 0, 1, "Animal ferido"
        );
        service.validarCoerenciaDiarioRota(request, true);
    }

    @Test
    void deveValidarCamposCompletosERejeitarQuilometragemInconsistenteNaMudancaDeStatus() {
        RelatorioViagemRepository repository = mock(RelatorioViagemRepository.class);
        RelatorioViagemService service = new RelatorioViagemService(repository);
        RelatorioViagemEntity entity = relatorioCompleto();
        entity.setUrlAssinaturaPecuarista("pec");
        entity.setUrlAssinaturaMotorista("mot");
        entity.setUrlAssinaturaManobrista("man");
        entity.setUrlAssinaturaCurraleiro("cur");
        UUID key = UUID.randomUUID();
        when(repository.findById(1)).thenReturn(java.util.Optional.of(entity));
        when(repository.findByIdempotencyKey(key)).thenReturn(java.util.Optional.empty());
        when(repository.save(any(RelatorioViagemEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertEquals("pendente", service.atualizarStatus(1, "pendente", key).status());

        RelatorioViagemEntity inconsistent = relatorioCompleto();
        inconsistent.setUrlAssinaturaPecuarista("pec");
        inconsistent.setUrlAssinaturaMotorista("mot");
        inconsistent.setUrlAssinaturaManobrista("man");
        inconsistent.setUrlAssinaturaCurraleiro("cur");
        inconsistent.setKmChegadaDesembarcadouro(90);
        when(repository.findById(2)).thenReturn(java.util.Optional.of(inconsistent));
        ValidacaoDiarioRotaException ex = assertThrows(ValidacaoDiarioRotaException.class,
                () -> service.atualizarStatus(2, "pendente", UUID.randomUUID()));
        assertTrue(ex.getFieldErrors().containsKey("kmChegadaDesembarcadouro"));
    }

    private CriarRelatorioViagemRequest requestComGta(CriarRelatorioViagemRequest base, String gta) {
        return new CriarRelatorioViagemRequest(
                base.fazendaId(), base.unidadeFrigorificaId(), base.motoristaId(), base.manobristaId(),
                base.curraleiroId(), base.cavaloId(), base.carretaId(), base.placaCavalo(), base.placaCarreta(),
                gta, base.numeroNotaFiscal(), base.dataEmbarque(), base.horarioEmbarque(),
                base.horarioSaidaPropriedade(), base.kmSaidaEmbarcadouro(), base.dataChegadaUnidade(),
                base.horarioChegadaUnidade(), base.horarioDesembarque(), base.kmChegadaDesembarcadouro(),
                base.numeroCurral(), base.sireneReFuncionou(), base.quantidadeMachos(), base.quantidadeFemeas(),
                base.quantidadeMarrucos(), base.quantidadeEmPe(), base.quantidadeDeitado(), base.quantidadeMorto(),
                base.quantidadeEmergencia(), base.motivoEmergencia(), base.comentarios(),
                base.urlAssinaturaPecuarista(), base.urlAssinaturaMotorista(), base.urlAssinaturaManobrista(),
                base.urlAssinaturaCurraleiro(), base.capacidadeCargaUtilizada(), base.urlLaudoMortalidade(),
                base.status(), base.paradasImprevistas(), base.anomaliasEmbarque(), base.anomaliasDesembarque()
        );
    }

    private CriarRelatorioViagemRequest requestComTempos(
            CriarRelatorioViagemRequest base, LocalDate dataEmbarque,
            LocalTime horarioEmbarque, LocalTime horarioSaida,
            LocalDate dataChegada, LocalTime horarioChegada,
            LocalTime horarioDesembarque, Integer kmSaida, Integer kmChegada
    ) {
        return new CriarRelatorioViagemRequest(
                base.fazendaId(), base.unidadeFrigorificaId(), base.motoristaId(), base.manobristaId(),
                base.curraleiroId(), base.cavaloId(), base.carretaId(), base.placaCavalo(), base.placaCarreta(),
                base.numeroGta(), base.numeroNotaFiscal(), dataEmbarque, horarioEmbarque, horarioSaida,
                kmSaida, dataChegada, horarioChegada, horarioDesembarque, kmChegada, base.numeroCurral(),
                base.sireneReFuncionou(), base.quantidadeMachos(), base.quantidadeFemeas(), base.quantidadeMarrucos(),
                base.quantidadeEmPe(), base.quantidadeDeitado(), base.quantidadeMorto(), base.quantidadeEmergencia(),
                base.motivoEmergencia(), base.comentarios(), base.urlAssinaturaPecuarista(),
                base.urlAssinaturaMotorista(), base.urlAssinaturaManobrista(), base.urlAssinaturaCurraleiro(),
                base.capacidadeCargaUtilizada(), base.urlLaudoMortalidade(), base.status(),
                base.paradasImprevistas(), base.anomaliasEmbarque(), base.anomaliasDesembarque()
        );
    }

    private CriarRelatorioViagemRequest requestComValores(
            String status, LocalTime horarioEmbarque, LocalTime horarioSaida,
            LocalTime horarioDesembarque, int kmSaida, int machos, int femeas,
            int marrucos, int emPe, int deitado, int morto, int emergencia, String motivo
    ) {
        return new CriarRelatorioViagemRequest(
                1, 2, 3, 4, 5, 6, 7, null, null, "GTA-1", "NF-1",
                LocalDate.of(2026, 8, 20), horarioEmbarque, horarioSaida, kmSaida,
                LocalDate.of(2026, 8, 20), LocalTime.of(12, 0), horarioDesembarque, 200,
                "C1", true, machos, femeas, marrucos, emPe, deitado, morto, emergencia,
                motivo, null, "pec", "mot", "man", "cur", null, null, status, null, null, null
        );
    }

    private CriarRelatorioViagemRequest requestValido() {
        return new CriarRelatorioViagemRequest(
                1, 2, 3, 4, 5, 6,
                " GTA-1 ", "NF-1",
                LocalDate.of(2026, 8, 20),
                LocalTime.of(8, 0),
                LocalTime.of(8, 30),
                100,
                LocalDate.of(2026, 8, 20),
                LocalTime.of(12, 0),
                LocalTime.of(12, 30),
                200,
                "C1",
                true,
                10, 8, 0, 18, 0, 0, 0,
                null,
                "",
                "assinatura-pecuarista",
                "assinatura-motorista",
                "assinatura-manobrista",
                "assinatura-curraleiro"
        );
    }

    private CriarRelatorioViagemRequest requestFinalValido() {
        return new CriarRelatorioViagemRequest(
                1, 2, 3, 4, 5, 6, 7, null, null,
                "GTA-EXISTENTE", "NF-1",
                LocalDate.of(2026, 8, 20), LocalTime.of(8, 0), LocalTime.of(8, 30), 100,
                LocalDate.of(2026, 8, 20), LocalTime.of(12, 0), LocalTime.of(12, 30), 200,
                "C1", true, 10, 8, 0, 18, 0, 0, 0,
                null, null, "pecuarista", "motorista", "manobrista", "curraleiro",
                null, null, "pendente", null, null, null
        );
    }

    private CriarRelatorioViagemRequest requestParcial() {
        return new CriarRelatorioViagemRequest(
                null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null
        );
    }

    private CriarRelatorioViagemRequest requestComStatus(String status) {
        CriarRelatorioViagemRequest valido = requestFinalValido();
        return new CriarRelatorioViagemRequest(
                valido.fazendaId(), valido.unidadeFrigorificaId(), valido.motoristaId(), valido.manobristaId(),
                valido.curraleiroId(), valido.cavaloId(), valido.carretaId(), valido.placaCavalo(), valido.placaCarreta(),
                valido.numeroGta(), valido.numeroNotaFiscal(), valido.dataEmbarque(), valido.horarioEmbarque(),
                valido.horarioSaidaPropriedade(), valido.kmSaidaEmbarcadouro(), valido.dataChegadaUnidade(),
                valido.horarioChegadaUnidade(), valido.horarioDesembarque(), valido.kmChegadaDesembarcadouro(),
                valido.numeroCurral(), valido.sireneReFuncionou(), valido.quantidadeMachos(), valido.quantidadeFemeas(),
                valido.quantidadeMarrucos(), valido.quantidadeEmPe(), valido.quantidadeDeitado(), valido.quantidadeMorto(),
                valido.quantidadeEmergencia(), valido.motivoEmergencia(), valido.comentarios(),
                valido.urlAssinaturaPecuarista(), valido.urlAssinaturaMotorista(), valido.urlAssinaturaManobrista(),
                valido.urlAssinaturaCurraleiro(), valido.capacidadeCargaUtilizada(), valido.urlLaudoMortalidade(),
                status, valido.paradasImprevistas(), valido.anomaliasEmbarque(), valido.anomaliasDesembarque()
        );
    }

    private CriarRelatorioViagemRequest requestComFilhos(
            CriarRelatorioViagemRequest request,
            List<ParadaImprevistaDto> paradas,
            List<AnomaliaItemDto> anomaliasEmbarque,
            List<AnomaliaItemDto> anomaliasDesembarque
    ) {
        return new CriarRelatorioViagemRequest(
                request.fazendaId(), request.unidadeFrigorificaId(), request.motoristaId(), request.manobristaId(),
                request.curraleiroId(), request.cavaloId(), request.carretaId(), request.placaCavalo(),
                request.placaCarreta(), request.numeroGta(), request.numeroNotaFiscal(), request.dataEmbarque(),
                request.horarioEmbarque(), request.horarioSaidaPropriedade(), request.kmSaidaEmbarcadouro(),
                request.dataChegadaUnidade(), request.horarioChegadaUnidade(), request.horarioDesembarque(),
                request.kmChegadaDesembarcadouro(), request.numeroCurral(), request.sireneReFuncionou(),
                request.quantidadeMachos(), request.quantidadeFemeas(), request.quantidadeMarrucos(),
                request.quantidadeEmPe(), request.quantidadeDeitado(), request.quantidadeMorto(),
                request.quantidadeEmergencia(), request.motivoEmergencia(), request.comentarios(),
                request.urlAssinaturaPecuarista(), request.urlAssinaturaMotorista(), request.urlAssinaturaManobrista(),
                request.urlAssinaturaCurraleiro(), request.capacidadeCargaUtilizada(), request.urlLaudoMortalidade(),
                request.status(), paradas, anomaliasEmbarque, anomaliasDesembarque
        );
    }

    private CriarRelatorioViagemRequest requestComStatusEValores(
            String status, LocalTime embarque, LocalTime saida, LocalTime desembarque,
            int kmSaida, int machos, int femeas, int marrucos, int emPe, int emergencia, String motivo
    ) {
        return new CriarRelatorioViagemRequest(
                1, 2, 3, 4, 5, 6, 7, null, null, "GTA-1", "NF-1",
                LocalDate.of(2026, 8, 20), embarque, saida, kmSaida,
                LocalDate.of(2026, 8, 20), LocalTime.of(8, 30), desembarque, 100,
                "C1", true, machos, femeas, marrucos, emPe, 0, 0, emergencia,
                motivo, null, null, null, null, null, null, null, status, null, null, null
        );
    }

    private RelatorioViagemEntity relatorioCompleto() {
        RelatorioViagemEntity entity = new RelatorioViagemEntity();
        entity.setId(1);
        entity.setFazendaId(1);
        entity.setUnidadeFrigorificaId(2);
        entity.setMotoristaId(3);
        entity.setManobristaId(4);
        entity.setCurraleiroId(5);
        entity.setCavaloId(6);
        entity.setCarretaId(7);
        entity.setNumeroGta("GTA-1");
        entity.setNumeroNotaFiscal("NF-1");
        entity.setDataEmbarque(LocalDate.of(2026, 8, 20));
        entity.setHorarioEmbarque(LocalTime.of(8, 0));
        entity.setHorarioSaidaPropriedade(LocalTime.of(8, 30));
        entity.setKmSaidaEmbarcadouro(100);
        entity.setDataChegadaUnidade(LocalDate.of(2026, 8, 20));
        entity.setHorarioChegadaUnidade(LocalTime.of(12, 0));
        entity.setHorarioDesembarque(LocalTime.of(12, 30));
        entity.setKmChegadaDesembarcadouro(200);
        entity.setNumeroCurral("C1");
        entity.setSireneReFuncionou(true);
        entity.setQuantidadeMachos(10);
        entity.setQuantidadeFemeas(8);
        entity.setQuantidadeMarrucos(0);
        entity.setQuantidadeEmPe(18);
        entity.setQuantidadeDeitado(0);
        entity.setQuantidadeMorto(0);
        entity.setQuantidadeEmergencia(0);
        return entity;
    }

    @Test
    void deveSalvarAssinaturaParticipanteMultipartComSucesso() {
        RelatorioViagemRepository repo = mock(RelatorioViagemRepository.class);
        RelatorioViagemEntity rel = relatorioCompleto();
        rel.setStatus("rascunho");

        when(repo.findById(1)).thenReturn(java.util.Optional.of(rel));
        when(repo.save(any(RelatorioViagemEntity.class))).thenAnswer(i -> i.getArgument(0));

        RelatorioViagemService serv = new RelatorioViagemService(repo, null, null, null, null, null, null);

        byte[] validPng = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A, 0x00};
        org.springframework.mock.web.MockMultipartFile arquivo = new org.springframework.mock.web.MockMultipartFile(
                "arquivo", "pecuarista.png", "image/png", validPng
        );

        var response = serv.salvarAssinaturaParticipanteMultipart(
                1, "pecuarista", UUID.randomUUID(), arquivo, null, null
        );

        assertNotNull(response);
        assertEquals("/api/v1/relatorios-viagem/1/assinaturas/pecuarista/conteudo", response.urlAssinaturaPecuarista());
    }

    @Test
    void deveRejeitarSalvarAssinaturaComPapelInvalido() {
        RelatorioViagemRepository repo = mock(RelatorioViagemRepository.class);
        RelatorioViagemService serv = new RelatorioViagemService(repo);

        byte[] validPng = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        org.springframework.mock.web.MockMultipartFile arquivo = new org.springframework.mock.web.MockMultipartFile(
                "arquivo", "pecuarista.png", "image/png", validPng
        );

        assertThrows(ValidacaoDiarioRotaException.class, () ->
                serv.salvarAssinaturaParticipanteMultipart(1, "invalido", null, arquivo, null, null)
        );
    }

    @Test
    void deveRejeitarSalvarAssinaturaEmRelatorioFinalizado() {
        RelatorioViagemRepository repo = mock(RelatorioViagemRepository.class);
        RelatorioViagemEntity rel = relatorioCompleto();
        rel.setStatus("aprovado");

        when(repo.findById(1)).thenReturn(java.util.Optional.of(rel));
        RelatorioViagemService serv = new RelatorioViagemService(repo);

        byte[] validPng = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        org.springframework.mock.web.MockMultipartFile arquivo = new org.springframework.mock.web.MockMultipartFile(
                "arquivo", "pecuarista.png", "image/png", validPng
        );

        assertThrows(ValidacaoDiarioRotaException.class, () ->
                serv.salvarAssinaturaParticipanteMultipart(1, "pecuarista", null, arquivo, null, null)
        );
    }

    @Test
    void deveBuscarConteudoAssinaturaMotoristaFixa() {
        RelatorioViagemRepository repo = mock(RelatorioViagemRepository.class);
        AssinaturaMotoristaRepository assRepo = mock(AssinaturaMotoristaRepository.class);
        RelatorioViagemEntity rel = relatorioCompleto();
        rel.setUrlAssinaturaMotorista("/api/v1/usuarios/3/assinatura/conteudo");

        when(repo.findById(1)).thenReturn(java.util.Optional.of(rel));

        AssinaturaMotoristaEntity assEntity = new AssinaturaMotoristaEntity();
        byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        assEntity.setConteudo(pngBytes);
        when(assRepo.findByMotoristaIdAndAtivaTrue(3)).thenReturn(java.util.Optional.of(assEntity));

        RelatorioViagemService serv = new RelatorioViagemService(repo, null, assRepo, null, null, null, null);

        var conteudo = serv.buscarConteudoAssinaturaParticipante(1, "motorista");
        assertNotNull(conteudo);
        assertEquals("image/png", conteudo.mimeType());
        assertEquals(pngBytes.length, conteudo.tamanhoBytes());
    }
}
