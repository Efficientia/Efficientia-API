package com.example.efficientia.relatorioviagem.service;

import com.example.efficientia.relatorioviagem.api.CriarRelatorioViagemRequest;
import com.example.efficientia.relatorioviagem.api.NumeroGtaDuplicadoException;
import com.example.efficientia.relatorioviagem.persistence.RelatorioViagemEntity;
import com.example.efficientia.relatorioviagem.persistence.RelatorioViagemRepository;
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
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
