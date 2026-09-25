package com.example.efficientia.empresa.persistence;

import com.example.efficientia.empresa.domain.EmpresaAdmin;

import java.util.List;
import java.util.Optional;

public interface EmpresaAdminRepository {

    EmpresaAdmin salvar(EmpresaAdmin admin);

    Optional<EmpresaAdmin> buscarPorId(Long id);

    Optional<EmpresaAdmin> buscarPorEmail(String email);

    Optional<EmpresaAdmin> buscarPorCpf(String cpf);

    List<EmpresaAdmin> listarPorEmpresaId(Long empresaId);

    List<EmpresaAdmin> listarPorCodigoEmpresa(String codigoEmpresa);

    boolean existePorEmail(String email);

    boolean existePorCpf(String cpf);

    long contarPorEmpresaId(Long empresaId);

    long contarPorCodigoEmpresa(String codigoEmpresa);
}
