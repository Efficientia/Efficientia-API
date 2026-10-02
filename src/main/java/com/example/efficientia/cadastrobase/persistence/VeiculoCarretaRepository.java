package com.example.efficientia.cadastrobase.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VeiculoCarretaRepository extends JpaRepository<VeiculoCarretaEntity, Integer> {
    boolean existsByPlaca(String placa);
    boolean existsByPlacaAndIdNot(String placa, Integer id);
    Optional<VeiculoCarretaEntity> findByPlaca(String placa);
    List<VeiculoCarretaEntity> findByEmpresaId(Integer empresaId);
    List<VeiculoCarretaEntity> findByAtivoTrue();
    List<VeiculoCarretaEntity> findByEmpresaIdAndAtivo(Integer empresaId, Boolean ativo);
}
