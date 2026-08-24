package com.example.efficientia.cadastrobase.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "veiculo_cavalo", schema = "public")
public class VeiculoCavaloEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "placa", nullable = false, unique = true, length = 7)
    private String placa;

    @Column(name = "ativo")
    private Boolean ativo;

    public VeiculoCavaloEntity() {
    }

    public Integer getId() { return id; }
    public String getPlaca() { return placa; }
    public Boolean getAtivo() { return ativo; }

    public void setPlaca(String placa) { this.placa = placa; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
}
