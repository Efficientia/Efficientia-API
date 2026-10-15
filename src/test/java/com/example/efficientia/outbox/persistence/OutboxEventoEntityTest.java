package com.example.efficientia.outbox.persistence;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OutboxEventoEntityTest {

    @Test
    @DisplayName("Deve instanciar via construtor padrão e testar todos os getters e setters")
    void deveTestarGettersESetters() {
        OutboxEventoEntity entity = new OutboxEventoEntity();

        UUID id = UUID.randomUUID();
        UUID empresaId = UUID.randomUUID();
        String tipoEvento = "VIAGEM_CONCLUIDA";
        String agregadoTipo = "RelatorioViagem";
        UUID agregadoId = UUID.randomUUID();
        String payload = "{\"status\":\"OK\"}";
        Instant criadoEm = Instant.now().minusSeconds(60);
        Instant processadoEm = Instant.now();
        Integer erroTentativas = 3;
        String ultimoErro = "Connection reset";

        entity.setId(id);
        entity.setEmpresaId(empresaId);
        entity.setTipoEvento(tipoEvento);
        entity.setAgregadoTipo(agregadoTipo);
        entity.setAgregadoId(agregadoId);
        entity.setPayload(payload);
        entity.setCriadoEm(criadoEm);
        entity.setProcessadoEm(processadoEm);
        entity.setErroTentativas(erroTentativas);
        entity.setUltimoErro(ultimoErro);

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getEmpresaId()).isEqualTo(empresaId);
        assertThat(entity.getTipoEvento()).isEqualTo(tipoEvento);
        assertThat(entity.getAgregadoTipo()).isEqualTo(agregadoTipo);
        assertThat(entity.getAgregadoId()).isEqualTo(agregadoId);
        assertThat(entity.getPayload()).isEqualTo(payload);
        assertThat(entity.getCriadoEm()).isEqualTo(criadoEm);
        assertThat(entity.getProcessadoEm()).isEqualTo(processadoEm);
        assertThat(entity.getErroTentativas()).isEqualTo(erroTentativas);
        assertThat(entity.getUltimoErro()).isEqualTo(ultimoErro);
    }

    @Test
    @DisplayName("Deve instanciar via construtor completo com todos os argumentos")
    void deveInstanciarViaConstrutorCompleto() {
        UUID id = UUID.randomUUID();
        UUID empresaId = UUID.randomUUID();
        String tipoEvento = "VIAGEM_CRIADA";
        String agregadoTipo = "RelatorioViagem";
        UUID agregadoId = UUID.randomUUID();
        String payload = "{\"id\":1}";
        Instant criadoEm = Instant.now().minusSeconds(10);
        Instant processadoEm = Instant.now();
        Integer erroTentativas = 1;
        String ultimoErro = "Timeout";

        OutboxEventoEntity entity = new OutboxEventoEntity(
                id, empresaId, tipoEvento, agregadoTipo, agregadoId,
                payload, criadoEm, processadoEm, erroTentativas, ultimoErro
        );

        assertThat(entity.getId()).isEqualTo(id);
        assertThat(entity.getEmpresaId()).isEqualTo(empresaId);
        assertThat(entity.getTipoEvento()).isEqualTo(tipoEvento);
        assertThat(entity.getAgregadoTipo()).isEqualTo(agregadoTipo);
        assertThat(entity.getAgregadoId()).isEqualTo(agregadoId);
        assertThat(entity.getPayload()).isEqualTo(payload);
        assertThat(entity.getCriadoEm()).isEqualTo(criadoEm);
        assertThat(entity.getProcessadoEm()).isEqualTo(processadoEm);
        assertThat(entity.getErroTentativas()).isEqualTo(1);
        assertThat(entity.getUltimoErro()).isEqualTo("Timeout");

        // Testar fallback quando erroTentativas é nulo no construtor
        OutboxEventoEntity entityComTentativaNula = new OutboxEventoEntity(
                id, empresaId, tipoEvento, agregadoTipo, agregadoId,
                payload, criadoEm, processadoEm, null, ultimoErro
        );
        assertThat(entityComTentativaNula.getErroTentativas()).isEqualTo(0);
    }

    @Test
    @DisplayName("Deve instanciar via construtor conveniente de 5 argumentos")
    void deveInstanciarViaConstrutorConveniente() {
        UUID empresaId = UUID.randomUUID();
        UUID agregadoId = UUID.randomUUID();

        OutboxEventoEntity entity = new OutboxEventoEntity(
                empresaId, "RELATORIO_APROVADO", "RelatorioViagem", agregadoId, "{\"aprovado\":true}"
        );

        assertThat(entity.getEmpresaId()).isEqualTo(empresaId);
        assertThat(entity.getTipoEvento()).isEqualTo("RELATORIO_APROVADO");
        assertThat(entity.getAgregadoTipo()).isEqualTo("RelatorioViagem");
        assertThat(entity.getAgregadoId()).isEqualTo(agregadoId);
        assertThat(entity.getPayload()).isEqualTo("{\"aprovado\":true}");
        assertThat(entity.getCriadoEm()).isNotNull();
        assertThat(entity.getErroTentativas()).isEqualTo(0);
        assertThat(entity.getId()).isNull();
        assertThat(entity.getProcessadoEm()).isNull();
        assertThat(entity.getUltimoErro()).isNull();
    }

    @Test
    @DisplayName("Deve aplicar prePersist() quando criadoEm e erroTentativas forem nulos")
    void deveExecutarPrePersistComValoresNulos() {
        OutboxEventoEntity entity = new OutboxEventoEntity();
        entity.setCriadoEm(null);
        entity.setErroTentativas(null);

        entity.prePersist();

        assertThat(entity.getCriadoEm()).isNotNull();
        assertThat(entity.getErroTentativas()).isEqualTo(0);
    }

    @Test
    @DisplayName("Deve manter criadoEm e erroTentativas no prePersist() quando já inicializados")
    void deveManterValoresExistentesNoPrePersist() {
        OutboxEventoEntity entity = new OutboxEventoEntity();
        Instant dataFixa = Instant.parse("2026-01-01T00:00:00Z");
        entity.setCriadoEm(dataFixa);
        entity.setErroTentativas(5);

        entity.prePersist();

        assertThat(entity.getCriadoEm()).isEqualTo(dataFixa);
        assertThat(entity.getErroTentativas()).isEqualTo(5);
    }

    @Test
    @DisplayName("marcarComoProcessado() deve registrar timestamp atual e limpar ultimoErro")
    void deveMarcarComoProcessado() {
        OutboxEventoEntity entity = new OutboxEventoEntity();
        entity.setUltimoErro("Falha anterior");
        entity.setProcessadoEm(null);

        entity.marcarComoProcessado();

        assertThat(entity.getProcessadoEm()).isNotNull();
        assertThat(entity.getUltimoErro()).isNull();
    }

    @Test
    @DisplayName("registrarFalha() deve incrementar tentativas e salvar mensagem de erro")
    void deveRegistrarFalhaEIncrementarTentativas() {
        OutboxEventoEntity entity = new OutboxEventoEntity();
        assertThat(entity.getErroTentativas()).isEqualTo(0);

        // 1ª falha: 0 -> 1
        entity.registrarFalha("Erro Redis 1");
        assertThat(entity.getErroTentativas()).isEqualTo(1);
        assertThat(entity.getUltimoErro()).isEqualTo("Erro Redis 1");

        // 2ª falha: 1 -> 2
        entity.registrarFalha("Erro Redis 2");
        assertThat(entity.getErroTentativas()).isEqualTo(2);
        assertThat(entity.getUltimoErro()).isEqualTo("Erro Redis 2");

        // Falha quando erroTentativas é nulo: null -> 1
        entity.setErroTentativas(null);
        entity.registrarFalha("Erro Redis 3");
        assertThat(entity.getErroTentativas()).isEqualTo(1);
        assertThat(entity.getUltimoErro()).isEqualTo("Erro Redis 3");
    }

    @Test
    @DisplayName("equals e hashCode devem se comportar de acordo com o contrato")
    void deveTestarEqualsEHashCode() {
        UUID id1 = UUID.randomUUID();
        UUID id2 = UUID.randomUUID();

        OutboxEventoEntity entity1 = new OutboxEventoEntity();
        entity1.setId(id1);

        OutboxEventoEntity entity2 = new OutboxEventoEntity();
        entity2.setId(id1);

        OutboxEventoEntity entity3 = new OutboxEventoEntity();
        entity3.setId(id2);

        OutboxEventoEntity entitySemId1 = new OutboxEventoEntity();
        OutboxEventoEntity entitySemId2 = new OutboxEventoEntity();

        // Mesmo objeto
        assertThat(entity1.equals(entity1)).isTrue();

        // Comparação com null
        assertThat(entity1.equals(null)).isFalse();

        // Comparação com outro tipo de objeto
        assertThat(entity1.equals("StringQualquer")).isFalse();

        // IDs iguais
        assertThat(entity1).isEqualTo(entity2);
        assertThat(entity1.hashCode()).isEqualTo(entity2.hashCode());

        // IDs diferentes
        assertThat(entity1).isNotEqualTo(entity3);
        assertThat(entity1.hashCode()).isNotEqualTo(entity3.hashCode());

        // Ambos com ID null
        assertThat(entitySemId1).isEqualTo(entitySemId2);
        assertThat(entitySemId1.hashCode()).isEqualTo(entitySemId2.hashCode());

        // Um com ID e outro sem ID
        assertThat(entity1).isNotEqualTo(entitySemId1);
    }
}
