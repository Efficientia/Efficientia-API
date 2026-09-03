package com.example.efficientia.exportacao.service;

import com.example.efficientia.exportacao.persistence.ExportacaoEntity;
import com.example.efficientia.exportacao.persistence.ExportacaoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExportacaoPersistenceService {

    private final ExportacaoRepository repository;

    public ExportacaoPersistenceService(ExportacaoRepository repository) {
        this.repository = repository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public ExportacaoEntity criar(ExportacaoEntity exportacao) {
        return repository.saveAndFlush(exportacao);
    }
}
