package com.example.efficientia.exportacao.service;

import com.example.efficientia.documento.storage.StorageService;
import com.example.efficientia.exportacao.domain.EstadoExportacao;
import com.example.efficientia.exportacao.persistence.ExportacaoEntity;
import com.example.efficientia.exportacao.persistence.ExportacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;

@Component
public class ExportacaoExpirationWorker {

    private static final Logger log = LoggerFactory.getLogger(ExportacaoExpirationWorker.class);

    private final ExportacaoRepository exportacaoRepository;
    private final StorageService storageService;

    public ExportacaoExpirationWorker(
            ExportacaoRepository exportacaoRepository,
            StorageService storageService
    ) {
        this.exportacaoRepository = exportacaoRepository;
        this.storageService = storageService;
    }

    @Scheduled(fixedDelay = 60000)
    public void expirarExportacoesAntigas() {
        List<EstadoExportacao> estados = List.of(EstadoExportacao.CONCLUIDA, EstadoExportacao.FALHA);
        List<ExportacaoEntity> expiradas = exportacaoRepository.findByEstadoInAndExpiraEmBefore(estados, Instant.now());

        for (ExportacaoEntity exportacao : expiradas) {
            try {
                if (exportacao.getStorageKey() != null) {
                    storageService.remover(exportacao.getStorageKey());
                }
                exportacao.setEstado(EstadoExportacao.EXPIRADA);
                exportacaoRepository.save(exportacao);
            } catch (Exception e) {
                log.error("Erro ao expirar exportação {}", exportacao.getId(), e);
            }
        }
    }
}
