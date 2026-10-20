package com.example.efficientia.empresa.persistence;

import com.example.efficientia.empresa.domain.Empresa;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryEmpresaRepository implements EmpresaRepository {

    private EmpresaRepository jdbcRepository;

    @Autowired(required = false)
    public void usarBancoQuandoDisponivel(JdbcTemplate jdbcTemplate) {
        this.jdbcRepository = new JdbcEmpresaRepository(jdbcTemplate);
    }

    private final Map<Long, Empresa> empresasPorId = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    @Override
    public Empresa salvar(Empresa empresa) {
        if (jdbcRepository != null) return jdbcRepository.salvar(empresa);
        if (empresa.getId() == null) {
            empresa.setId(idSequence.getAndIncrement());
        }
        empresasPorId.put(empresa.getId(), empresa);
        return empresa;
    }

    @Override
    public Optional<Empresa> buscarPorId(Long id) {
        if (jdbcRepository != null) return jdbcRepository.buscarPorId(id);
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(empresasPorId.get(id));
    }

    @Override
    public Optional<Empresa> buscarPorCodigo(String codigo) {
        if (jdbcRepository != null) return jdbcRepository.buscarPorCodigo(codigo);
        if (codigo == null) {
            return Optional.empty();
        }
        return empresasPorId.values().stream()
                .filter(e -> codigo.equalsIgnoreCase(e.getCodigoEmpresa()))
                .findFirst();
    }

    @Override
    public Optional<Empresa> buscarPorCnpj(String cnpj) {
        if (jdbcRepository != null) return jdbcRepository.buscarPorCnpj(cnpj);
        if (cnpj == null) {
            return Optional.empty();
        }
        return empresasPorId.values().stream()
                .filter(e -> cnpj.equalsIgnoreCase(e.getCnpj()))
                .findFirst();
    }

    @Override
    public Optional<Empresa> buscarPorEmail(String email) {
        if (jdbcRepository != null) return jdbcRepository.buscarPorEmail(email);
        if (email == null) {
            return Optional.empty();
        }
        return empresasPorId.values().stream()
                .filter(e -> email.equalsIgnoreCase(e.getEmailCorporativo()))
                .findFirst();
    }

    @Override
    public boolean existePorCnpj(String cnpj) {
        if (jdbcRepository != null) return jdbcRepository.existePorCnpj(cnpj);
        if (cnpj == null) {
            return false;
        }
        return empresasPorId.values().stream()
                .anyMatch(e -> cnpj.equalsIgnoreCase(e.getCnpj()));
    }

    @Override
    public boolean existePorEmail(String email) {
        if (jdbcRepository != null) return jdbcRepository.existePorEmail(email);
        if (email == null) {
            return false;
        }
        return empresasPorId.values().stream()
                .anyMatch(e -> email.equalsIgnoreCase(e.getEmailCorporativo()));
    }

    @Override
    public boolean existePorCodigo(String codigo) {
        if (jdbcRepository != null) return jdbcRepository.existePorCodigo(codigo);
        if (codigo == null) {
            return false;
        }
        return empresasPorId.values().stream()
                .anyMatch(e -> codigo.equalsIgnoreCase(e.getCodigoEmpresa()));
    }

    @Override
    public List<Empresa> listarTodas() {
        if (jdbcRepository != null) return jdbcRepository.listarTodas();
        return new ArrayList<>(empresasPorId.values());
    }
}
