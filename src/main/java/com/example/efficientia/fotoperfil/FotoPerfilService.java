package com.example.efficientia.fotoperfil;

import com.example.efficientia.cadastrobase.domain.TipoUsuario;
import com.example.efficientia.cadastrobase.persistence.UsuarioEntity;
import com.example.efficientia.cadastrobase.persistence.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Arrays;

@Service
public class FotoPerfilService {
    static final int MAX_BYTES = 2 * 1024 * 1024;
    private final UsuarioRepository usuarios;

    public FotoPerfilService(UsuarioRepository usuarios) {
        this.usuarios = usuarios;
    }

    @Transactional
    public void salvar(Integer usuarioId, byte[] conteudo, String mime) {
        if (conteudo == null || conteudo.length == 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A foto é obrigatória.");
        }
        if (conteudo.length > MAX_BYTES) {
            throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "A foto deve ter até 2 MB.");
        }
        String formato = validarFormato(conteudo, mime);
        UsuarioEntity usuario = motorista(usuarioId);
        usuario.setFotoPerfil(Arrays.copyOf(conteudo, conteudo.length));
        usuario.setFotoPerfilMime(formato);
        usuario.setFotoPerfilAtualizadaEm(Instant.now());
        usuarios.save(usuario);
    }

    @Transactional(readOnly = true)
    public FotoConteudo buscar(Integer usuarioId) {
        UsuarioEntity usuario = motorista(usuarioId);
        if (usuario.getFotoPerfil() == null || usuario.getFotoPerfil().length == 0) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Foto de perfil não cadastrada.");
        }
        return new FotoConteudo(usuario.getFotoPerfilMime(), usuario.getFotoPerfil());
    }

    @Transactional
    public void remover(Integer usuarioId) {
        UsuarioEntity usuario = motorista(usuarioId);
        usuario.setFotoPerfil(null);
        usuario.setFotoPerfilMime(null);
        usuario.setFotoPerfilAtualizadaEm(Instant.now());
        usuarios.save(usuario);
    }

    private UsuarioEntity motorista(Integer usuarioId) {
        UsuarioEntity usuario = usuarios.findById(usuarioId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Motorista não encontrado."));
        if (usuario.getTipo() != TipoUsuario.motorista) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Acesso restrito a motoristas.");
        }
        return usuario;
    }

    private String validarFormato(byte[] bytes, String mime) {
        boolean png = bytes.length >= 8 && bytes[0] == (byte) 0x89 && bytes[1] == 0x50
                && bytes[2] == 0x4e && bytes[3] == 0x47 && bytes[4] == 0x0d
                && bytes[5] == 0x0a && bytes[6] == 0x1a && bytes[7] == 0x0a;
        boolean jpeg = bytes.length >= 4 && bytes[0] == (byte) 0xff
                && bytes[1] == (byte) 0xd8 && bytes[2] == (byte) 0xff
                && bytes[bytes.length - 2] == (byte) 0xff && bytes[bytes.length - 1] == (byte) 0xd9;
        if (png && "image/png".equalsIgnoreCase(mime)) return "image/png";
        if (jpeg && "image/jpeg".equalsIgnoreCase(mime)) return "image/jpeg";
        throw new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Envie um JPEG ou PNG válido.");
    }

    public record FotoConteudo(String mime, byte[] bytes) { }
}
