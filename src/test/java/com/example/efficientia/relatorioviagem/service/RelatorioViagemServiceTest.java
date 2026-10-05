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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
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

        RelatorioViagemEntity entity = new RelatorioViagemEntity();
        entity.setId(1);
        entity.setUrlAssinaturaMotorista("url-motorista");
        // apenas 1 de 4 assinaturas

        when(repository.findById(1)).thenReturn(java.util.Optional.of(entity));

        com.example.efficientia.relatorioviagem.api.AssinaturasIncompletasException ex = assertThrows(
                com.example.efficientia.relatorioviagem.api.AssinaturasIncompletasException.class,
                () -> service.finalizar(1)
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

        RelatorioViagemEntity entity = new RelatorioViagemEntity();
        entity.setId(1);
        entity.setUrlAssinaturaPecuarista("url-pecuarista");
        entity.setUrlAssinaturaMotorista("url-motorista");
        entity.setUrlAssinaturaManobrista("url-manobrista");
        entity.setUrlAssinaturaCurraleiro("url-curraleiro");

        when(repository.findById(1)).thenReturn(java.util.Optional.of(entity));
        when(repository.save(any(RelatorioViagemEntity.class))).thenAnswer(i -> i.getArgument(0));

        var response = service.finalizar(1);

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

        var response = service.criar(requestValido(), null, key);

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
}
