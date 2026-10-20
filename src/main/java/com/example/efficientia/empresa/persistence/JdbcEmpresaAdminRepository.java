package com.example.efficientia.empresa.persistence;

import com.example.efficientia.empresa.domain.EmpresaAdmin;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public class JdbcEmpresaAdminRepository implements EmpresaAdminRepository {

    private static final String COLUNAS = "id, empresa_id, codigo_empresa, cnpj_empresa, nome, email, cpf, telefone, cargo, senha_hash, ativo, criado_em, atualizado_em";
    private static final RowMapper<EmpresaAdmin> MAPPER = (rs, rowNum) -> new EmpresaAdmin(
            rs.getLong("id"),
            rs.getLong("empresa_id"),
            rs.getString("codigo_empresa"),
            rs.getString("cnpj_empresa"),
            rs.getString("nome"),
            rs.getString("email"),
            rs.getString("cpf"),
            rs.getString("telefone"),
            rs.getString("cargo"),
            rs.getString("senha_hash"),
            rs.getBoolean("ativo"),
            rs.getTimestamp("criado_em").toInstant(),
            rs.getTimestamp("atualizado_em").toInstant()
    );

    private final JdbcTemplate jdbc;

    public JdbcEmpresaAdminRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public EmpresaAdmin salvar(EmpresaAdmin admin) {
        if (admin.getId() == null) {
            GeneratedKeyHolder key = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO public.empresa_admin (empresa_id, codigo_empresa, cnpj_empresa, nome, email, cpf, telefone, cargo, senha_hash, ativo, criado_em, atualizado_em) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        new String[]{"id"});
                preencher(statement, admin);
                return statement;
            }, key);
            admin.setId(key.getKey().longValue());
        } else {
            jdbc.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE public.empresa_admin SET empresa_id=?, codigo_empresa=?, cnpj_empresa=?, nome=?, email=?, cpf=?, telefone=?, cargo=?, senha_hash=?, ativo=?, criado_em=?, atualizado_em=? WHERE id=?");
                preencher(statement, admin);
                statement.setLong(13, admin.getId());
                return statement;
            });
        }
        return admin;
    }

    private static void preencher(PreparedStatement statement, EmpresaAdmin admin) throws java.sql.SQLException {
        statement.setLong(1, admin.getEmpresaId());
        statement.setString(2, admin.getCodigoEmpresa());
        statement.setString(3, admin.getCnpjEmpresa());
        statement.setString(4, admin.getNome());
        statement.setString(5, admin.getEmail());
        statement.setString(6, admin.getCpf());
        statement.setString(7, admin.getTelefone());
        statement.setString(8, admin.getCargo());
        statement.setString(9, admin.getSenhaHash());
        statement.setObject(10, admin.getAtivo());
        statement.setTimestamp(11, Timestamp.from(admin.getCriadoEm()));
        statement.setTimestamp(12, Timestamp.from(admin.getAtualizadoEm()));
    }

    private Optional<EmpresaAdmin> primeiro(String sql, Object value) {
        return jdbc.query(sql, MAPPER, value).stream().findFirst();
    }

    @Override public Optional<EmpresaAdmin> buscarPorId(Long id) {
        return id == null ? Optional.empty() : primeiro("SELECT " + COLUNAS + " FROM public.empresa_admin WHERE id=?", id);
    }

    @Override public Optional<EmpresaAdmin> buscarPorEmail(String email) {
        return email == null ? Optional.empty() : primeiro("SELECT " + COLUNAS + " FROM public.empresa_admin WHERE lower(email)=lower(?)", email);
    }

    @Override public Optional<EmpresaAdmin> buscarPorCpf(String cpf) {
        return cpf == null ? Optional.empty() : primeiro("SELECT " + COLUNAS + " FROM public.empresa_admin WHERE cpf=?", cpf);
    }

    @Override public List<EmpresaAdmin> listarPorEmpresaId(Long empresaId) {
        return empresaId == null ? List.of() : jdbc.query("SELECT " + COLUNAS + " FROM public.empresa_admin WHERE empresa_id=? ORDER BY id", MAPPER, empresaId);
    }

    @Override public List<EmpresaAdmin> listarPorCodigoEmpresa(String codigoEmpresa) {
        return codigoEmpresa == null ? List.of() : jdbc.query("SELECT " + COLUNAS + " FROM public.empresa_admin WHERE lower(codigo_empresa)=lower(?) ORDER BY id", MAPPER, codigoEmpresa);
    }

    private long contar(String sql, Object value) {
        return value == null ? 0 : jdbc.queryForObject(sql, Long.class, value);
    }

    @Override public boolean existePorEmail(String email) {
        return contar("SELECT count(*) FROM public.empresa_admin WHERE lower(email)=lower(?)", email) > 0;
    }

    @Override public boolean existePorCpf(String cpf) {
        return contar("SELECT count(*) FROM public.empresa_admin WHERE cpf=?", cpf) > 0;
    }

    @Override public long contarPorEmpresaId(Long empresaId) {
        return contar("SELECT count(*) FROM public.empresa_admin WHERE empresa_id=?", empresaId);
    }

    @Override public long contarPorCodigoEmpresa(String codigoEmpresa) {
        return contar("SELECT count(*) FROM public.empresa_admin WHERE lower(codigo_empresa)=lower(?)", codigoEmpresa);
    }
}
