package com.example.efficientia.outbox.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "tb_outbox_evento", schema = "sc_operacao")
public class OutboxEventoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(name = "id", nullable = false, updatable = false)
    private UUID id;

    @Column(name = "empresa_id", nullable = false)
    private UUID empresaId;

    @Column(name = "tipo_evento", nullable = false, length = 100)
    private String tipoEvento;

    @Column(name = "agregado_tipo", nullable = false, length = 50)
    private String agregadoTipo;

    @Column(name = "agregado_id", nullable = false)
    private UUID agregadoId;

    @Column(name = "payload", columnDefinition = "jsonb", nullable = false)
    private String payload;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "processado_em")
    private Instant processadoEm;

    @Column(name = "erro_tentativas", nullable = false)
    private Integer erroTentativas = 0;

    @Column(name = "ultimo_erro", columnDefinition = "text")
    private String ultimoErro;

    public OutboxEventoEntity() {
    }

    public OutboxEventoEntity(UUID id, UUID empresaId, String tipoEvento, String agregadoTipo,
                              UUID agregadoId, String payload, Instant criadoEm,
                              Instant processadoEm, Integer erroTentativas, String ultimoErro) {
        this.id = id;
        this.empresaId = empresaId;
        this.tipoEvento = tipoEvento;
        this.agregadoTipo = agregadoTipo;
        this.agregadoId = agregadoId;
        this.payload = payload;
        this.criadoEm = criadoEm;
        this.processadoEm = processadoEm;
        this.erroTentativas = erroTentativas != null ? erroTentativas : 0;
        this.ultimoErro = ultimoErro;
    }

    public OutboxEventoEntity(UUID empresaId, String tipoEvento, String agregadoTipo,
                              UUID agregadoId, String payload) {
        this.empresaId = empresaId;
        this.tipoEvento = tipoEvento;
        this.agregadoTipo = agregadoTipo;
        this.agregadoId = agregadoId;
        this.payload = payload;
        this.criadoEm = Instant.now();
        this.erroTentativas = 0;
    }

    @PrePersist
    public void prePersist() {
        if (this.criadoEm == null) {
            this.criadoEm = Instant.now();
        }
        if (this.erroTentativas == null) {
            this.erroTentativas = 0;
        }
    }

    public void marcarComoProcessado() {
        this.processadoEm = Instant.now();
        this.ultimoErro = null;
    }

    public void registrarFalha(String erro) {
        this.erroTentativas = (this.erroTentativas == null ? 0 : this.erroTentativas) + 1;
        this.ultimoErro = erro;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(UUID empresaId) {
        this.empresaId = empresaId;
    }

    public String getTipoEvento() {
        return tipoEvento;
    }

    public void setTipoEvento(String tipoEvento) {
        this.tipoEvento = tipoEvento;
    }

    public String getAgregadoTipo() {
        return agregadoTipo;
    }

    public void setAgregadoTipo(String agregadoTipo) {
        this.agregadoTipo = agregadoTipo;
    }

    public UUID getAgregadoId() {
        return agregadoId;
    }

    public void setAgregadoId(UUID agregadoId) {
        this.agregadoId = agregadoId;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm;
    }

    public Instant getProcessadoEm() {
        return processadoEm;
    }

    public void setProcessadoEm(Instant processadoEm) {
        this.processadoEm = processadoEm;
    }

    public Integer getErroTentativas() {
        return erroTentativas;
    }

    public void setErroTentativas(Integer erroTentativas) {
        this.erroTentativas = erroTentativas;
    }

    public String getUltimoErro() {
        return ultimoErro;
    }

    public void setUltimoErro(String ultimoErro) {
        this.ultimoErro = ultimoErro;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        OutboxEventoEntity that = (OutboxEventoEntity) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(id);
    }
}
