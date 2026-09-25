package com.example.efficientia.empresa.persistence;

import com.example.efficientia.empresa.domain.Empresa;

import java.util.List;
import java.util.Optional;

public interface EmpresaRepository {

    Empresa salvar(Empresa empresa);

    Optional<Empresa> buscarPorId(Long id);

    Optional<Empresa> buscarPorCodigo(String codigo);

    Optional<Empresa> buscarPorCnpj(String cnpj);

    Optional<Empresa> buscarPorEmail(String email);

    boolean existePorCnpj(String cnpj);

    boolean existePorEmail(String email);

    boolean existePorCodigo(String codigo);

    List<Empresa> listarTodas();
}
