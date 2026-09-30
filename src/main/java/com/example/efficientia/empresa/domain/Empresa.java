package com.example.efficientia.empresa.domain;

import java.time.Instant;

public class Empresa {

    private Long id;
    private Integer enderecoId;
    private String codigoEmpresa;
    private String nomeEmpresa;
    private String nomeFantasia;
    private String razaoSocial;
    private String cnpj;
    private String emailCorporativo;
    private String telefone;
    private String logoUrl;
    private Integer etapaCadastro = 1;
    private Boolean cadastroCompleto = false;
    private String senhaHash;
    private Boolean ativo = true;
    private Instant criadoEm;
    private Instant atualizadoEm;

    public Empresa() {
        this.etapaCadastro = 1;
        this.cadastroCompleto = false;
        this.ativo = true;
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
        this.nomeFantasia = nomeEmpresa;
        this.razaoSocial = razaoSocial != null && !razaoSocial.isBlank() ? razaoSocial : nomeEmpresa;
        this.cnpj = cnpj;
        this.emailCorporativo = emailCorporativo;
        this.senhaHash = senhaHash;
        this.ativo = ativo;
        this.etapaCadastro = 1;
        this.cadastroCompleto = false;
        this.criadoEm = criadoEm;
        this.atualizadoEm = atualizadoEm;
    }

    public Empresa(
            Long id,
            Integer enderecoId,
            String codigoEmpresa,
            String nomeEmpresa,
            String nomeFantasia,
            String razaoSocial,
            String cnpj,
            String emailCorporativo,
            String telefone,
            String logoUrl,
            Integer etapaCadastro,
            Boolean cadastroCompleto,
            String senhaHash,
            Boolean ativo,
            Instant criadoEm,
            Instant atualizadoEm
    ) {
        this.id = id;
        this.enderecoId = enderecoId;
        this.codigoEmpresa = codigoEmpresa;
        this.nomeEmpresa = nomeEmpresa;
        this.nomeFantasia = nomeFantasia != null && !nomeFantasia.isBlank() ? nomeFantasia : nomeEmpresa;
        this.razaoSocial = razaoSocial != null && !razaoSocial.isBlank() ? razaoSocial : (this.nomeFantasia != null ? this.nomeFantasia : nomeEmpresa);
        this.cnpj = cnpj;
        this.emailCorporativo = emailCorporativo;
        this.telefone = telefone;
        this.logoUrl = logoUrl;
        this.etapaCadastro = etapaCadastro != null ? etapaCadastro : 1;
        this.cadastroCompleto = cadastroCompleto != null ? cadastroCompleto : false;
        this.senhaHash = senhaHash;
        this.ativo = ativo != null ? ativo : true;
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
        if (this.nomeFantasia == null || this.nomeFantasia.isBlank()) {
            this.nomeFantasia = nomeEmpresa;
        }
    }

    public String getNomeFantasia() {
        return nomeFantasia != null && !nomeFantasia.isBlank() ? nomeFantasia : nomeEmpresa;
    }

    public void setNomeFantasia(String nomeFantasia) {
        this.nomeFantasia = nomeFantasia;
        if (this.nomeEmpresa == null || this.nomeEmpresa.isBlank()) {
            this.nomeEmpresa = nomeFantasia;
        }
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
    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getLogoUrl() {
        return logoUrl;
    }

    public void setLogoUrl(String logoUrl) {
        this.logoUrl = logoUrl;
    }

    public Integer getEtapaCadastro() {
        return etapaCadastro != null ? etapaCadastro : 1;
    }

    public void setEtapaCadastro(Integer etapaCadastro) {
        this.etapaCadastro = etapaCadastro;
    }

    public Boolean getCadastroCompleto() {
        return cadastroCompleto != null ? cadastroCompleto : false;
    }

    public void setCadastroCompleto(Boolean cadastroCompleto) {
        this.cadastroCompleto = cadastroCompleto;
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
