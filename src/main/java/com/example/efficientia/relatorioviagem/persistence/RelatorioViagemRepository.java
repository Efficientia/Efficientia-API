package com.example.efficientia.relatorioviagem.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface RelatorioViagemRepository extends JpaRepository<RelatorioViagemEntity, Integer> {

    boolean existsByNumeroGta(String numeroGta);

    boolean existsByIdAndMotoristaId(Integer id, Integer motoristaId);
}
