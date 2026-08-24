package com.example.efficientia.cadastrobase.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VeiculoCarretaRepository extends JpaRepository<VeiculoCarretaEntity, Integer> {
    boolean existsByPlaca(String placa);
}
