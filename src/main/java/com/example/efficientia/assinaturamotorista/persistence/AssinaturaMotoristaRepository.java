package com.example.efficientia.assinaturamotorista.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AssinaturaMotoristaRepository extends JpaRepository<AssinaturaMotoristaEntity, UUID> {

    Optional<AssinaturaMotoristaEntity> findByMotoristaIdAndAtivaTrue(Integer motoristaId);

    Optional<AssinaturaMotoristaEntity> findByIdempotencyKey(UUID idempotencyKey);

    List<AssinaturaMotoristaEntity> findByMotoristaIdOrderByCriadoEmDesc(Integer motoristaId);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE AssinaturaMotoristaEntity a SET a.ativa = false, a.atualizadoEm = CURRENT_TIMESTAMP WHERE a.motoristaId = :motoristaId AND a.ativa = true")
    int desativarAssinaturasAtivas(@Param("motoristaId") Integer motoristaId);
}
