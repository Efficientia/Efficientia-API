package com.example.efficientia.cadastrobase.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDate;

@Entity
@Table(name = "veiculo_cavalo", schema = "public")
public class VeiculoCavaloEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "empresa_id")
    private Integer empresaId;

    @Column(name = "placa", nullable = false, unique = true, length = 7)
    private String placa;

    @Column(name = "ativo")
    private Boolean ativo;

    @Column(name = "data_vencimento_inspecao")
    private LocalDate dataVencimentoInspecao;

    @Column(name = "km_acumulado")
    private Integer kmAcumulado;

    @Column(name = "marca", length = 50)
    private String marca;

    @Column(name = "modelo", length = 50)
    private String modelo;

    @Column(name = "ano_fabricacao")
    private Integer anoFabricacao;

    public VeiculoCavaloEntity() {
    }

    public Integer getId() { return id; }
    public Integer getEmpresaId() { return empresaId; }
    public String getPlaca() { return placa; }
    public Boolean getAtivo() { return ativo; }
    public LocalDate getDataVencimentoInspecao() { return dataVencimentoInspecao; }
    public Integer getKmAcumulado() { return kmAcumulado; }
    public String getMarca() { return marca; }
    public String getModelo() { return modelo; }
    public Integer getAnoFabricacao() { return anoFabricacao; }

    public void setId(Integer id) { this.id = id; }
    public void setEmpresaId(Integer empresaId) { this.empresaId = empresaId; }
    public void setPlaca(String placa) { this.placa = placa; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
    public void setDataVencimentoInspecao(LocalDate dataVencimentoInspecao) { this.dataVencimentoInspecao = dataVencimentoInspecao; }
    public void setKmAcumulado(Integer kmAcumulado) { this.kmAcumulado = kmAcumulado; }
    public void setMarca(String marca) { this.marca = marca; }
    public void setModelo(String modelo) { this.modelo = modelo; }
    public void setAnoFabricacao(Integer anoFabricacao) { this.anoFabricacao = anoFabricacao; }
}
