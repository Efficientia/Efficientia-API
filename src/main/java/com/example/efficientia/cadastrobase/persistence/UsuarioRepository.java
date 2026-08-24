package com.example.efficientia.cadastrobase.persistence;

import com.example.efficientia.cadastrobase.domain.TipoUsuario;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioRepository extends JpaRepository<UsuarioEntity, Integer> {
    boolean existsByCpf(String cpf);
    boolean existsByEmail(String email);
    boolean existsByIdAndTipo(Integer id, TipoUsuario tipo);
}
