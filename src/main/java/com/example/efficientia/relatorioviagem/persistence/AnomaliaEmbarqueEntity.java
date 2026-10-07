package com.example.efficientia.relatorioviagem.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "tb_anomalia_embarque", schema = "sc_operacao")
public class AnomaliaEmbarqueEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "relatorio_id", nullable = false)
    private Integer relatorioId;

    @Column(name = "anomalia", nullable = false, length = 50)
    private String anomalia;

    @Column(name = "descricao_outros", length = 150)
    private String descricaoOutros;

    @Column(name = "quantidade_animais")
    private Integer quantidadeAnimais = 1;

    public AnomaliaEmbarqueEntity() {
    }

    public AnomaliaEmbarqueEntity(Integer relatorioId, String anomalia, String descricaoOutros, Integer quantidadeAnimais) {
        this.relatorioId = relatorioId;
        this.anomalia = anomalia;
        this.descricaoOutros = descricaoOutros;
        this.quantidadeAnimais = quantidadeAnimais != null ? quantidadeAnimais : 1;
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getRelatorioId() { return relatorioId; }
    public void setRelatorioId(Integer relatorioId) { this.relatorioId = relatorioId; }
    public String getAnomalia() { return anomalia; }
    public void setAnomalia(String anomalia) { this.anomalia = anomalia; }
    public String getDescricaoOutros() { return descricaoOutros; }
    public void setDescricaoOutros(String descricaoOutros) { this.descricaoOutros = descricaoOutros; }
    public Integer getQuantidadeAnimais() { return quantidadeAnimais; }
    public void setQuantidadeAnimais(Integer quantidadeAnimais) { this.quantidadeAnimais = quantidadeAnimais; }
}
