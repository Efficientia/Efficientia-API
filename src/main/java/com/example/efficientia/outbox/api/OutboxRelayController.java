package com.example.efficientia.outbox.api;

import com.example.efficientia.outbox.service.OutboxRelayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/outbox")
@Tag(name = "Transactional Outbox Relay", description = "Endpoints para gerenciamento e monitoramento da fila de eventos do Outbox Pattern integrados ao Redis Stream")
public class OutboxRelayController {

    private final OutboxRelayService outboxRelayService;

    public OutboxRelayController(OutboxRelayService outboxRelayService) {
        this.outboxRelayService = outboxRelayService;
    }

    @PostMapping("/relay")
    @Operation(summary = "Processar eventos pendentes do Outbox",
            description = "Dispara o relay para publicar eventos pendentes de viagens no Redis Stream (até o limite indicado, padrão 50).")
    public ResponseEntity<Map<String, Object>> relayEventosPendentes(
            @RequestParam(defaultValue = "50") int limite) {
        int processados = outboxRelayService.processarEventosPendentes(limite);
        long pendentesRestantes = outboxRelayService.contarPendentes();

        return ResponseEntity.ok(Map.of(
                "processados", processados,
                "pendentesRestantes", pendentesRestantes,
                "timestamp", Instant.now().toString()
        ));
    }

    @GetMapping("/pendentes")
    @Operation(summary = "Contar eventos pendentes do Outbox",
            description = "Retorna a quantidade total de eventos outbox que ainda não foram processados/publicados no stream.")
    public ResponseEntity<Map<String, Object>> contarEventosPendentes() {
        long totalPendentes = outboxRelayService.contarPendentes();

        return ResponseEntity.ok(Map.of(
                "totalPendentes", totalPendentes,
                "timestamp", Instant.now().toString()
        ));
    }
}
