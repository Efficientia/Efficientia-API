package com.example.efficientia.empresa.api;

import com.example.efficientia.cadastrobase.domain.TipoUsuario;
import com.example.efficientia.empresa.api.EmpresaContracts.EmpresaResponse;
import com.example.efficientia.empresa.domain.EmpresaAdmin;
import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

public final class EmpresaAdminContracts {

    private EmpresaAdminContracts() {
    }

    /**
     * DTO de entrada para criação do primeiro administrador logo após o cadastro da empresa.
     * Permite identificação da empresa via empresaId, codigoEmpresa ou cnpj.
     */
    public record CriarPrimeiroAdminRequest(
            @JsonAlias({"empresa_id", "idEmpresa"})
            Long empresaId,

            @Size(max = 50, message = "O código da empresa deve ter no máximo 50 caracteres.")
            @JsonAlias({"codigo_empresa", "codigo", "codigoInterno", "codigo_interno"})
            String codigoEmpresa,

            @JsonAlias({"cnpj_empresa"})
            String cnpj,

            @NotBlank(message = "O nome do administrador é obrigatório.")
            @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
            @JsonAlias({"nome_completo", "nomeCompleto"})
            String nome,

            @NotBlank(message = "O e-mail do administrador é obrigatório.")
            @Email(message = "Formato de e-mail inválido.")
            @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
            String email,

            @NotBlank(message = "A senha é obrigatória.")
            @Size(min = 6, max = 64, message = "A senha deve ter entre 6 e 64 caracteres.")
            @JsonAlias({"password"})
            String senha,

            @Pattern(regexp = "\\d{11}", message = "O CPF deve conter exatamente 11 dígitos numéricos.")
            @JsonAlias({"cpf_admin"})
            String cpf,

            @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres.")
            String telefone,

            @Size(max = 100, message = "O cargo deve ter no máximo 100 caracteres.")
            @JsonAlias({"departamento", "funcao"})
            String cargo
    ) {
    }

    /**
     * DTO de entrada para adesão de novos administradores por um administrador existente.
     */
    public record CriarAdminRequest(
            @NotBlank(message = "O nome do administrador é obrigatório.")
            @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
            @JsonAlias({"nome_completo", "nomeCompleto"})
            String nome,

            @NotBlank(message = "O e-mail do administrador é obrigatório.")
            @Email(message = "Formato de e-mail inválido.")
            @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
            String email,

            @NotBlank(message = "A senha é obrigatória.")
            @Size(min = 6, max = 64, message = "A senha deve ter entre 6 e 64 caracteres.")
            @JsonAlias({"password"})
            String senha,

            @Pattern(regexp = "\\d{11}", message = "O CPF deve conter exatamente 11 dígitos numéricos.")
            @JsonAlias({"cpf_admin"})
            String cpf,

            @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres.")
            String telefone,

            @Size(max = 100, message = "O cargo deve ter no máximo 100 caracteres.")
            @JsonAlias({"departamento", "funcao"})
            String cargo
    ) {
    }

    /**
     * DTO de resposta com os dados de um administrador da empresa.
     */
    public record AdminResponse(
            Long id,
            Long empresaId,
            String codigoEmpresa,
            String cnpjEmpresa,
            String nome,
            String email,
            String cpf,
            String telefone,
            String cargo,
            Boolean ativo,
            Instant criadoEm
    ) {
        public static AdminResponse from(EmpresaAdmin admin) {
            return new AdminResponse(
                    admin.getId(),
                    admin.getEmpresaId(),
                    admin.getCodigoEmpresa(),
                    admin.getCnpjEmpresa(),
                    admin.getNome(),
                    admin.getEmail(),
                    admin.getCpf(),
                    admin.getTelefone(),
                    admin.getCargo(),
                    admin.getAtivo(),
                    admin.getCriadoEm()
            );
        }
    }

    /**
     * DTO de entrada para login do administrador.
     */
    public record LoginAdminRequest(
            @NotBlank(message = "O identificador (e-mail ou CPF) é obrigatório.")
            @JsonAlias({"login", "usuario", "cpf"})
            String email,

            @NotBlank(message = "A senha é obrigatória.")
            @JsonAlias({"password"})
            String senha,

            @Size(max = 50, message = "O código da empresa deve ter no máximo 50 caracteres.")
            @JsonAlias({"codigo_empresa", "codigo", "codigoInterno", "codigo_interno"})
            String codigoEmpresa,

            @JsonAlias({"cnpj_empresa", "cnpjEmpresa"})
            String cnpj
    ) {
        public LoginAdminRequest(String email, String senha, String codigoEmpresa) {
            this(email, senha, codigoEmpresa, null);
        }
    }

    /**
     * DTO de resposta para login bem-sucedido de administrador.
     */
    public record LoginAdminResponse(
            String token,
            String tokenType,
            Long expiraEmSegundos,
            AdminResponse admin,
            EmpresaResponse empresa,
            String mensagem
    ) {
        public LoginAdminResponse(String token, AdminResponse admin, EmpresaResponse empresa) {
            this(token, "Bearer", 86400L, admin, empresa, "Autenticação de administrador realizada com sucesso. Acesso liberado.");
        }

        @JsonProperty("roles")
        public List<String> roles() {
            return List.of("ADMIN", "ADMINISTRADOR");
        }
    }

    /**
     * DTO para o administrador cadastrar funcionários da empresa (motoristas, manobristas, analistas, curraleiros, pecuaristas).
     */
    public record CriarFuncionarioEmpresaRequest(
            @NotNull(message = "O tipo do usuário é obrigatório.")
            TipoUsuario tipo,

            @NotBlank(message = "O CPF é obrigatório.")
            @Pattern(regexp = "\\d{11}", message = "O CPF deve conter exatamente 11 dígitos numéricos.")
            String cpf,

            @NotBlank(message = "O nome do funcionário é obrigatório.")
            @Size(max = 150, message = "O nome deve ter no máximo 150 caracteres.")
            @JsonAlias({"nome_completo", "nomeCompleto"})
            String nome,

            LocalDate dataNascimento,

            @NotBlank(message = "O e-mail é obrigatório.")
            @Email(message = "Formato de e-mail inválido.")
            @Size(max = 150, message = "O e-mail deve ter no máximo 150 caracteres.")
            String email,

            @NotBlank(message = "O telefone é obrigatório.")
            @Size(max = 20, message = "O telefone deve ter no máximo 20 caracteres.")
            String telefone,

            @NotBlank(message = "A senha é obrigatória.")
            @Size(min = 6, max = 64, message = "A senha deve ter entre 6 e 64 caracteres.")
            @JsonAlias({"password"})
            String senha,

            @Size(max = 150, message = "O cargo deve ter no máximo 150 caracteres.")
            String cargo,

            @Size(max = 20, message = "O número da CNH deve ter no máximo 20 caracteres.")
            @JsonAlias({"cnh_numero", "numeroCnh"})
            String cnhNumero,

            @JsonAlias({"categoria_cnh", "categoriaCnh"})
            String categoriaCnh,

            @JsonAlias({"data_vencimento_cnh", "vencimentoCnh"})
            LocalDate dataVencimentoCnh
    ) {
    }

    /**
     * DTO de resposta para funcionário vinculado à empresa.
     */
    public record FuncionarioEmpresaResponse(
            Integer id,
            Long empresaId,
            String codigoEmpresa,
            TipoUsuario tipo,
            String nome,
            String cpf,
            String email,
            String telefone,
            String cargo,
            Boolean ativo,
            Instant criadoEm
    ) {
    }
}
