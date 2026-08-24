package com.example.efficientia.relatorioviagem.service;

import com.example.efficientia.relatorioviagem.api.CriarRelatorioViagemRequest;
import com.example.efficientia.relatorioviagem.api.NumeroGtaDuplicadoException;
import com.example.efficientia.relatorioviagem.persistence.RelatorioViagemEntity;
import com.example.efficientia.relatorioviagem.persistence.RelatorioViagemRepository;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalTime;

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
