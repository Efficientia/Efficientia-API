package com.example.efficientia.relatorioviagem.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RelatorioViagemRepository extends JpaRepository<RelatorioViagemEntity, Integer> {

    boolean existsByNumeroGta(String numeroGta);

    boolean existsByIdAndMotoristaId(Integer id, Integer motoristaId);

    List<RelatorioViagemEntity> findByMotoristaId(Integer motoristaId);
    Page<RelatorioViagemEntity> findByMotoristaId(Integer motoristaId, Pageable pageable);
    Page<RelatorioViagemEntity> findByMotoristaIdAndStatusNot(Integer motoristaId, String status, Pageable pageable);
    Page<RelatorioViagemEntity> findByStatusNot(String status, Pageable pageable);
    Page<RelatorioViagemEntity> findByNumeroGtaContainingIgnoreCaseAndStatusNot(String numeroGta, String status, Pageable pageable);
    Page<RelatorioViagemEntity> findByMotoristaIdAndNumeroGtaContainingIgnoreCaseAndStatusNot(Integer motoristaId, String numeroGta, String status, Pageable pageable);
    Page<RelatorioViagemEntity> findByStatusAndStatusNot(String status, String excludedStatus, Pageable pageable);
    Page<RelatorioViagemEntity> findByStatusAndNumeroGtaContainingIgnoreCaseAndStatusNot(
            String status, String numeroGta, String excludedStatus, Pageable pageable);
    Page<RelatorioViagemEntity> findByMotoristaIdAndStatusAndStatusNot(
            Integer motoristaId, String status, String excludedStatus, Pageable pageable);
    Page<RelatorioViagemEntity> findByMotoristaIdAndStatusAndNumeroGtaContainingIgnoreCaseAndStatusNot(
            Integer motoristaId, String status, String numeroGta, String excludedStatus, Pageable pageable);

    List<RelatorioViagemEntity> findByCavaloId(Integer cavaloId);

    List<RelatorioViagemEntity> findByCarretaId(Integer carretaId);

    Optional<RelatorioViagemEntity> findFirstByMotoristaIdAndStatusInOrderByCriadoEmDesc(Integer motoristaId, Collection<String> statuses);

    Optional<RelatorioViagemEntity> findFirstByCavaloIdAndStatusInOrderByCriadoEmDesc(Integer cavaloId, Collection<String> statuses);

    Optional<RelatorioViagemEntity> findFirstByCarretaIdAndStatusInOrderByCriadoEmDesc(Integer carretaId, Collection<String> statuses);

    List<RelatorioViagemEntity> findByStatusIn(Collection<String> statuses);

    Optional<RelatorioViagemEntity> findByIdempotencyKey(UUID idempotencyKey);

    List<RelatorioViagemEntity> findByEmpresaId(Integer empresaId);
}
