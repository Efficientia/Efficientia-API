package com.example.efficientia.cadastrobase.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "veiculo_carreta", schema = "public")
public class VeiculoCarretaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "placa", nullable = false, unique = true, length = 7)
    private String placa;

    @Column(name = "capacidade_cabecas", nullable = false)
    private Integer capacidadeCabecas;

    public VeiculoCarretaEntity() {
    }

    public Integer getId() { return id; }
    public String getPlaca() { return placa; }
    public Integer getCapacidadeCabecas() { return capacidadeCabecas; }

    public void setPlaca(String placa) { this.placa = placa; }
    public void setCapacidadeCabecas(Integer capacidadeCabecas) {
        this.capacidadeCabecas = capacidadeCabecas;
    }
}
