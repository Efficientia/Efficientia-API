package com.example.efficientia.empresa.persistence;

import com.example.efficientia.empresa.domain.Empresa;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Repository
public class InMemoryEmpresaRepository implements EmpresaRepository {

    private final Map<Long, Empresa> empresasPorId = new ConcurrentHashMap<>();
    private final AtomicLong idSequence = new AtomicLong(1);

    @Override
    public Empresa salvar(Empresa empresa) {
        if (empresa.getId() == null) {
            empresa.setId(idSequence.getAndIncrement());
        }
        empresasPorId.put(empresa.getId(), empresa);
        return empresa;
    }

    @Override
    public Optional<Empresa> buscarPorId(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(empresasPorId.get(id));
    }

    @Override
    public Optional<Empresa> buscarPorCodigo(String codigo) {
        if (codigo == null) {
            return Optional.empty();
        }
        return empresasPorId.values().stream()
                .filter(e -> codigo.equalsIgnoreCase(e.getCodigoEmpresa()))
                .findFirst();
    }

    @Override
    public Optional<Empresa> buscarPorCnpj(String cnpj) {
        if (cnpj == null) {
            return Optional.empty();
        }
        return empresasPorId.values().stream()
                .filter(e -> cnpj.equalsIgnoreCase(e.getCnpj()))
                .findFirst();
    }

    @Override
    public Optional<Empresa> buscarPorEmail(String email) {
        if (email == null) {
            return Optional.empty();
        }
        return empresasPorId.values().stream()
                .filter(e -> email.equalsIgnoreCase(e.getEmailCorporativo()))
                .findFirst();
    }

    @Override
    public boolean existePorCnpj(String cnpj) {
        if (cnpj == null) {
            return false;
        }
        return empresasPorId.values().stream()
                .anyMatch(e -> cnpj.equalsIgnoreCase(e.getCnpj()));
    }

    @Override
    public boolean existePorEmail(String email) {
        if (email == null) {
            return false;
        }
        return empresasPorId.values().stream()
                .anyMatch(e -> email.equalsIgnoreCase(e.getEmailCorporativo()));
    }

    @Override
    public boolean existePorCodigo(String codigo) {
        if (codigo == null) {
            return false;
        }
        return empresasPorId.values().stream()
                .anyMatch(e -> codigo.equalsIgnoreCase(e.getCodigoEmpresa()));
    }

    @Override
    public List<Empresa> listarTodas() {
        return new ArrayList<>(empresasPorId.values());
    }
}
