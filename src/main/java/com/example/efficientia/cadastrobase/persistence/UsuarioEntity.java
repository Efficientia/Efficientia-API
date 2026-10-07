package com.example.efficientia.cadastrobase.persistence;

import com.example.efficientia.cadastrobase.domain.TipoUsuario;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDate;

@Entity
@Table(name = "tb_usuario", schema = "sc_corporativo")
public class UsuarioEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Enumerated(EnumType.STRING)
    @JdbcTypeCode(SqlTypes.NAMED_ENUM)
    @Column(name = "tipo", nullable = false, columnDefinition = "tipo_usuario")
    private TipoUsuario tipo;

    @Column(name = "cpf", nullable = false, unique = true, length = 11)
    private String cpf;

    @Column(name = "codigo_interno", unique = true, length = 50)
    private String codigoInterno;

    @Column(name = "nome", nullable = false, length = 150)
    private String nome;

    @Column(name = "data_nascimento")
    private LocalDate dataNascimento;

    @Column(name = "email", unique = true, length = 150)
    private String email;

    @Column(name = "telefone", length = 20)
    private String telefone;

    @Column(name = "senha_hash", nullable = false, length = 255)
    private String senhaHash;

    @Column(name = "ativo", nullable = false)
    private Boolean ativo;

    @Column(name = "empresa_id")
    private Integer empresaId;

    @Column(name = "url_assinatura_geral", columnDefinition = "TEXT")
    private String urlAssinaturaGeral;

    @Column(name = "cargo", length = 150)
    private String cargo;

    @Column(name = "nivel_acesso", length = 50)
    private String nivelAcesso;

    @Column(name = "cnh_numero", length = 20)
    private String cnhNumero;

    @Column(name = "categoria_cnh", length = 5)
    private String categoriaCnh;

    @Column(name = "data_vencimento_cnh")
    private LocalDate dataVencimentoCnh;

    @Column(name = "nome_completo", length = 250)
    private String nomeCompleto;

    @Column(name = "status_cadastro", length = 20)
    private String statusCadastro = "ativo";

    public UsuarioEntity() {
    }

    public Integer getId() { return id; }
    public TipoUsuario getTipo() { return tipo; }
    public String getCpf() { return cpf; }
    public String getCodigoInterno() { return codigoInterno; }
    public String getNome() { return nome; }
    public LocalDate getDataNascimento() { return dataNascimento; }
    public String getEmail() { return email; }
    public String getTelefone() { return telefone; }
    public String getSenhaHash() { return senhaHash; }
    public Boolean getAtivo() { return ativo; }

    public void setId(Integer id) { this.id = id; }
    public void setTipo(TipoUsuario tipo) { this.tipo = tipo; }
    public void setCpf(String cpf) { this.cpf = cpf; }
    public void setCodigoInterno(String codigoInterno) { this.codigoInterno = codigoInterno; }
    public void setNome(String nome) { this.nome = nome; }
    public void setDataNascimento(LocalDate dataNascimento) { this.dataNascimento = dataNascimento; }
    public void setEmail(String email) { this.email = email; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public void setSenhaHash(String senhaHash) { this.senhaHash = senhaHash; }
    public void setAtivo(Boolean ativo) { this.ativo = ativo; }
    public Integer getEmpresaId() { return empresaId; }
    public void setEmpresaId(Integer empresaId) { this.empresaId = empresaId; }

    public String getUrlAssinaturaGeral() { return urlAssinaturaGeral; }
    public void setUrlAssinaturaGeral(String urlAssinaturaGeral) { this.urlAssinaturaGeral = urlAssinaturaGeral; }

    public String getCargo() { return cargo; }
    public void setCargo(String cargo) { this.cargo = cargo; }

    public String getNivelAcesso() { return nivelAcesso; }
    public void setNivelAcesso(String nivelAcesso) { this.nivelAcesso = nivelAcesso; }

    public String getCnhNumero() { return cnhNumero; }
    public void setCnhNumero(String cnhNumero) { this.cnhNumero = cnhNumero; }

    public String getCategoriaCnh() { return categoriaCnh; }
    public void setCategoriaCnh(String categoriaCnh) { this.categoriaCnh = categoriaCnh; }

    public LocalDate getDataVencimentoCnh() { return dataVencimentoCnh; }
    public void setDataVencimentoCnh(LocalDate dataVencimentoCnh) { this.dataVencimentoCnh = dataVencimentoCnh; }

    public String getNomeCompleto() { return nomeCompleto != null ? nomeCompleto : nome; }
    public void setNomeCompleto(String nomeCompleto) { this.nomeCompleto = nomeCompleto; }

    public String getStatusCadastro() { return statusCadastro; }
    public void setStatusCadastro(String statusCadastro) { this.statusCadastro = statusCadastro; }
}
