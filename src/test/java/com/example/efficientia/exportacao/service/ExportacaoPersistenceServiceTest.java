package com.example.efficientia.exportacao.service;

import com.example.efficientia.exportacao.persistence.ExportacaoEntity;
import com.example.efficientia.exportacao.persistence.ExportacaoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExportacaoPersistenceServiceTest {

    private ExportacaoRepository repository;
    private ExportacaoPersistenceService service;

    @BeforeEach
    void setUp() {
        repository = mock(ExportacaoRepository.class);
        service = new ExportacaoPersistenceService(repository);
    }

    @Test
    void criaExportacaoComFlushEDevolveEntidadePersistida() {
        ExportacaoEntity exportacao = new ExportacaoEntity();
        exportacao.setId(UUID.randomUUID());
        when(repository.saveAndFlush(exportacao)).thenReturn(exportacao);

        ExportacaoEntity resultado = service.criar(exportacao);

        assertThat(resultado).isSameAs(exportacao);
        verify(repository).saveAndFlush(exportacao);
    }

    @Test
    void propagaFalhaDoRepositorio() {
        ExportacaoEntity exportacao = new ExportacaoEntity();
        RuntimeException falha = new IllegalStateException("banco indisponivel");
        when(repository.saveAndFlush(exportacao)).thenThrow(falha);

        assertThatThrownBy(() -> service.criar(exportacao)).isSameAs(falha);
        verify(repository).saveAndFlush(exportacao);
    }

    @Test
    void executaCriacaoEmTransacaoNova() throws NoSuchMethodException {
        Transactional transactional = ExportacaoPersistenceService.class
                .getMethod("criar", ExportacaoEntity.class)
                .getAnnotation(Transactional.class);

        assertThat(transactional).isNotNull();
        assertThat(transactional.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
    }
}
