package com.example.efficientia.documento.persistence;

import com.example.efficientia.documento.domain.DocumentoCursor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public interface DocumentoRepository extends JpaRepository<DocumentoEntity, UUID> {

    Optional<DocumentoEntity> findByIdempotencyKey(UUID idempotencyKey);

    boolean existsByIdempotencyKey(UUID idempotencyKey);

    Page<DocumentoEntity> findByViagemId(Integer viagemId, Pageable pageable);

    Slice<DocumentoEntity> findAllByOrderByCriadoEmAscIdAsc(Pageable pageable);

    @Query("""
            SELECT documento
              FROM DocumentoEntity documento
             WHERE documento.criadoEm > :criadoEm
                OR (documento.criadoEm = :criadoEm AND documento.id > :id)
             ORDER BY documento.criadoEm ASC, documento.id ASC
            """)
    Slice<DocumentoEntity> findNextCursor(
            @Param("criadoEm") Instant criadoEm,
            @Param("id") UUID id,
            Pageable pageable
    );

    default Slice<DocumentoEntity> buscarProximos(DocumentoCursor cursor, int tamanho) {
        if (tamanho < 1 || tamanho > 100) {
            throw new IllegalArgumentException("O tamanho deve estar entre 1 e 100.");
        }

        Pageable pageable = PageRequest.of(0, tamanho);
        if (cursor == null) {
            return findAllByOrderByCriadoEmAscIdAsc(pageable);
        }
        return findNextCursor(cursor.criadoEm(), cursor.id(), pageable);
    }
}
