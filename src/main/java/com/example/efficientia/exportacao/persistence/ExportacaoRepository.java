package com.example.efficientia.exportacao.persistence;

import com.example.efficientia.exportacao.domain.EstadoExportacao;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ExportacaoRepository extends JpaRepository<ExportacaoEntity, UUID> {

    Optional<ExportacaoEntity> findByIdempotencyKey(UUID idempotencyKey);

    Optional<ExportacaoEntity> findFirstByEstadoOrderByCriadoEmAsc(EstadoExportacao estado);

    List<ExportacaoEntity> findByEstadoInAndExpiraEmBefore(List<EstadoExportacao> estados, Instant expiraEm);
}
