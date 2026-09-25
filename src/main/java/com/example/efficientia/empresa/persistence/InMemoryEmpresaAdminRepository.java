package com.example.efficientia.empresa.persistence;

import com.example.efficientia.empresa.domain.EmpresaAdmin;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryEmpresaAdminRepository implements EmpresaAdminRepository {

    private final Map<Long, EmpresaAdmin> adminsPorId = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    @Override
    public EmpresaAdmin salvar(EmpresaAdmin admin) {
        if (admin.getId() == null) {
            admin.setId(idSequence.getAndIncrement());
        }
        adminsPorId.put(admin.getId(), admin);
        return admin;
    }

    @Override
    public Optional<EmpresaAdmin> buscarPorId(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(adminsPorId.get(id));
    }

    @Override
    public Optional<EmpresaAdmin> buscarPorEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return adminsPorId.values().stream()
                .filter(a -> email.equalsIgnoreCase(a.getEmail()))
                .findFirst();
    }

    @Override
    public Optional<EmpresaAdmin> buscarPorCpf(String cpf) {
        if (cpf == null) {
            return Optional.empty();
        }
        return adminsPorId.values().stream()
                .filter(a -> cpf.equalsIgnoreCase(a.getCpf()))
                .findFirst();
    }

    @Override
    public List<EmpresaAdmin> listarPorEmpresaId(Long empresaId) {
        if (empresaId == null) {
            return List.of();
        }
        return adminsPorId.values().stream()
                .filter(a -> empresaId.equals(a.getEmpresaId()))
                .toList();
    }

    @Override
    public List<EmpresaAdmin> listarPorCodigoEmpresa(String codigoEmpresa) {
        if (codigoEmpresa == null) {
            return List.of();
        }
        return adminsPorId.values().stream()
                .filter(a -> codigoEmpresa.equalsIgnoreCase(a.getCodigoEmpresa()))
                .toList();
    }

    @Override
    public boolean existePorEmail(String email) {
        if (email == null) {
            return false;
        }
        return adminsPorId.values().stream()
                .anyMatch(a -> email.equalsIgnoreCase(a.getEmail()));
    }

    @Override
    public boolean existePorCpf(String cpf) {
        if (cpf == null) {
            return false;
        }
        return adminsPorId.values().stream()
                .anyMatch(a -> cpf.equalsIgnoreCase(a.getCpf()));
    }

    @Override
    public long contarPorEmpresaId(Long empresaId) {
        if (empresaId == null) {
            return 0;
        }
        return adminsPorId.values().stream()
                .filter(a -> empresaId.equals(a.getEmpresaId()))
                .count();
    }

    @Override
    public long contarPorCodigoEmpresa(String codigoEmpresa) {
        if (codigoEmpresa == null) {
            return 0;
        }
        return adminsPorId.values().stream()
                .filter(a -> codigoEmpresa.equalsIgnoreCase(a.getCodigoEmpresa()))
                .count();
    }
}
