package com.example.efficientia.cadastrobase.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
// O diário usa IDs INTEGER da tabela transacional da API. A tabela
// sc_corporativo.tb_unidade_frigorifica pertence ao catálogo UUID externo.
@Table(name = "unidade_frigorifica", schema = "public")
public class UnidadeFrigorificaEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "endereco_id")
    private Integer enderecoId;

    @Column(name = "empresa_id")
    private Integer empresaId;

    public Integer getId() { return id; }
    public String getNome() { return nome; }
    public Integer getEnderecoId() { return enderecoId; }
    public Integer getEmpresaId() { return empresaId; }
}
