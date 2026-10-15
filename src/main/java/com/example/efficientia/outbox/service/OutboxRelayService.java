package com.example.efficientia.outbox.service;

import com.example.efficientia.outbox.persistence.OutboxEventoEntity;
import com.example.efficientia.outbox.persistence.OutboxEventoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OutboxRelayService {

    private static final Logger log = LoggerFactory.getLogger(OutboxRelayService.class);

    private final OutboxEventoRepository repository;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.redis.stream.viagens:efficientia:events:viagens}")
    private String streamKey;

    public OutboxRelayService(
            OutboxEventoRepository repository,
            @Autowired(required = false) StringRedisTemplate redisTemplate) {
        this.repository = repository;
        this.redisTemplate = redisTemplate;
    }

    @Transactional
    public int processarEventosPendentes(int limite) {
        int limiteEfetivo = (limite > 0) ? limite : 50;
        List<OutboxEventoEntity> pendentes = repository.findTop50ByProcessadoEmIsNullOrderByCriadoEmAsc();

        if (pendentes.isEmpty()) {
            return 0;
        }

        if (pendentes.size() > limiteEfetivo) {
            pendentes = pendentes.subList(0, limiteEfetivo);
        }

        int processadosComSucesso = 0;

        for (OutboxEventoEntity evento : pendentes) {
            try {
                if (redisTemplate != null) {
                    publicarNoStream(evento);
                } else {
                    log.warn("StringRedisTemplate não configurado ou nulo. Marcando evento outbox {} como processado localmente.", evento.getId());
                }

                evento.marcarComoProcessado();
                repository.save(evento);
                processadosComSucesso++;
            } catch (Exception ex) {
                log.error("Erro ao relatar evento outbox {} para o Redis Stream {}: {}",
                        evento.getId(), streamKey, ex.getMessage(), ex);
                evento.registrarFalha(ex.getMessage() != null ? ex.getMessage() : ex.getClass().getSimpleName());
                repository.save(evento);
            }
        }

        return processadosComSucesso;
    }

    public long contarPendentes() {
        return repository.countByProcessadoEmIsNull();
    }

    private void publicarNoStream(OutboxEventoEntity evento) {
        Map<String, String> streamPayload = new HashMap<>();
        streamPayload.put("id", evento.getId() != null ? evento.getId().toString() : "");
        streamPayload.put("empresaId", evento.getEmpresaId() != null ? evento.getEmpresaId().toString() : "");
        streamPayload.put("tipoEvento", evento.getTipoEvento() != null ? evento.getTipoEvento() : "");
        streamPayload.put("agregadoTipo", evento.getAgregadoTipo() != null ? evento.getAgregadoTipo() : "");
        streamPayload.put("agregadoId", evento.getAgregadoId() != null ? evento.getAgregadoId().toString() : "");
        streamPayload.put("payload", evento.getPayload() != null ? evento.getPayload() : "{}");
        streamPayload.put("criadoEm", evento.getCriadoEm() != null ? evento.getCriadoEm().toString() : "");

        MapRecord<String, String, String> record = StreamRecords.string(streamPayload).withStreamKey(streamKey);
        RecordId recordId = redisTemplate.opsForStream().add(record);
        log.info("Evento outbox {} publicado com sucesso no Redis Stream '{}' com RecordId: {}",
                evento.getId(), streamKey, recordId);
    }
}
