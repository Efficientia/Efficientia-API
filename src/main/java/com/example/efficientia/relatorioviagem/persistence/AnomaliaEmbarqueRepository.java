package com.example.efficientia.relatorioviagem.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AnomaliaEmbarqueRepository extends JpaRepository<AnomaliaEmbarqueEntity, Integer> {
    List<AnomaliaEmbarqueEntity> findByRelatorioId(Integer relatorioId);
    void deleteByRelatorioId(Integer relatorioId);
}
