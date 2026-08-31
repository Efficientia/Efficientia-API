package com.example.efficientia.documento.audit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "documento_auditoria", schema = "public")
public class DocumentoAuditEntity {

    @Id
    private UUID id;

    @Column(name = "documento_id", nullable = false)
    private UUID documentoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private DocumentoAuditOperation operacao;

    @Column(name = "usuario_id")
    private Integer usuarioId;

    @Column(length = 500)
    private String detalhes;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @PrePersist
    void antesDePersistir() {
        if (id == null) id = UUID.randomUUID();
        if (criadoEm == null) criadoEm = Instant.now();
    }

    public UUID getId() { return id; }
    public UUID getDocumentoId() { return documentoId; }
    public DocumentoAuditOperation getOperacao() { return operacao; }
    public Integer getUsuarioId() { return usuarioId; }
    public String getDetalhes() { return detalhes; }
    public Instant getCriadoEm() { return criadoEm; }

    public void setId(UUID id) { this.id = id; }
    public void setDocumentoId(UUID documentoId) { this.documentoId = documentoId; }
    public void setOperacao(DocumentoAuditOperation operacao) { this.operacao = operacao; }
    public void setUsuarioId(Integer usuarioId) { this.usuarioId = usuarioId; }
    public void setDetalhes(String detalhes) { this.detalhes = detalhes; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
}
