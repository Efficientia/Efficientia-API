package com.example.efficientia.exportacao.persistence;

import com.example.efficientia.exportacao.domain.EstadoExportacao;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "exportacao",
        schema = "public",
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_exportacao_idempotency_key", columnNames = "idempotency_key")
        },
        indexes = {
                @Index(name = "idx_exportacao_estado_criado", columnList = "estado, criado_em"),
                @Index(name = "idx_exportacao_expira_em", columnList = "expira_em")
        }
)
public class ExportacaoEntity {

    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    @Column(name = "idempotency_key", nullable = false, updatable = false)
    private UUID idempotencyKey;

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "exportacao_documento",
            schema = "public",
            joinColumns = @JoinColumn(
                    name = "exportacao_id",
                    nullable = false,
                    foreignKey = @ForeignKey(name = "fk_exportacao_documento_exportacao")
            )
    )
    @OrderColumn(name = "posicao", nullable = false)
    @Column(name = "documento_id", nullable = false)
    private List<UUID> documentoIds = new ArrayList<>();

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EstadoExportacao estado;

    @Column(name = "solicitado_por")
    private Integer solicitadoPor;

    @Column(name = "tamanho_origem_bytes", nullable = false)
    private long tamanhoOrigemBytes;

    @Column(name = "tamanho_zip_bytes")
    private Long tamanhoZipBytes;

    @Column(name = "storage_key", length = 500)
    private String storageKey;

    @Column(name = "nome_arquivo", length = 255)
    private String nomeArquivo;

    @Column(name = "erro_codigo", length = 100)
    private String erroCodigo;

    @Column(name = "criado_em", nullable = false, updatable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Column(name = "iniciado_em")
    private Instant iniciadoEm;

    @Column(name = "concluido_em")
    private Instant concluidoEm;

    @Column(name = "expira_em")
    private Instant expiraEm;

    @Version
    @Column(nullable = false)
    private Long versao;

    public ExportacaoEntity() {
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

    public UUID getId() { return id; }
    public UUID getIdempotencyKey() { return idempotencyKey; }
    public List<UUID> getDocumentoIds() { return documentoIds; }
    public EstadoExportacao getEstado() { return estado; }
    public Integer getSolicitadoPor() { return solicitadoPor; }
    public long getTamanhoOrigemBytes() { return tamanhoOrigemBytes; }
    public Long getTamanhoZipBytes() { return tamanhoZipBytes; }
    public String getStorageKey() { return storageKey; }
    public String getNomeArquivo() { return nomeArquivo; }
    public String getErroCodigo() { return erroCodigo; }
    public Instant getCriadoEm() { return criadoEm; }
    public Instant getAtualizadoEm() { return atualizadoEm; }
    public Instant getIniciadoEm() { return iniciadoEm; }
    public Instant getConcluidoEm() { return concluidoEm; }
    public Instant getExpiraEm() { return expiraEm; }
    public Long getVersao() { return versao; }

    public void setId(UUID id) { this.id = id; }
    public void setIdempotencyKey(UUID idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public void setDocumentoIds(List<UUID> documentoIds) { this.documentoIds = new ArrayList<>(documentoIds); }
    public void setEstado(EstadoExportacao estado) { this.estado = estado; }
    public void setSolicitadoPor(Integer solicitadoPor) { this.solicitadoPor = solicitadoPor; }
    public void setTamanhoOrigemBytes(long tamanhoOrigemBytes) { this.tamanhoOrigemBytes = tamanhoOrigemBytes; }
    public void setTamanhoZipBytes(Long tamanhoZipBytes) { this.tamanhoZipBytes = tamanhoZipBytes; }
    public void setStorageKey(String storageKey) { this.storageKey = storageKey; }
    public void setNomeArquivo(String nomeArquivo) { this.nomeArquivo = nomeArquivo; }
    public void setErroCodigo(String erroCodigo) { this.erroCodigo = erroCodigo; }
    public void setCriadoEm(Instant criadoEm) { this.criadoEm = criadoEm; }
    public void setAtualizadoEm(Instant atualizadoEm) { this.atualizadoEm = atualizadoEm; }
    public void setIniciadoEm(Instant iniciadoEm) { this.iniciadoEm = iniciadoEm; }
    public void setConcluidoEm(Instant concluidoEm) { this.concluidoEm = concluidoEm; }
    public void setExpiraEm(Instant expiraEm) { this.expiraEm = expiraEm; }
    public void setVersao(Long versao) { this.versao = versao; }
}
