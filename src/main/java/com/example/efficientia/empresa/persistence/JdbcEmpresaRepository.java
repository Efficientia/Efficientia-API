package com.example.efficientia.empresa.persistence;

import com.example.efficientia.empresa.domain.Empresa;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;

import java.sql.PreparedStatement;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;

public class JdbcEmpresaRepository implements EmpresaRepository {

    private static final String COLUNAS = "id, endereco_id, codigo_interno, nome, nome_fantasia, razao_social, cnpj, email, telefone, logo_url, etapa_cadastro, cadastro_completo, senha_hash, ativo, criado_em, atualizado_em";
    private static final RowMapper<Empresa> MAPPER = (rs, rowNum) -> new Empresa(
            rs.getLong("id"),
            (Integer) rs.getObject("endereco_id"),
            rs.getString("codigo_interno"),
            rs.getString("nome"),
            rs.getString("nome_fantasia"),
            rs.getString("razao_social"),
            rs.getString("cnpj"),
            rs.getString("email"),
            rs.getString("telefone"),
            rs.getString("logo_url"),
            (Integer) rs.getObject("etapa_cadastro"),
            (Boolean) rs.getObject("cadastro_completo"),
            rs.getString("senha_hash"),
            rs.getBoolean("ativo"),
            rs.getTimestamp("criado_em").toInstant(),
            rs.getTimestamp("atualizado_em").toInstant()
    );

    private final JdbcTemplate jdbc;

    public JdbcEmpresaRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @Override
    public Empresa salvar(Empresa empresa) {
        if (empresa.getId() == null) {
            GeneratedKeyHolder key = new GeneratedKeyHolder();
            jdbc.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        "INSERT INTO public.empresa (endereco_id, codigo_interno, nome, nome_fantasia, razao_social, cnpj, email, telefone, logo_url, etapa_cadastro, cadastro_completo, senha_hash, ativo, criado_em, atualizado_em) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
                        new String[]{"id"});
                preencher(statement, empresa, 0);
                return statement;
            }, key);
            empresa.setId(key.getKey().longValue());
        } else {
            jdbc.update(connection -> {
                PreparedStatement statement = connection.prepareStatement(
                        "UPDATE public.empresa SET endereco_id=?, codigo_interno=?, nome=?, nome_fantasia=?, razao_social=?, cnpj=?, email=?, telefone=?, logo_url=?, etapa_cadastro=?, cadastro_completo=?, senha_hash=?, ativo=?, criado_em=?, atualizado_em=? WHERE id=?");
                preencher(statement, empresa, 0);
                statement.setLong(16, empresa.getId());
                return statement;
            });
        }
        return empresa;
    }

    private static void preencher(PreparedStatement statement, Empresa empresa, int offset) throws java.sql.SQLException {
        statement.setObject(1 + offset, empresa.getEnderecoId());
        statement.setString(2 + offset, empresa.getCodigoEmpresa());
        statement.setString(3 + offset, empresa.getNomeEmpresa());
        statement.setString(4 + offset, empresa.getNomeFantasia());
        statement.setString(5 + offset, empresa.getRazaoSocial());
        statement.setString(6 + offset, empresa.getCnpj());
        statement.setString(7 + offset, empresa.getEmailCorporativo());
        statement.setString(8 + offset, empresa.getTelefone());
        statement.setString(9 + offset, empresa.getLogoUrl());
        statement.setObject(10 + offset, empresa.getEtapaCadastro());
        statement.setObject(11 + offset, empresa.getCadastroCompleto());
        statement.setString(12 + offset, empresa.getSenhaHash());
        statement.setObject(13 + offset, empresa.getAtivo());
        statement.setTimestamp(14 + offset, Timestamp.from(empresa.getCriadoEm()));
        statement.setTimestamp(15 + offset, Timestamp.from(empresa.getAtualizadoEm()));
    }

    private Optional<Empresa> primeiro(String sql, Object value) {
        return jdbc.query(sql, MAPPER, value).stream().findFirst();
    }

    @Override public Optional<Empresa> buscarPorId(Long id) {
        return id == null ? Optional.empty() : primeiro("SELECT " + COLUNAS + " FROM public.empresa WHERE id=?", id);
    }

    @Override public Optional<Empresa> buscarPorCodigo(String codigo) {
        return codigo == null ? Optional.empty() : primeiro("SELECT " + COLUNAS + " FROM public.empresa WHERE lower(codigo_interno)=lower(?)", codigo);
    }

    @Override public Optional<Empresa> buscarPorCnpj(String cnpj) {
        return cnpj == null ? Optional.empty() : primeiro("SELECT " + COLUNAS + " FROM public.empresa WHERE cnpj=?", cnpj);
    }

    @Override public Optional<Empresa> buscarPorEmail(String email) {
        return email == null ? Optional.empty() : primeiro("SELECT " + COLUNAS + " FROM public.empresa WHERE lower(email)=lower(?)", email);
    }

    private boolean existe(String coluna, String value) {
        return value != null && jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM public.empresa WHERE lower(" + coluna + ")=lower(?))", Boolean.class, value);
    }

    @Override public boolean existePorCnpj(String cnpj) { return existe("cnpj", cnpj); }
    @Override public boolean existePorEmail(String email) { return existe("email", email); }
    @Override public boolean existePorCodigo(String codigo) { return existe("codigo_interno", codigo); }

    @Override public List<Empresa> listarTodas() {
        return jdbc.query("SELECT " + COLUNAS + " FROM public.empresa ORDER BY id", MAPPER);
    }
}
