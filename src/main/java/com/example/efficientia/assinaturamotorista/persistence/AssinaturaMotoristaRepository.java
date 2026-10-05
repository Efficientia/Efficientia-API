package com.example.efficientia.assinaturamotorista.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssinaturaMotoristaRepository extends JpaRepository<AssinaturaMotoristaEntity, UUID> {

    Optional<AssinaturaMotoristaEntity> findByMotoristaIdAndAtivaTrue(Integer motoristaId);

    Optional<AssinaturaMotoristaEntity> findByIdempotencyKey(UUID idempotencyKey);

    List<AssinaturaMotoristaEntity> findByMotoristaIdOrderByCriadoEmDesc(Integer motoristaId);
}
