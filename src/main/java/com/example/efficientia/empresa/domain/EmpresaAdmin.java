package com.example.efficientia.empresa.domain;

import java.time.Instant;

/**
 * Representa um administrador vinculado a uma empresa parceira no ecossistema Efficientia.
 * Uma empresa pode possuir múltiplos administradores para atendimento a requisitos
 * de RH, organograma e segregação de funções.
 */
public class EmpresaAdmin {

    private Long id;
    private Long empresaId;
    private String codigoEmpresa;
    private String cnpjEmpresa;
    private String nome;
    private String email;
    private String cpf;
    private String telefone;
    private String cargo;
    private String senhaHash;
    private Boolean ativo;
    private Instant criadoEm;
    private Instant atualizadoEm;

    public EmpresaAdmin() {
    }

    public EmpresaAdmin(
            Long id,
            Long empresaId,
            String codigoEmpresa,
            String cnpjEmpresa,
            String nome,
            String email,
            String cpf,
            String telefone,
            String cargo,
            String senhaHash,
            Boolean ativo,
            Instant criadoEm,
            Instant atualizadoEm
    ) {
        this.id = id;
        this.empresaId = empresaId;
        this.codigoEmpresa = codigoEmpresa;
        this.cnpjEmpresa = cnpjEmpresa;
        this.nome = nome;
        this.email = email;
        this.cpf = cpf;
        this.telefone = telefone;
        this.cargo = cargo;
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

    public Long getEmpresaId() {
        return empresaId;
    }

    public void setEmpresaId(Long empresaId) {
        this.empresaId = empresaId;
    }

    public String getCodigoEmpresa() {
        return codigoEmpresa;
    }

    public void setCodigoEmpresa(String codigoEmpresa) {
        this.codigoEmpresa = codigoEmpresa;
    }

    public String getCnpjEmpresa() {
        return cnpjEmpresa;
    }

    public void setCnpjEmpresa(String cnpjEmpresa) {
        this.cnpjEmpresa = cnpjEmpresa;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getCpf() {
        return cpf;
    }

    public void setCpf(String cpf) {
        this.cpf = cpf;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public String getCargo() {
        return cargo;
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
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
