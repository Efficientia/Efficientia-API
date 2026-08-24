package com.example.efficientia.cadastrobase.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "endereco", schema = "public")
public class EnderecoEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "cep", nullable = false, length = 10)
    private String cep;

    @Column(name = "logradouro", nullable = false, length = 150)
    private String logradouro;

    @Column(name = "numero", nullable = false, length = 20)
    private String numero;

    @Column(name = "cidade", nullable = false, length = 100)
    private String cidade;

    @Column(name = "estado", nullable = false, length = 2)
    private String estado;

    public EnderecoEntity() {
    }

    public Integer getId() { return id; }
    public String getCep() { return cep; }
    public String getLogradouro() { return logradouro; }
    public String getNumero() { return numero; }
    public String getCidade() { return cidade; }
    public String getEstado() { return estado; }

    public void setCep(String cep) { this.cep = cep; }
    public void setLogradouro(String logradouro) { this.logradouro = logradouro; }
    public void setNumero(String numero) { this.numero = numero; }
    public void setCidade(String cidade) { this.cidade = cidade; }
    public void setEstado(String estado) { this.estado = estado; }
}
