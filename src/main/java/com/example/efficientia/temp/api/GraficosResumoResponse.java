package com.example.efficientia.temp.api;

import java.util.Map;

public record GraficosResumoResponse(
        Long totalViagens,
        Long totalUsuarios,
        Long totalVeiculosCavalo,
        Long totalVeiculosCarreta,
        Long totalParadasImprevistas,
        Long totalAnomalias,
        Map<String, Long> viagensPorStatus,
        Map<String, Long> paradasPorMotivo,
        Map<String, Long> anomaliasPorTipo,
        Map<String, Object> totaisGerais
) {
}
