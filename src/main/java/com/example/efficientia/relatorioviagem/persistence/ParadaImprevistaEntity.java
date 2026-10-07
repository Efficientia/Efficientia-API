package com.example.efficientia.relatorioviagem.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "tb_parada_imprevista", schema = "sc_operacao")
public class ParadaImprevistaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "relatorio_id", nullable = false)
    private Integer relatorioId;

    @Column(name = "motivo", nullable = false, length = 50)
    private String motivo;

    @Column(name = "data_hora_inicio", nullable = false)
    private LocalDateTime dataHoraInicio;

    @Column(name = "data_hora_fim", nullable = false)
    private LocalDateTime dataHoraFim;

    public ParadaImprevistaEntity() {
    }

    public ParadaImprevistaEntity(Integer relatorioId, String motivo, LocalDateTime dataHoraInicio, LocalDateTime dataHoraFim) {
        this.relatorioId = relatorioId;
        this.motivo = motivo;
        this.dataHoraInicio = dataHoraInicio;
        this.dataHoraFim = dataHoraFim;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getRelatorioId() { return relatorioId; }
    public void setRelatorioId(Integer relatorioId) { this.relatorioId = relatorioId; }
    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }
    public LocalDateTime getDataHoraInicio() { return dataHoraInicio; }
    public void setDataHoraInicio(LocalDateTime dataHoraInicio) { this.dataHoraInicio = dataHoraInicio; }
    public LocalDateTime getDataHoraFim() { return dataHoraFim; }
    public void setDataHoraFim(LocalDateTime dataHoraFim) { this.dataHoraFim = dataHoraFim; }
}
