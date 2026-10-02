package com.example.efficientia.relatorioviagem.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface RelatorioViagemRepository extends JpaRepository<RelatorioViagemEntity, Integer> {

    boolean existsByNumeroGta(String numeroGta);

    boolean existsByIdAndMotoristaId(Integer id, Integer motoristaId);

    List<RelatorioViagemEntity> findByMotoristaId(Integer motoristaId);

    List<RelatorioViagemEntity> findByCavaloId(Integer cavaloId);

    List<RelatorioViagemEntity> findByCarretaId(Integer carretaId);

    Optional<RelatorioViagemEntity> findFirstByMotoristaIdAndStatusInOrderByCriadoEmDesc(Integer motoristaId, Collection<String> statuses);

    Optional<RelatorioViagemEntity> findFirstByCavaloIdAndStatusInOrderByCriadoEmDesc(Integer cavaloId, Collection<String> statuses);

    Optional<RelatorioViagemEntity> findFirstByCarretaIdAndStatusInOrderByCriadoEmDesc(Integer carretaId, Collection<String> statuses);

    List<RelatorioViagemEntity> findByStatusIn(Collection<String> statuses);
}
