package com.example.efficientia.exportacao.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ExportacaoRepository extends JpaRepository<ExportacaoEntity, UUID> {

    Optional<ExportacaoEntity> findByIdempotencyKey(UUID idempotencyKey);
}
