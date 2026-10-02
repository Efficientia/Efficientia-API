package com.example.efficientia.assinaturamotorista.service;

import com.example.efficientia.assinaturamotorista.api.AssinaturaFormatoInvalidoException;
import com.example.efficientia.assinaturamotorista.api.AssinaturaMotoristaContracts.AssinaturaMetadadosRequest;
import com.example.efficientia.assinaturamotorista.api.AssinaturaMotoristaContracts.AssinaturaMotoristaResponse;
import com.example.efficientia.assinaturamotorista.api.AssinaturaNaoEncontradaException;
import com.example.efficientia.assinaturamotorista.api.MotoristaInvalidoException;
import com.example.efficientia.assinaturamotorista.persistence.AssinaturaMotoristaEntity;
import com.example.efficientia.assinaturamotorista.persistence.AssinaturaMotoristaRepository;
import com.example.efficientia.assinaturamotorista.validation.PngSignatureValidator;
import com.example.efficientia.assinaturamotorista.validation.PngSignatureValidator.ValidatedPng;
import com.example.efficientia.cadastrobase.domain.TipoUsuario;
import com.example.efficientia.cadastrobase.persistence.UsuarioEntity;
import com.example.efficientia.cadastrobase.persistence.UsuarioRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AssinaturaMotoristaService {

    private final AssinaturaMotoristaRepository assinaturaRepository;
    private final UsuarioRepository usuarioRepository;

    public AssinaturaMotoristaService(
            AssinaturaMotoristaRepository assinaturaRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.assinaturaRepository = assinaturaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public AssinaturaMotoristaResponse salvarOuAtualizarAssinatura(
            Integer motoristaId,
            Integer criadoPorId,
            UUID idempotencyKey,
            AssinaturaMetadadosRequest metadados,
            byte[] arquivoBytes,
            boolean isMe
    ) {
        if (idempotencyKey == null) {
            throw new AssinaturaFormatoInvalidoException("O cabeçalho Idempotency-Key é obrigatório.");
        }

        // 1. Respeitar Idempotency-Key para evitar duplicidade em redes móveis instáveis
        Optional<AssinaturaMotoristaEntity> existenteOpt = assinaturaRepository.findByIdempotencyKey(idempotencyKey);
        if (existenteOpt.isPresent()) {
            return AssinaturaMotoristaResponse.fromEntity(existenteOpt.get(), isMe);
        }

        // 2. Validar que o usuário informado existe e possui o papel de motorista
        UsuarioEntity motorista = usuarioRepository.findById(motoristaId)
                .orElseThrow(() -> new AssinaturaNaoEncontradaException("Motorista não encontrado com id: " + motoristaId));

        if (motorista.getTipo() != TipoUsuario.motorista) {
            throw new MotoristaInvalidoException("O usuário id " + motoristaId + " não possui o perfil de motorista.");
        }

        // 3. Validar assinatura binária do arquivo PNG, tamanho e calcular SHA-256
        if (metadados == null || metadados.modalidade() == null) {
            throw new AssinaturaFormatoInvalidoException("Os metadados da assinatura são obrigatórios.");
        }

        ValidatedPng validated = PngSignatureValidator.validate(arquivoBytes);

        // 4. Substituição transacional da assinatura ativa (manter versões anteriores imutáveis)
        Optional<AssinaturaMotoristaEntity> ativaOpt = assinaturaRepository.findByMotoristaIdAndAtivaTrue(motoristaId);
        long proximaVersao = 0L;
        if (ativaOpt.isPresent()) {
            AssinaturaMotoristaEntity ativaAnterior = ativaOpt.get();
            ativaAnterior.setAtiva(false);
            ativaAnterior.setAtualizadoEm(Instant.now());
            assinaturaRepository.save(ativaAnterior);
            proximaVersao = (ativaAnterior.getVersao() != null ? ativaAnterior.getVersao() : 0L) + 1L;
        }

        // 5. Inserir nova versão ativa
        AssinaturaMotoristaEntity nova = new AssinaturaMotoristaEntity();
        nova.setId(UUID.randomUUID());
        nova.setMotoristaId(motoristaId);
        nova.setModalidade(metadados.modalidade().name());
        nova.setTextoOrigem(metadados.textoOrigem());
        nova.setMimeType("image/png");
        nova.setConteudo(validated.bytes());
        nova.setTamanhoBytes(validated.tamanhoBytes());
        nova.setSha256(validated.sha256());
        nova.setIdempotencyKey(idempotencyKey);
        nova.setAtiva(true);
        nova.setCriadoPor(criadoPorId != null ? criadoPorId : motoristaId);
        nova.setCriadoEm(Instant.now());
        nova.setAtualizadoEm(Instant.now());
        nova.setVersao(proximaVersao);

        AssinaturaMotoristaEntity salva = assinaturaRepository.save(nova);
        return AssinaturaMotoristaResponse.fromEntity(salva, isMe);
    }

    @Transactional(readOnly = true)
    public AssinaturaMotoristaResponse buscarAssinaturaAtiva(Integer motoristaId, boolean isMe) {
        if (!usuarioRepository.existsById(motoristaId)) {
            throw new AssinaturaNaoEncontradaException("Motorista não encontrado com id: " + motoristaId);
        }

        AssinaturaMotoristaEntity entity = assinaturaRepository.findByMotoristaIdAndAtivaTrue(motoristaId)
                .orElseThrow(() -> new AssinaturaNaoEncontradaException("Nenhuma assinatura fixa cadastrada para o motorista " + motoristaId));

        return AssinaturaMotoristaResponse.fromEntity(entity, isMe);
    }

    @Transactional(readOnly = true)
    public byte[] buscarConteudoAssinaturaAtiva(Integer motoristaId) {
        if (!usuarioRepository.existsById(motoristaId)) {
            throw new AssinaturaNaoEncontradaException("Motorista não encontrado com id: " + motoristaId);
        }

        AssinaturaMotoristaEntity entity = assinaturaRepository.findByMotoristaIdAndAtivaTrue(motoristaId)
                .orElseThrow(() -> new AssinaturaNaoEncontradaException("Nenhuma assinatura fixa cadastrada para o motorista " + motoristaId));

        return entity.getConteudo();
    }

    @Transactional(readOnly = true)
    public Optional<AssinaturaMotoristaEntity> buscarAtivaPorMotoristaId(Integer motoristaId) {
        return assinaturaRepository.findByMotoristaIdAndAtivaTrue(motoristaId);
    }

    @Transactional(readOnly = true)
    public Optional<AssinaturaMotoristaEntity> buscarPorId(UUID id) {
        return assinaturaRepository.findById(id);
    }
}
