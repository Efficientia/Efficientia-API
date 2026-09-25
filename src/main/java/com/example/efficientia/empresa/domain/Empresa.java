package com.example.efficientia.empresa.domain;

import java.time.Instant;

public class Empresa {

    private Long id;
    private Integer enderecoId;
    private String codigoEmpresa;
    private String nomeEmpresa;
    private String razaoSocial;
    private String cnpj;
    private String emailCorporativo;
    private String senhaHash;
    private Boolean ativo;
    private Instant criadoEm;
    private Instant atualizadoEm;

    public Empresa() {
    }

    public Empresa(
            Long id,
            String codigoEmpresa,
            String nomeEmpresa,
            String cnpj,
            String emailCorporativo,
            String senhaHash,
            Boolean ativo,
            Instant criadoEm,
            Instant atualizadoEm
    ) {
        this(id, null, codigoEmpresa, nomeEmpresa, nomeEmpresa, cnpj, emailCorporativo, senhaHash, ativo, criadoEm, atualizadoEm);
    }

    public Empresa(
            Long id,
            Integer enderecoId,
            String codigoEmpresa,
            String nomeEmpresa,
            String razaoSocial,
            String cnpj,
            String emailCorporativo,
            String senhaHash,
            Boolean ativo,
            Instant criadoEm,
            Instant atualizadoEm
    ) {
        this.id = id;
        this.enderecoId = enderecoId;
        this.codigoEmpresa = codigoEmpresa;
        this.nomeEmpresa = nomeEmpresa;
        this.razaoSocial = razaoSocial != null && !razaoSocial.isBlank() ? razaoSocial : nomeEmpresa;
        this.cnpj = cnpj;
        this.emailCorporativo = emailCorporativo;
        this.senhaHash = senhaHash;
        this.ativo = ativo;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getEnderecoId() {
        return enderecoId;
    }

    public void setEnderecoId(Integer enderecoId) {
        this.enderecoId = enderecoId;
    }

    public String getCodigoEmpresa() {
        return codigoEmpresa;
    }

    public String getCodigoInterno() {
        return codigoEmpresa;
    }

    public void setCodigoEmpresa(String codigoEmpresa) {
        this.codigoEmpresa = codigoEmpresa;
    }

    public void setCodigoInterno(String codigoInterno) {
        this.codigoEmpresa = codigoInterno;
    }

    public String getNomeEmpresa() {
        return nomeEmpresa;
    }

    public String getNome() {
        return nomeEmpresa;
    }

    public void setNomeEmpresa(String nomeEmpresa) {
        this.nomeEmpresa = nomeEmpresa;
    }

    public String getRazaoSocial() {
        return razaoSocial != null ? razaoSocial : nomeEmpresa;
    }

    public void setRazaoSocial(String razaoSocial) {
        this.razaoSocial = razaoSocial;
    }

    public String getCnpj() {
        return cnpj;
    }

    public void setCnpj(String cnpj) {
        this.cnpj = cnpj;
    }

    public String getEmailCorporativo() {
        return emailCorporativo;
    }

    public String getEmail() {
        return emailCorporativo;
    }

    public void setEmailCorporativo(String emailCorporativo) {
        this.emailCorporativo = emailCorporativo;
    }

    public String getSenhaHash() {
        return senhaHash;
    }

    public void setSenhaHash(String senhaHash) {
        this.senhaHash = senhaHash;
    }

    public Boolean getAtivo() {
        return ativo;
    }

    public void setAtivo(Boolean ativo) {
        this.ativo = ativo;
    }

    public Instant getCriadoEm() {
        return criadoEm;
    }

    public void setCriadoEm(Instant criadoEm) {
        this.criadoEm = criadoEm;
    }

    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    public void setAtualizadoEm(Instant atualizadoEm) {
        this.atualizadoEm = atualizadoEm;
    }
}
