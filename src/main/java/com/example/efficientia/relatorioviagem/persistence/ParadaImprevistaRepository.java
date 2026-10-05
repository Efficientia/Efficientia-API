package com.example.efficientia.relatorioviagem.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ParadaImprevistaRepository extends JpaRepository<ParadaImprevistaEntity, Integer> {
    List<ParadaImprevistaEntity> findByRelatorioId(Integer relatorioId);
    void deleteByRelatorioId(Integer relatorioId);
}
