package com.example.efficientia.auth.api;

import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.UsuarioResponse;
import com.example.efficientia.cadastrobase.domain.TipoUsuario;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public final class AuthContracts {

    private AuthContracts() {
    }

    public record LoginRequest(
            @NotBlank @Pattern(regexp = "\\d{11}") String cpf,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank String senha,
            @NotBlank @Size(max = 50) String codigoEmpresa
    ) {
    }

    public record SignupRequest(
            @NotNull TipoUsuario tipo,
            @NotBlank @Pattern(regexp = "\\d{11}") String cpf,
            @Size(max = 50) String codigoInterno,
            @NotBlank @Size(max = 150) String nome,
            LocalDate dataNascimento,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Pattern(regexp = "\\d{10,20}") String telefone,
            @NotBlank @Size(min = 8, max = 64) String senha
    ) {
    }

    public record LoginResponse(
            String token,
            String tokenType,
            UsuarioResponse usuario
    ) {
        public LoginResponse(String token, UsuarioResponse usuario) {
            this(token, "Bearer", usuario);
        }
    }
}
