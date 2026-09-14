package com.example.efficientia.cadastrobase.persistence;

import com.example.efficientia.cadastrobase.domain.TipoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Integer> {
    boolean existsByCpf(String cpf);
    boolean existsByEmail(String email);
    boolean existsByIdAndTipo(Integer id, TipoUsuario tipo);
    Optional<UsuarioEntity> findByCpf(String cpf);
    Optional<UsuarioEntity> findByEmail(String email);
}

