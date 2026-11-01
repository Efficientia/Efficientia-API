package com.example.efficientia.auth.service;

import com.example.efficientia.auth.api.AutenticacaoInvalidaException;
import com.example.efficientia.cadastrobase.domain.TipoUsuario;
import com.example.efficientia.cadastrobase.persistence.UsuarioEntity;
import com.example.efficientia.cadastrobase.persistence.UsuarioRepository;
import com.example.efficientia.empresa.domain.EmpresaAdmin;
import com.example.efficientia.empresa.persistence.EmpresaAdminRepository;
import com.example.efficientia.empresa.service.EmpresaAdminService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Locale;

@Service
public class CodigoLoginService {
    private final JdbcTemplate jdbc;
    private final JavaMailSender mail;
    private final UsuarioRepository usuarios;
    private final EmpresaAdminRepository admins;
    private final AuthService authService;
    private final EmpresaAdminService adminService;
    private final String remetente;
    private final String segredo;
    private final SecureRandom random = new SecureRandom();

    public CodigoLoginService(JdbcTemplate jdbc, JavaMailSender mail,
                              UsuarioRepository usuarios, EmpresaAdminRepository admins,
                              AuthService authService, EmpresaAdminService adminService,
                              @Value("${app.mail.from:}") String remetente,
                              @Value("${app.security.secret}") String segredo) {
        this.jdbc = jdbc;
        this.mail = mail;
        this.usuarios = usuarios;
        this.admins = admins;
        this.authService = authService;
        this.adminService = adminService;
        this.remetente = remetente;
        this.segredo = segredo;
    }

    @Transactional
    public void solicitar(String emailInformado, String perfil) {
        String email = emailInformado.trim().toLowerCase(Locale.ROOT);
        if (remetente == null || remetente.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Envio de e-mail não configurado.");
        }
        if (!existeContaAtiva(email, perfil)) return;

        Boolean permitido = jdbc.query("SELECT proximo_envio_em <= now() FROM sc_corporativo.tb_codigo_login_email "
                + "WHERE email = ? AND perfil = ?", rs -> rs.next() ? rs.getBoolean(1) : true, email, perfil);
        if (!permitido) throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                "Aguarde um minuto para solicitar outro código.");

        String codigo = String.format(Locale.ROOT, "%06d", random.nextInt(1_000_000));
        jdbc.update("INSERT INTO sc_corporativo.tb_codigo_login_email "
                        + "(email, perfil, codigo_hash, expira_em, proximo_envio_em, tentativas) "
                        + "VALUES (?, ?, ?, now() + interval '10 minutes', now() + interval '1 minute', 0) "
                        + "ON CONFLICT (email, perfil) DO UPDATE SET codigo_hash = EXCLUDED.codigo_hash, "
                        + "expira_em = EXCLUDED.expira_em, proximo_envio_em = EXCLUDED.proximo_envio_em, tentativas = 0",
                email, perfil, hash(email, perfil, codigo));

        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setFrom(remetente);
        mensagem.setTo(email);
        mensagem.setSubject("Seu código de acesso Efficientia");
        mensagem.setText("Seu código de acesso é " + codigo + ". Ele expira em 10 minutos. "
                + "Se você não solicitou este acesso, ignore esta mensagem.");
        try { mail.send(mensagem); }
        catch (RuntimeException erro) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Não foi possível enviar o e-mail agora.", erro);
        }
    }

    @Transactional(noRollbackFor = AutenticacaoInvalidaException.class)
    public Object validar(String emailInformado, String perfil, String codigo) {
        String email = emailInformado.trim().toLowerCase(Locale.ROOT);
        String esperado = jdbc.query("SELECT codigo_hash FROM sc_corporativo.tb_codigo_login_email "
                + "WHERE email = ? AND perfil = ? AND expira_em > now() AND tentativas < 5 FOR UPDATE",
                rs -> rs.next() ? rs.getString(1) : null, email, perfil);
        if (esperado == null || !MessageDigest.isEqual(esperado.getBytes(StandardCharsets.UTF_8),
                hash(email, perfil, codigo).getBytes(StandardCharsets.UTF_8))) {
            jdbc.update("UPDATE sc_corporativo.tb_codigo_login_email SET tentativas = tentativas + 1 "
                    + "WHERE email = ? AND perfil = ?", email, perfil);
            throw new AutenticacaoInvalidaException("Código inválido ou expirado.");
        }
        Object resposta;
        if ("motorista".equals(perfil)) {
            UsuarioEntity usuario = usuarios.findByEmail(email)
                    .filter(u -> u.getTipo() == TipoUsuario.motorista && Boolean.TRUE.equals(u.getAtivo()))
                    .orElseThrow(() -> new AutenticacaoInvalidaException("Código inválido ou expirado."));
            resposta = authService.autenticarPorCodigo(usuario);
        } else if ("administrador".equals(perfil)) {
            EmpresaAdmin admin = admins.buscarPorEmail(email)
                    .filter(a -> Boolean.TRUE.equals(a.getAtivo()))
                    .orElseThrow(() -> new AutenticacaoInvalidaException("Código inválido ou expirado."));
            resposta = adminService.autenticarPorCodigo(admin);
        } else throw new AutenticacaoInvalidaException("Código inválido ou expirado.");
        jdbc.update("DELETE FROM sc_corporativo.tb_codigo_login_email WHERE email = ? AND perfil = ?", email, perfil);
        return resposta;
    }

    private boolean existeContaAtiva(String email, String perfil) {
        if ("motorista".equals(perfil)) return usuarios.findByEmail(email)
                .map(u -> u.getTipo() == TipoUsuario.motorista && Boolean.TRUE.equals(u.getAtivo())).orElse(false);
        if ("administrador".equals(perfil)) return admins.buscarPorEmail(email)
                .map(a -> Boolean.TRUE.equals(a.getAtivo())).orElse(false);
        return false;
    }

    private String hash(String email, String perfil, String codigo) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(
                    (segredo + ":" + email + ":" + perfil + ":" + codigo).getBytes(StandardCharsets.UTF_8)));
        } catch (Exception erro) { throw new IllegalStateException("SHA-256 indisponível", erro); }
    }
}
