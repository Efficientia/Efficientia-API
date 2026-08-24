package com.example.efficientia.cadastrobase.api;

import com.example.efficientia.cadastrobase.domain.TipoUsuario;
import com.example.efficientia.cadastrobase.persistence.EnderecoEntity;
import com.example.efficientia.cadastrobase.persistence.FazendaEntity;
import com.example.efficientia.cadastrobase.persistence.UsuarioEntity;
import com.example.efficientia.cadastrobase.persistence.VeiculoCarretaEntity;
import com.example.efficientia.cadastrobase.persistence.VeiculoCavaloEntity;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public final class CadastroBaseContracts {

    private CadastroBaseContracts() {
    }

    public record CriarUsuarioRequest(
            @NotNull TipoUsuario tipo,
            @NotBlank @Pattern(regexp = "\\d{11}") String cpf,
            @Size(max = 50) String codigoInterno,
            @NotBlank @Size(max = 150) String nome,
            @NotNull @Past LocalDate dataNascimento,
            @NotBlank @Email @Size(max = 150) String email,
            @NotBlank @Pattern(regexp = "\\d{10,20}") String telefone,
            @NotBlank @Size(min = 8, max = 64) String senha
    ) {
    }

    public record UsuarioResponse(
            Integer id,
            TipoUsuario tipo,
            String cpf,
            String codigoInterno,
            String nome,
            LocalDate dataNascimento,
            String email,
            String telefone,
            Boolean ativo
    ) {
        public static UsuarioResponse from(UsuarioEntity entity) {
            return new UsuarioResponse(
                    entity.getId(), entity.getTipo(), entity.getCpf(), entity.getCodigoInterno(),
                    entity.getNome(), entity.getDataNascimento(), entity.getEmail(),
                    entity.getTelefone(), entity.getAtivo()
            );
        }
    }

    public record CriarEnderecoRequest(
            @NotBlank @Pattern(regexp = "\\d{8}") String cep,
            @NotBlank @Size(max = 150) String logradouro,
            @NotBlank @Size(max = 20) String numero,
            @NotBlank @Size(max = 100) String cidade,
            @NotBlank @Pattern(regexp = "[A-Za-z]{2}") String estado
    ) {
    }

    public record EnderecoResponse(
            Integer id,
            String cep,
            String logradouro,
            String numero,
            String cidade,
            String estado
    ) {
        public static EnderecoResponse from(EnderecoEntity entity) {
            return new EnderecoResponse(
                    entity.getId(), entity.getCep(), entity.getLogradouro(),
                    entity.getNumero(), entity.getCidade(), entity.getEstado()
            );
        }
    }

    public record CriarFazendaRequest(
            @NotNull @Positive Integer pecuaristaId,
            @NotNull @Positive Integer enderecoId,
            @NotBlank @Size(max = 150) String nome
    ) {
    }

    public record FazendaResponse(
            Integer id,
            Integer pecuaristaId,
            Integer enderecoId,
            String nome
    ) {
        public static FazendaResponse from(FazendaEntity entity) {
            return new FazendaResponse(
                    entity.getId(), entity.getPecuaristaId(), entity.getEnderecoId(), entity.getNome()
            );
        }
    }

    public record CriarCavaloRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z]{3}[0-9][A-Za-z0-9][0-9]{2}") String placa,
            @NotNull Boolean ativo
    ) {
    }

    public record CavaloResponse(Integer id, String placa, Boolean ativo) {
        public static CavaloResponse from(VeiculoCavaloEntity entity) {
            return new CavaloResponse(entity.getId(), entity.getPlaca(), entity.getAtivo());
        }
    }

    public record CriarCarretaRequest(
            @NotBlank @Pattern(regexp = "[A-Za-z]{3}[0-9][A-Za-z0-9][0-9]{2}") String placa,
            @NotNull @Positive Integer capacidadeCabecas
    ) {
    }

    public record CarretaResponse(Integer id, String placa, Integer capacidadeCabecas) {
        public static CarretaResponse from(VeiculoCarretaEntity entity) {
            return new CarretaResponse(entity.getId(), entity.getPlaca(), entity.getCapacidadeCabecas());
        }
    }
}
