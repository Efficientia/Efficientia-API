package com.example.efficientia.cadastrobase.persistence;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UnidadeFrigorificaRepository extends JpaRepository<UnidadeFrigorificaEntity, Integer> {
    Page<UnidadeFrigorificaEntity> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
}
