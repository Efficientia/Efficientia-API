package com.example.efficientia.cadastrobase.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface FazendaRepository extends JpaRepository<FazendaEntity, Integer> {
    Page<FazendaEntity> findByNomeContainingIgnoreCase(String nome, Pageable pageable);
}
