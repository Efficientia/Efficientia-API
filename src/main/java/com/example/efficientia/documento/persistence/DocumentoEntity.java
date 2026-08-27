package com.example.efficientia.documento.persistence;

import com.example.efficientia.documento.domain.ModalidadeAssinatura;
import com.example.efficientia.documento.domain.OrigemDocumento;
import com.example.efficientia.documento.domain.PapelAssinante;
import com.example.efficientia.documento.domain.TipoDocumento;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(
        name = "documento",
        schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_documento_storage_key", columnNames = "storage_key"),
                @UniqueConstraint(name = "uq_documento_idempotency_key", columnNames = "idempotency_key")
        },
        indexes = {
                @Index(name = "idx_documento_viagem_criado", columnList = "viagem_id, criado_em"),
                @Index(name = "idx_documento_assinante_criado", columnList = "assinante_id, criado_em"),
                @Index(name = "idx_documento_cursor", columnList = "criado_em, id"),
                @Index(name = "idx_documento_sha256", columnList = "sha256")
        }
)
public class DocumentoEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "viagem_id", nullable = false)
    private Integer viagemId;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_documento", nullable = false, length = 40)
    private TipoDocumento tipoDocumento;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OrigemDocumento origem;

    @Column(name = "assinante_id")
    private Integer assinanteId;

    @Enumerated(EnumType.STRING)
    @Column(name = "papel_assinante", length = 30)
    private PapelAssinante papelAssinante;

    @Enumerated(EnumType.STRING)
    @Column(name = "modalidade_assinatura", length = 20)
    private ModalidadeAssinatura modalidadeAssinatura;

    @Column(name = "texto_assinatura", length = 150)
    private String textoAssinatura;

    @Column(length = 300)
    private String descricao;

    @Column(name = "nome_original", length = 255)
    private String nomeOriginal;

    @Column(name = "mime_type", length = 50)
    private String mimeType;

    @Column(name = "tamanho_bytes")
    private Long tamanhoBytes;

    @JdbcTypeCode(SqlTypes.CHAR)
    @Column(length = 64, columnDefinition = "CHAR(64)")
    private String sha256;

    @Column(name = "storage_key", length = 500)
    private String storageKey;

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private UUID idempotencyKey;

    @Column(name = "criado_por")
    private Integer criadoPor;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Version
    @Column(nullable = false)
    private Long versao;

    public DocumentoEntity() {
    }

    @PrePersist
    void antesDePersistir() {
        Instant agora = Instant.now();
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (criadoEm == null) {
            criadoEm = agora;
        }
        if (atualizadoEm == null) {
            atualizadoEm = criadoEm;
        }
    }

    @PreUpdate
    void antesDeAtualizar() {
        atualizadoEm = Instant.now();
    }

    public boolean temArquivo() {
        return storageKey != null;
    }

    public boolean ehAssinaturaTextual() {
        return tipoDocumento == TipoDocumento.ASSINATURA
                && modalidadeAssinatura == ModalidadeAssinatura.TEXTO;
    }

    public UUID getId() { return id; }
    public Integer getViagemId() { return viagemId; }
    public TipoDocumento getTipoDocumento() { return tipoDocumento; }
    public OrigemDocumento getOrigem() { return origem; }
    public Integer getAssinanteId() { return assinanteId; }
    public PapelAssinante getPapelAssinante() { return papelAssinante; }
    public ModalidadeAssinatura getModalidadeAssinatura() { return modalidadeAssinatura; }
    public String getTextoAssinatura() { return textoAssinatura; }
    public String getDescricao() { return descricao; }
    public String getNomeOriginal() { return nomeOriginal; }
    public String getMimeType() { return mimeType; }
    public Long getTamanhoBytes() { return tamanhoBytes; }
    public String getSha256() { return sha256; }
    public String getStorageKey() { return storageKey; }
    public UUID getIdempotencyKey() { return idempotencyKey; }
    public Integer getCriadoPor() { return criadoPor; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
    public Long getVersao() { return versao; }

    public void setViagemId(Integer viagemId) { this.viagemId = viagemId; }
    public void setTipoDocumento(TipoDocumento tipoDocumento) { this.tipoDocumento = tipoDocumento; }
    public void setOrigem(OrigemDocumento origem) { this.origem = origem; }
    public void setAssinanteId(Integer assinanteId) { this.assinanteId = assinanteId; }
    public void setPapelAssinante(PapelAssinante papelAssinante) { this.papelAssinante = papelAssinante; }
    public void setModalidadeAssinatura(ModalidadeAssinatura modalidadeAssinatura) {
        this.modalidadeAssinatura = modalidadeAssinatura;
    }
    public void setTextoAssinatura(String textoAssinatura) { this.textoAssinatura = textoAssinatura; }
    public void setDescricao(String descricao) { this.descricao = descricao; }
    public void setNomeOriginal(String nomeOriginal) { this.nomeOriginal = nomeOriginal; }
    public void setMimeType(String mimeType) { this.mimeType = mimeType; }
    public void setTamanhoBytes(Long tamanhoBytes) { this.tamanhoBytes = tamanhoBytes; }
    public void setSha256(String sha256) { this.sha256 = sha256; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public void setIdempotencyKey(UUID idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public void setCriadoPor(Integer criadoPor) { this.criadoPor = criadoPor; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
    public void setAtualizadoEm(Instant atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}
