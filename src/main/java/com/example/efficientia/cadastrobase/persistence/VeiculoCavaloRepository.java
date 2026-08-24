package com.example.efficientia.cadastrobase.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

public interface VeiculoCavaloRepository extends JpaRepository<VeiculoCavaloEntity, Integer> {
    boolean existsByPlaca(String placa);
}
