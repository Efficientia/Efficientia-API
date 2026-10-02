package com.example.efficientia.caminhao.api;

import com.example.efficientia.cadastrobase.persistence.VeiculoCarretaEntity;
import com.example.efficientia.cadastrobase.persistence.VeiculoCavaloEntity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

public final class CaminhaoContracts {

    private CaminhaoContracts() {
    }

    public enum TipoVeiculo {
        CAVALO,
        CARRETA,
        CONJUNTO
    }

    public enum StatusUsoCaminhao {
        DISPONIVEL,
        EM_USO
    }

    public record CriarCaminhaoRequest(
            @NotNull TipoVeiculo tipo,
            @NotBlank @Pattern(regexp = "[A-Za-z]{3}[0-9][A-Za-z0-9][0-9]{2}") String placa,
            @Pattern(regexp = "[A-Za-z]{3}[0-9][A-Za-z0-9][0-9]{2}") String placaCarreta,
            Integer empresaId,
            @Positive Integer capacidadeCabecas,
            Integer kmAcumulado,
            @Size(max = 50) String marca,
            @Size(max = 50) String modelo,
            Integer anoFabricacao,
            @Size(max = 50) String tipoCarreta,
            LocalDate dataVencimentoInspecao,
            Boolean ativo
    ) {
    }

    public record AtualizarCaminhaoRequest(
            @Pattern(regexp = "[A-Za-z]{3}[0-9][A-Za-z0-9][0-9]{2}") String placa,
            Integer empresaId,
            @Positive Integer capacidadeCabecas,
            Integer kmAcumulado,
            @Size(max = 50) String marca,
            @Size(max = 50) String modelo,
            Integer anoFabricacao,
            @Size(max = 50) String tipoCarreta,
            LocalDate dataVencimentoInspecao,
            Boolean ativo
    ) {
    }

    public record CaminhaoResponse(
            Integer id,
            String tipo,
            String placa,
            Integer empresaId,
            Boolean ativo,
            LocalDate dataVencimentoInspecao,
            Integer kmAcumulado,
            Integer capacidadeCabecas,
            String marca,
            String modelo,
            Integer anoFabricacao,
            String tipoCarreta,
            String statusUso,
            Integer relatorioAtualId,
            Integer motoristaAtualId,
            String motoristaAtualNome
    ) {
        public static CaminhaoResponse fromCavalo(
                VeiculoCavaloEntity entity,
                String statusUso,
                Integer relatorioAtualId,
                Integer motoristaAtualId,
                String motoristaAtualNome
        ) {
            return new CaminhaoResponse(
                    entity.getId(),
                    "CAVALO",
                    entity.getPlaca(),
                    entity.getEmpresaId(),
                    entity.getAtivo() == null || entity.getAtivo(),
                    entity.getDataVencimentoInspecao(),
                    entity.getKmAcumulado() != null ? entity.getKmAcumulado() : 0,
                    null,
                    entity.getMarca(),
                    entity.getModelo(),
                    entity.getAnoFabricacao(),
                    null,
                    statusUso,
                    relatorioAtualId,
                    motoristaAtualId,
                    motoristaAtualNome
            );
        }

        public static CaminhaoResponse fromCarreta(
                VeiculoCarretaEntity entity,
                String statusUso,
                Integer relatorioAtualId,
                Integer motoristaAtualId,
                String motoristaAtualNome
        ) {
            return new CaminhaoResponse(
                    entity.getId(),
                    "CARRETA",
                    entity.getPlaca(),
                    entity.getEmpresaId(),
                    entity.getAtivo() == null || entity.getAtivo(),
                    entity.getDataVencimentoInspecao(),
                    null,
                    entity.getCapacidadeCabecas(),
                    entity.getMarca(),
                    entity.getModelo(),
                    null,
                    entity.getTipoCarreta(),
                    statusUso,
                    relatorioAtualId,
                    motoristaAtualId,
                    motoristaAtualNome
            );
        }
    }

    public record CaminhaoAppResponse(
            Integer id,
            String tipo,
            String placa,
            Integer capacidadeCabecas,
            Integer kmAcumulado,
            String marca,
            String modelo,
            Boolean ativo,
            Boolean inspecaoValida,
            Long diasParaVencerInspecao,
            String statusUso,
            Integer relatorioAtualId,
            String motoristaAtualNome
    ) {
        public static CaminhaoAppResponse fromCaminhaoResponse(CaminhaoResponse res) {
            LocalDate hoje = LocalDate.now();
            boolean valida = res.dataVencimentoInspecao() == null || !res.dataVencimentoInspecao().isBefore(hoje);
            Long dias = res.dataVencimentoInspecao() != null ? ChronoUnit.DAYS.between(hoje, res.dataVencimentoInspecao()) : null;

            return new CaminhaoAppResponse(
                    res.id(),
                    res.tipo(),
                    res.placa(),
                    res.capacidadeCabecas(),
                    res.kmAcumulado(),
                    res.marca(),
                    res.modelo(),
                    res.ativo(),
                    valida,
                    dias,
                    res.statusUso(),
                    res.relatorioAtualId(),
                    res.motoristaAtualNome()
            );
        }
    }

    public record CaminhaoRelatorioResponse(
            Integer relatorioId,
            String statusRelatorio,
            Integer motoristaId,
            String motoristaNome,
            CaminhaoResponse cavalo,
            CaminhaoResponse carreta,
            String placaCavalo,
            String placaCarreta,
            Boolean emUso
    ) {
    }

    public record VincularCaminhaoRelatorioRequest(
            @Pattern(regexp = "[A-Za-z]{3}[0-9][A-Za-z0-9][0-9]{2}") String placaCavalo,
            @Pattern(regexp = "[A-Za-z]{3}[0-9][A-Za-z0-9][0-9]{2}") String placaCarreta,
            Integer cavaloId,
            Integer carretaId,
            Integer motoristaId
    ) {
    }
}
