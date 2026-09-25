package com.example.efficientia.empresa.api;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class EmpresaContracts {

    private EmpresaContracts() {
    }

    public record CriarEmpresaRequest(
            @NotBlank(message = "O nome da empresa é obrigatório.")
            @Size(max = 150, message = "O nome da empresa deve ter no máximo 150 caracteres.")
            @JsonAlias({"nome", "nome_empresa"})
            String nomeEmpresa,

            @Size(max = 150, message = "A razão social deve ter no máximo 150 caracteres.")
            @JsonAlias({"razao_social", "razaoSocial"})
            String razaoSocial,

            @NotBlank(message = "O CNPJ é obrigatório.")
            @JsonAlias({"cnpj_empresa"})
            String cnpj,

            @NotBlank(message = "O e-mail corporativo é obrigatório.")
            @Email(message = "Formato de e-mail corporativo inválido.")
            @Size(max = 150, message = "O e-mail corporativo deve ter no máximo 150 caracteres.")
            @JsonAlias({"email", "email_corporativo", "email_empresarial", "emailEmpresarial"})
            String emailCorporativo,

            @Size(min = 6, max = 64, message = "A senha deve ter entre 6 e 64 caracteres.")
            @JsonAlias({"password"})
            String senha,

            @JsonAlias({"endereco_id", "idEndereco", "id_endereco"})
            Integer enderecoId
    ) {
        public CriarEmpresaRequest(
                String nomeEmpresa,
                String cnpj,
                String emailCorporativo,
                String senha
        ) {
            this(nomeEmpresa, nomeEmpresa, cnpj, emailCorporativo, senha, null);
        }
    }

    public record EmpresaResponse(
            Long id,
            String codigoEmpresa,
            String codigo,
            String codigoInterno,
            String nomeEmpresa,
            String nome,
            String razaoSocial,
            String cnpj,
            String emailCorporativo,
            String email,
            Integer enderecoId,
            String status,
            Boolean requerPrimeiroAdmin,
            String proximoPasso,
            String mensagem,
            Instant criadoEm
    ) {
        public EmpresaResponse(
                Long id,
                String codigoEmpresa,
                String nomeEmpresa,
                String cnpj,
                String emailCorporativo,
                Instant criadoEm
        ) {
            this(id, codigoEmpresa, codigoEmpresa, codigoEmpresa, nomeEmpresa, nomeEmpresa, nomeEmpresa, cnpj, emailCorporativo, emailCorporativo, null, "PENDENTE_PRIMEIRO_ADMIN", true, "CADASTRO_PRIMEIRO_ADMIN", "Empresa registrada com sucesso. O cadastro do primeiro administrador é obrigatório para liberar o acesso.", criadoEm);
        }

        public EmpresaResponse(
                Long id,
                String codigoEmpresa,
                String nomeEmpresa,
                String razaoSocial,
                String cnpj,
                String emailCorporativo,
                Integer enderecoId,
                String status,
                Boolean requerPrimeiroAdmin,
                String proximoPasso,
                String mensagem,
                Instant criadoEm
        ) {
            this(id, codigoEmpresa, codigoEmpresa, codigoEmpresa, nomeEmpresa, nomeEmpresa, razaoSocial, cnpj, emailCorporativo, emailCorporativo, enderecoId, status, requerPrimeiroAdmin, proximoPasso, mensagem, criadoEm);
        }

        @JsonProperty("codigo")
        public String codigo() {
            return codigoEmpresa;
        }

        @JsonProperty("codigoInterno")
        public String codigoInterno() {
            return codigoEmpresa;
        }

        @JsonProperty("nome")
        public String nome() {
            return nomeEmpresa;
        }

        @JsonProperty("email")
        public String email() {
            return emailCorporativo;
        }
    }

    public record LoginEmpresaRequest(
            @NotBlank(message = "O CNPJ, e-mail ou código da empresa é obrigatório.")
            @JsonAlias({"cnpj_empresa", "identificador", "email", "codigoEmpresa", "codigo", "codigo_empresa"})
            String cnpj,

            @JsonAlias({"password"})
            String senha
    ) {
    }

    public record LoginEmpresaResponse(
            EmpresaResponse empresa,
            String status,
            Boolean requerPrimeiroAdmin,
            String proximoPasso,
            String token,
            String tokenType,
            Long expiraEmSegundos,
            Object admin,
            String mensagem
    ) {
        public static LoginEmpresaResponse pendentePrimeiroAdmin(EmpresaResponse empresa) {
            return new LoginEmpresaResponse(
                    empresa,
                    "PENDENTE_PRIMEIRO_ADMIN",
                    true,
                    "CADASTRO_PRIMEIRO_ADMIN",
                    null,
                    null,
                    null,
                    null,
                    "Empresa localizada. É de total obrigatoriedade o cadastro do primeiro administrador para liberar o acesso ao sistema."
            );
        }

        public static LoginEmpresaResponse ativoRequerAdmin(EmpresaResponse empresa) {
            return new LoginEmpresaResponse(
                    empresa,
                    "ATIVO",
                    false,
                    "LOGIN_ADMINISTRADOR",
                    null,
                    null,
                    null,
                    null,
                    "Empresa ativa. Para acessar o painel corporativo, realize o login com as credenciais de um administrador cadastrado."
            );
        }

        public static LoginEmpresaResponse autenticado(EmpresaResponse empresa, String token, Object admin) {
            return new LoginEmpresaResponse(
                    empresa,
                    "ATIVO",
                    false,
                    "PAINEL_ADMINISTRATIVO",
                    token,
                    "Bearer",
                    86400L,
                    admin,
                    "Autenticação corporativa realizada com sucesso."
            );
        }
    }
}
