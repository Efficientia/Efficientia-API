package com.example.efficientia.cadastrobase.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "fazenda", schema = "public")
public class FazendaEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "pecuarista_id", nullable = false)
    private Integer pecuaristaId;

    @Column(name = "endereco_id", nullable = false)
    private Integer enderecoId;

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    public FazendaEntity() {
    }

    public Integer getId() { return id; }
    public Integer getPecuaristaId() { return pecuaristaId; }
    public Integer getEnderecoId() { return enderecoId; }
    public String getNome() { return nome; }

    public void setPecuaristaId(Integer pecuaristaId) { this.pecuaristaId = pecuaristaId; }
    public void setEnderecoId(Integer enderecoId) { this.enderecoId = enderecoId; }
    public void setNome(String nome) { this.nome = nome; }
}
