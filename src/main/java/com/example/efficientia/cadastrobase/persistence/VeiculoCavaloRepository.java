package com.example.efficientia.cadastrobase.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface VeiculoCavaloRepository extends JpaRepository<VeiculoCavaloEntity, Integer> {
    boolean existsByPlaca(String placa);
    boolean existsByPlacaAndIdNot(String placa, Integer id);
    Optional<VeiculoCavaloEntity> findByPlaca(String placa);
    List<VeiculoCavaloEntity> findByEmpresaId(Integer empresaId);
    List<VeiculoCavaloEntity> findByAtivoTrue();
    List<VeiculoCavaloEntity> findByEmpresaIdAndAtivo(Integer empresaId, Boolean ativo);
}
