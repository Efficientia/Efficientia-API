package com.example.efficientia.outbox.service;

import com.example.efficientia.outbox.persistence.OutboxEventoEntity;
import com.example.efficientia.outbox.persistence.OutboxEventoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxRelayServiceTest {

    @Mock
    private OutboxEventoRepository repository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private StreamOperations<String, Object, Object> streamOperations;

    private OutboxRelayService service;

    @BeforeEach
    void setUp() {
        service = new OutboxRelayService(repository, redisTemplate);
        ReflectionTestUtils.setField(service, "streamKey", "efficientia:events:viagens");
    }

    @Test
    @DisplayName("processarEventosPendentes retorna 0 quando repositório não tem eventos")
    void deveRetornarZeroQuandoNaoHaEventosPendentes() {
        when(repository.findTop50ByProcessadoEmIsNullOrderByCriadoEmAsc()).thenReturn(Collections.emptyList());

        int processados = service.processarEventosPendentes(10);

        assertThat(processados).isZero();
        verify(repository).findTop50ByProcessadoEmIsNullOrderByCriadoEmAsc();
        verifyNoInteractions(redisTemplate);
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("processarEventosPendentes com 2 eventos pendentes e Redis ativo publica e marca como processado")
    void deveProcessarEventosPendentesComSucesso() {
        UUID eventoId1 = UUID.randomUUID();
        UUID eventoId2 = UUID.randomUUID();
        UUID empresaId = UUID.randomUUID();

        OutboxEventoEntity evento1 = new OutboxEventoEntity(
                empresaId, "VIAGEM_CRIADA", "RelatorioViagem", UUID.randomUUID(), "{\"valor\":100}"
        );
        evento1.setId(eventoId1);

        OutboxEventoEntity evento2 = new OutboxEventoEntity(
                empresaId, "VIAGEM_FINALIZADA", "RelatorioViagem", UUID.randomUUID(), "{\"valor\":200}"
        );
        evento2.setId(eventoId2);

        List<OutboxEventoEntity> pendentes = new ArrayList<>(List.of(evento1, evento2));
        when(repository.findTop50ByProcessadoEmIsNullOrderByCriadoEmAsc()).thenReturn(pendentes);
        doReturn(streamOperations).when(redisTemplate).opsForStream();
        when(streamOperations.add(any(MapRecord.class))).thenReturn(RecordId.of("1700000000000-0"));

        int processados = service.processarEventosPendentes(50);

        assertThat(processados).isEqualTo(2);
        assertThat(evento1.getProcessadoEm()).isNotNull();
        assertThat(evento1.getUltimoErro()).isNull();
        assertThat(evento2.getProcessadoEm()).isNotNull();
        assertThat(evento2.getUltimoErro()).isNull();

        verify(streamOperations, times(2)).add(any(MapRecord.class));
        verify(repository, times(2)).save(any(OutboxEventoEntity.class));
    }

    @Test
    @DisplayName("processarEventosPendentes com campos nulos no evento preenche payload com fallbacks vazios")
    void devePublicarEventoComCamposNulosSemExcecao() {
        OutboxEventoEntity evento = new OutboxEventoEntity();
        evento.setId(null);
        evento.setEmpresaId(null);
        evento.setTipoEvento(null);
        evento.setAgregadoTipo(null);
        evento.setAgregadoId(null);
        evento.setPayload(null);
        evento.setCriadoEm(null);

        List<OutboxEventoEntity> pendentes = new ArrayList<>(List.of(evento));
        when(repository.findTop50ByProcessadoEmIsNullOrderByCriadoEmAsc()).thenReturn(pendentes);
        doReturn(streamOperations).when(redisTemplate).opsForStream();
        when(streamOperations.add(any(MapRecord.class))).thenReturn(RecordId.of("1700000000001-0"));

        int processados = service.processarEventosPendentes(10);

        assertThat(processados).isEqualTo(1);
        assertThat(evento.getProcessadoEm()).isNotNull();
        verify(streamOperations, times(1)).add(any(MapRecord.class));
        verify(repository, times(1)).save(evento);
    }

    @Test
    @DisplayName("processarEventosPendentes quando StringRedisTemplate for nulo marca processado localmente")
    void deveProcessarLocalmenteQuandoRedisTemplateForNulo() {
        OutboxRelayService serviceSemRedis = new OutboxRelayService(repository, null);
        OutboxEventoEntity evento = new OutboxEventoEntity(
                UUID.randomUUID(), "STATUS_ALTERADO", "RelatorioViagem", UUID.randomUUID(), "{}"
        );
        evento.setId(UUID.randomUUID());

        when(repository.findTop50ByProcessadoEmIsNullOrderByCriadoEmAsc()).thenReturn(new ArrayList<>(List.of(evento)));

        int processados = serviceSemRedis.processarEventosPendentes(10);

        assertThat(processados).isEqualTo(1);
        assertThat(evento.getProcessadoEm()).isNotNull();
        assertThat(evento.getUltimoErro()).isNull();
        verify(repository).save(evento);
    }

    @Test
    @DisplayName("processarEventosPendentes quando Redis falha registra falha e continua loop")
    void deveRegistrarFalhaQuandoRedisLancarExcecao() {
        OutboxEventoEntity evento1 = new OutboxEventoEntity(
                UUID.randomUUID(), "VIAGEM_1", "RelatorioViagem", UUID.randomUUID(), "{}"
        );
        evento1.setId(UUID.randomUUID());

        OutboxEventoEntity evento2 = new OutboxEventoEntity(
                UUID.randomUUID(), "VIAGEM_2", "RelatorioViagem", UUID.randomUUID(), "{}"
        );
        evento2.setId(UUID.randomUUID());

        when(repository.findTop50ByProcessadoEmIsNullOrderByCriadoEmAsc()).thenReturn(new ArrayList<>(List.of(evento1, evento2)));
        doReturn(streamOperations).when(redisTemplate).opsForStream();
        when(streamOperations.add(any(MapRecord.class)))
                .thenThrow(new RedisConnectionFailureException("Redis timeout"))
                .thenReturn(RecordId.of("1700000000002-0"));

        int processados = service.processarEventosPendentes(50);

        assertThat(processados).isEqualTo(1);

        // Primeiro evento falhou
        assertThat(evento1.getProcessadoEm()).isNull();
        assertThat(evento1.getErroTentativas()).isEqualTo(1);
        assertThat(evento1.getUltimoErro()).isEqualTo("Redis timeout");

        // Segundo evento foi processado
        assertThat(evento2.getProcessadoEm()).isNotNull();
        assertThat(evento2.getUltimoErro()).isNull();

        verify(repository, times(2)).save(any(OutboxEventoEntity.class));
    }

    @Test
    @DisplayName("processarEventosPendentes com exceção sem mensagem usa nome simples da classe de exceção")
    void deveRegistrarFalhaComNomeDaClasseQuandoMensagemNula() {
        OutboxEventoEntity evento = new OutboxEventoEntity(
                UUID.randomUUID(), "VIAGEM_1", "RelatorioViagem", UUID.randomUUID(), "{}"
        );
        evento.setId(UUID.randomUUID());

        when(repository.findTop50ByProcessadoEmIsNullOrderByCriadoEmAsc()).thenReturn(new ArrayList<>(List.of(evento)));
        doReturn(streamOperations).when(redisTemplate).opsForStream();
        when(streamOperations.add(any(MapRecord.class))).thenThrow(new RuntimeException((String) null));

        int processados = service.processarEventosPendentes(10);

        assertThat(processados).isZero();
        assertThat(evento.getProcessadoEm()).isNull();
        assertThat(evento.getErroTentativas()).isEqualTo(1);
        assertThat(evento.getUltimoErro()).isEqualTo("RuntimeException");
        verify(repository).save(evento);
    }

    @Test
    @DisplayName("processarEventosPendentes aplica fallback de limite <= 0 e faz slice do subList")
    void deveTratarLimiteZeroESubList() {
        List<OutboxEventoEntity> listaDeEventos = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            OutboxEventoEntity ev = new OutboxEventoEntity(
                    UUID.randomUUID(), "EVENTO_" + i, "RelatorioViagem", UUID.randomUUID(), "{}"
            );
            ev.setId(UUID.randomUUID());
            listaDeEventos.add(ev);
        }

        // Caso 1: limite informado é 2, lista tem 5 -> deve processar apenas os 2 primeiros
        when(repository.findTop50ByProcessadoEmIsNullOrderByCriadoEmAsc()).thenReturn(new ArrayList<>(listaDeEventos));
        doReturn(streamOperations).when(redisTemplate).opsForStream();
        when(streamOperations.add(any(MapRecord.class))).thenReturn(RecordId.of("1700000000003-0"));

        int processados = service.processarEventosPendentes(2);
        assertThat(processados).isEqualTo(2);

        // Caso 2: limite informado <= 0 (ex: -5) -> limiteEfetivo se torna 50
        clearInvocations(repository);
        when(repository.findTop50ByProcessadoEmIsNullOrderByCriadoEmAsc()).thenReturn(new ArrayList<>(listaDeEventos));
        int processadosLimitePadrao = service.processarEventosPendentes(-5);
        assertThat(processadosLimitePadrao).isEqualTo(5);
    }

    @Test
    @DisplayName("contarPendentes delega chamada ao repository.countByProcessadoEmIsNull()")
    void deveContarPendentesCorretamente() {
        when(repository.countByProcessadoEmIsNull()).thenReturn(42L);

        long pendentes = service.contarPendentes();

        assertThat(pendentes).isEqualTo(42L);
        verify(repository).countByProcessadoEmIsNull();
    }
}
