package com.example.efficientia.outbox.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface OutboxEventoRepository extends JpaRepository<OutboxEventoEntity, UUID> {

    /**
     * Busca os eventos mais antigos que ainda não foram processados pelo Outbox Relay.
     * Utiliza o índice parcial idx_outbox_pendentes (WHERE processado_em IS NULL).
     *
     * @return Lista dos primeiros 50 eventos pendentes ordenados por criado_em ASC
     */
    List<OutboxEventoEntity> findTop50ByProcessadoEmIsNullOrderByCriadoEmAsc();

    /**
     * Busca eventos pendentes filtrando por empresa para isolamento multi-tenant.
     *
     * @param empresaId identificador único da empresa
     * @return Lista de eventos pendentes da empresa ordenados por criado_em ASC
     */
    List<OutboxEventoEntity> findByEmpresaIdAndProcessadoEmIsNullOrderByCriadoEmAsc(UUID empresaId);

    /**
     * Contagem de eventos que ainda não foram sincronizados com o Redis Stream.
     *
     * @return quantidade total de eventos pendentes
     */
    long countByProcessadoEmIsNull();
}
