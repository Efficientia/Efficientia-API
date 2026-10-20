package com.example.efficientia.empresa.persistence;

import com.example.efficientia.empresa.domain.EmpresaAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryEmpresaAdminRepository implements EmpresaAdminRepository {

    private EmpresaAdminRepository jdbcRepository;

    @Autowired(required = false)
    public void usarBancoQuandoDisponivel(JdbcTemplate jdbcTemplate) {
        this.jdbcRepository = new JdbcEmpresaAdminRepository(jdbcTemplate);
    }

    private final Map<Long, EmpresaAdmin> adminsPorId = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    @Override
    public EmpresaAdmin salvar(EmpresaAdmin admin) {
        if (jdbcRepository != null) return jdbcRepository.salvar(admin);
        if (admin.getId() == null) {
            admin.setId(idSequence.getAndIncrement());
        }
        adminsPorId.put(admin.getId(), admin);
        return admin;
    }

    @Override
    public Optional<EmpresaAdmin> buscarPorId(Long id) {
        if (jdbcRepository != null) return jdbcRepository.buscarPorId(id);
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(adminsPorId.get(id));
    }

    @Override
    public Optional<EmpresaAdmin> buscarPorEmail(String email) {
        if (jdbcRepository != null) return jdbcRepository.buscarPorEmail(email);
        if (email == null) {
            return Optional.empty();
        }
        return adminsPorId.values().stream()
                .filter(a -> email.equalsIgnoreCase(a.getEmail()))
                .findFirst();
    }

    @Override
    public Optional<EmpresaAdmin> buscarPorCpf(String cpf) {
        if (jdbcRepository != null) return jdbcRepository.buscarPorCpf(cpf);
        if (cpf == null) {
            return Optional.empty();
        }
        return adminsPorId.values().stream()
                .filter(a -> cpf.equalsIgnoreCase(a.getCpf()))
                .findFirst();
    }

    @Override
    public List<EmpresaAdmin> listarPorEmpresaId(Long empresaId) {
        if (jdbcRepository != null) return jdbcRepository.listarPorEmpresaId(empresaId);
        if (empresaId == null) {
            return List.of();
        }
        return adminsPorId.values().stream()
                .filter(a -> empresaId.equals(a.getEmpresaId()))
                .toList();
    }

    @Override
    public List<EmpresaAdmin> listarPorCodigoEmpresa(String codigoEmpresa) {
        if (jdbcRepository != null) return jdbcRepository.listarPorCodigoEmpresa(codigoEmpresa);
        if (codigoEmpresa == null) {
            return List.of();
        }
        return adminsPorId.values().stream()
                .filter(a -> codigoEmpresa.equalsIgnoreCase(a.getCodigoEmpresa()))
                .toList();
    }

    @Override
    public boolean existePorEmail(String email) {
        if (jdbcRepository != null) return jdbcRepository.existePorEmail(email);
        if (email == null) {
            return false;
        }
        return adminsPorId.values().stream()
                .anyMatch(a -> email.equalsIgnoreCase(a.getEmail()));
    }

    @Override
    public boolean existePorCpf(String cpf) {
        if (jdbcRepository != null) return jdbcRepository.existePorCpf(cpf);
        if (cpf == null) {
            return false;
        }
        return adminsPorId.values().stream()
                .anyMatch(a -> cpf.equalsIgnoreCase(a.getCpf()));
    }

    @Override
    public long contarPorEmpresaId(Long empresaId) {
        if (jdbcRepository != null) return jdbcRepository.contarPorEmpresaId(empresaId);
        if (empresaId == null) {
            return 0;
        }
        return adminsPorId.values().stream()
                .filter(a -> empresaId.equals(a.getEmpresaId()))
                .count();
    }

    @Override
    public long contarPorCodigoEmpresa(String codigoEmpresa) {
        if (jdbcRepository != null) return jdbcRepository.contarPorCodigoEmpresa(codigoEmpresa);
        if (codigoEmpresa == null) {
            return 0;
        }
        return adminsPorId.values().stream()
                .filter(a -> codigoEmpresa.equalsIgnoreCase(a.getCodigoEmpresa()))
                .count();
    }
}
