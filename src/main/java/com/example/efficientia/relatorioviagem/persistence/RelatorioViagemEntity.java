package com.example.efficientia.relatorioviagem.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Duration;
import java.util.UUID;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;

@Entity
@Table(name = "relatorio_viagem", schema = "public")
public class RelatorioViagemEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "empresa_id")
    private Integer empresaId;

    @Column(name = "status", length = 20)
    private String status = "rascunho";

    @Column(name = "enviado_em")
    private LocalDateTime enviadoEm;

    @Column(name = "finalizado_em")
    private LocalDateTime finalizadoEm;

    @Column(name = "unidade_frigorifica_id")
    private Integer unidadeFrigorificaId;

    @Column(name = "idempotency_key")
    private UUID idempotencyKey;

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm;

    @Column(name = "fazenda_id", nullable = false)
    private Integer fazendaId;

    @Column(name = "motorista_id", nullable = false)
    private Integer motoristaId;

    @Column(name = "manobrista_id", nullable = false)
    private Integer manobristaId;

    @Column(name = "curraleiro_id", nullable = false)
    private Integer curraleiroId;

    @Column(name = "cavalo_id", nullable = false)
    private Integer cavaloId;

    @Column(name = "carreta_id", nullable = false)
    private Integer carretaId;

    @Column(name = "numero_gta", nullable = false, unique = true, length = 50)
    private String numeroGta;

    @Column(name = "numero_nota_fiscal", nullable = false, length = 50)
    private String numeroNotaFiscal;

    @Column(name = "data_embarque", nullable = false)
    private LocalDate dataEmbarque;

    @Column(name = "horario_embarque", nullable = false)
    private LocalTime horarioEmbarque;

    @Column(name = "horario_saida_propriedade", nullable = false)
    private LocalTime horarioSaidaPropriedade;

    @Column(name = "km_saida_embarcadouro", nullable = false)
    private Integer kmSaidaEmbarcadouro;

    @Column(name = "data_chegada_unidade", nullable = false)
    private LocalDate dataChegadaUnidade;

    @Column(name = "horario_chegada_unidade", nullable = false)
    private LocalTime horarioChegadaUnidade;

    @Column(name = "horario_desembarque", nullable = false)
    private LocalTime horarioDesembarque;

    @Column(name = "km_chegada_desembarcadouro", nullable = false)
    private Integer kmChegadaDesembarcadouro;

    @Column(name = "numero_curral", nullable = false, length = 20)
    private String numeroCurral;

    @Column(name = "sirene_re_funcionou", nullable = false)
    private Boolean sireneReFuncionou;

    @Column(name = "qtd_machos", nullable = false)
    private Integer quantidadeMachos;

    @Column(name = "qtd_femeas", nullable = false)
    private Integer quantidadeFemeas;

    @Column(name = "qtd_marrucos", nullable = false)
    private Integer quantidadeMarrucos;

    @Column(name = "qtd_em_pe", nullable = false)
    private Integer quantidadeEmPe;

    @Column(name = "qtd_deitado", nullable = false)
    private Integer quantidadeDeitado;

    @Column(name = "qtd_morto", nullable = false)
    private Integer quantidadeMorto;

    @Column(name = "qtd_emergencia", nullable = false)
    private Integer quantidadeEmergencia;

    @Column(name = "motivo_emergencia")
    private String motivoEmergencia;

    @Column(name = "comentarios")
    private String comentarios;

    @Column(name = "url_assinatura_pecuarista", columnDefinition = "TEXT")
    private String urlAssinaturaPecuarista;

    @Column(name = "url_assinatura_motorista", columnDefinition = "TEXT")
    private String urlAssinaturaMotorista;

    @Column(name = "url_assinatura_manobrista", columnDefinition = "TEXT")
    private String urlAssinaturaManobrista;

    @Column(name = "url_assinatura_curraleiro", columnDefinition = "TEXT")
    private String urlAssinaturaCurraleiro;

    @Column(name = "capacidade_carga_utilizada")
    private Integer capacidadeCargaUtilizada;

    @Column(name = "url_laudo_mortalidade", columnDefinition = "TEXT")
    private String urlLaudoMortalidade;
    @Column(name = "criado_em")
    private LocalDateTime criadoEm;

    public RelatorioViagemEntity() {
    }

    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Integer getFazendaId() { return fazendaId; }
    public Integer getMotoristaId() { return motoristaId; }
    public Integer getManobristaId() { return manobristaId; }
    public Integer getCurraleiroId() { return curraleiroId; }
    public Integer getCavaloId() { return cavaloId; }
    public Integer getCarretaId() { return carretaId; }
    public String getNumeroGta() { return numeroGta; }
    public String getNumeroNotaFiscal() { return numeroNotaFiscal; }
    public LocalDate getDataEmbarque() { return dataEmbarque; }
    public LocalTime getHorarioEmbarque() { return horarioEmbarque; }
    public LocalTime getHorarioSaidaPropriedade() { return horarioSaidaPropriedade; }
    public Integer getKmSaidaEmbarcadouro() { return kmSaidaEmbarcadouro; }
    public LocalDate getDataChegadaUnidade() { return dataChegadaUnidade; }
    public LocalTime getHorarioChegadaUnidade() { return horarioChegadaUnidade; }
    public LocalTime getHorarioDesembarque() { return horarioDesembarque; }
    public Integer getKmChegadaDesembarcadouro() { return kmChegadaDesembarcadouro; }
    public String getNumeroCurral() { return numeroCurral; }
    public Boolean getSireneReFuncionou() { return sireneReFuncionou; }
    public Integer getQuantidadeMachos() { return quantidadeMachos; }
    public Integer getQuantidadeFemeas() { return quantidadeFemeas; }
    public Integer getQuantidadeMarrucos() { return quantidadeMarrucos; }
    public Integer getQuantidadeEmPe() { return quantidadeEmPe; }
    public Integer getQuantidadeDeitado() { return quantidadeDeitado; }
    public Integer getQuantidadeMorto() { return quantidadeMorto; }
    public Integer getQuantidadeEmergencia() { return quantidadeEmergencia; }
    public String getMotivoEmergencia() { return motivoEmergencia; }
    public String getComentarios() { return comentarios; }
    public String getUrlAssinaturaPecuarista() { return urlAssinaturaPecuarista; }
    public String getUrlAssinaturaMotorista() { return urlAssinaturaMotorista; }
    public String getUrlAssinaturaManobrista() { return urlAssinaturaManobrista; }
    public String getUrlAssinaturaCurraleiro() { return urlAssinaturaCurraleiro; }
    public LocalDateTime getCriadoEm() { return criadoEm; }

    public void setFazendaId(Integer fazendaId) { this.fazendaId = fazendaId; }
    public void setMotoristaId(Integer motoristaId) { this.motoristaId = motoristaId; }
    public void setManobristaId(Integer manobristaId) { this.manobristaId = manobristaId; }
    public void setCurraleiroId(Integer curraleiroId) { this.curraleiroId = curraleiroId; }
    public void setCavaloId(Integer cavaloId) { this.cavaloId = cavaloId; }
    public void setCarretaId(Integer carretaId) { this.carretaId = carretaId; }
    public void setNumeroGta(String numeroGta) { this.numeroGta = numeroGta; }
    public void setNumeroNotaFiscal(String numeroNotaFiscal) { this.numeroNotaFiscal = numeroNotaFiscal; }
    public void setDataEmbarque(LocalDate dataEmbarque) { this.dataEmbarque = dataEmbarque; }
    public void setHorarioEmbarque(LocalTime horarioEmbarque) { this.horarioEmbarque = horarioEmbarque; }
    public void setHorarioSaidaPropriedade(LocalTime horarioSaidaPropriedade) {
        this.horarioSaidaPropriedade = horarioSaidaPropriedade;
    }
    public void setKmSaidaEmbarcadouro(Integer kmSaidaEmbarcadouro) {
        this.kmSaidaEmbarcadouro = kmSaidaEmbarcadouro;
    }
    public void setDataChegadaUnidade(LocalDate dataChegadaUnidade) {
        this.dataChegadaUnidade = dataChegadaUnidade;
    }
    public void setHorarioChegadaUnidade(LocalTime horarioChegadaUnidade) {
        this.horarioChegadaUnidade = horarioChegadaUnidade;
    }
    public void setHorarioDesembarque(LocalTime horarioDesembarque) {
        this.horarioDesembarque = horarioDesembarque;
    }
    public void setKmChegadaDesembarcadouro(Integer kmChegadaDesembarcadouro) {
        this.kmChegadaDesembarcadouro = kmChegadaDesembarcadouro;
    }
    public void setNumeroCurral(String numeroCurral) { this.numeroCurral = numeroCurral; }
    public void setSireneReFuncionou(Boolean sireneReFuncionou) {
        this.sireneReFuncionou = sireneReFuncionou;
    }
    public void setQuantidadeMachos(Integer quantidadeMachos) { this.quantidadeMachos = quantidadeMachos; }
    public void setQuantidadeFemeas(Integer quantidadeFemeas) { this.quantidadeFemeas = quantidadeFemeas; }
    public void setQuantidadeMarrucos(Integer quantidadeMarrucos) { this.quantidadeMarrucos = quantidadeMarrucos; }
    public void setQuantidadeEmPe(Integer quantidadeEmPe) { this.quantidadeEmPe = quantidadeEmPe; }
    public void setQuantidadeDeitado(Integer quantidadeDeitado) { this.quantidadeDeitado = quantidadeDeitado; }
    public void setQuantidadeMorto(Integer quantidadeMorto) { this.quantidadeMorto = quantidadeMorto; }
    public void setQuantidadeEmergencia(Integer quantidadeEmergencia) {
        this.quantidadeEmergencia = quantidadeEmergencia;
    }
    public void setMotivoEmergencia(String motivoEmergencia) { this.motivoEmergencia = motivoEmergencia; }
    public void setComentarios(String comentarios) { this.comentarios = comentarios; }
    public void setUrlAssinaturaPecuarista(String urlAssinaturaPecuarista) {
        this.urlAssinaturaPecuarista = urlAssinaturaPecuarista;
    }
    public void setUrlAssinaturaMotorista(String urlAssinaturaMotorista) {
        this.urlAssinaturaMotorista = urlAssinaturaMotorista;
    }
    public void setUrlAssinaturaManobrista(String urlAssinaturaManobrista) {
        this.urlAssinaturaManobrista = urlAssinaturaManobrista;
    }
    public void setUrlAssinaturaCurraleiro(String urlAssinaturaCurraleiro) {
        this.urlAssinaturaCurraleiro = urlAssinaturaCurraleiro;
    }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public Integer getEmpresaId() { return empresaId; }
    public void setEmpresaId(Integer empresaId) { this.empresaId = empresaId; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public LocalDateTime getEnviadoEm() { return enviadoEm; }
    public void setEnviadoEm(LocalDateTime enviadoEm) { this.enviadoEm = enviadoEm; }
    public LocalDateTime getFinalizadoEm() { return finalizadoEm; }
    public void setFinalizadoEm(LocalDateTime finalizadoEm) { this.finalizadoEm = finalizadoEm; }
    public Integer getCapacidadeCargaUtilizada() { return capacidadeCargaUtilizada; }
    public void setCapacidadeCargaUtilizada(Integer capacidadeCargaUtilizada) {
        this.capacidadeCargaUtilizada = capacidadeCargaUtilizada;
    }
    public String getUrlLaudoMortalidade() { return urlLaudoMortalidade; }
    public void setUrlLaudoMortalidade(String urlLaudoMortalidade) {
        this.urlLaudoMortalidade = urlLaudoMortalidade;
    }

    public int contarAssinaturasPreenchidas() {
        int count = 0;
        if (urlAssinaturaPecuarista != null && !urlAssinaturaPecuarista.isBlank()) count++;
        if (urlAssinaturaMotorista != null && !urlAssinaturaMotorista.isBlank()) count++;
        if (urlAssinaturaManobrista != null && !urlAssinaturaManobrista.isBlank()) count++;
        if (urlAssinaturaCurraleiro != null && !urlAssinaturaCurraleiro.isBlank()) count++;
        return count;
    }

    public boolean possuiAssinaturasCompletas(int qtdObrigatoria) {
        return contarAssinaturasPreenchidas() >= qtdObrigatoria;
    }

    public Integer getUnidadeFrigorificaId() { return unidadeFrigorificaId; }
    public void setUnidadeFrigorificaId(Integer unidadeFrigorificaId) { this.unidadeFrigorificaId = unidadeFrigorificaId; }

    public UUID getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(UUID idempotencyKey) { this.idempotencyKey = idempotencyKey; }

    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }

    @PrePersist
    public void prePersist() {
        if (criadoEm == null) {
            criadoEm = LocalDateTime.now();
        }
        atualizadoEm = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        atualizadoEm = LocalDateTime.now();
    }

    public Integer getTotalAnimais() {
        int m = quantidadeMachos != null ? quantidadeMachos : 0;
        int f = quantidadeFemeas != null ? quantidadeFemeas : 0;
        int r = quantidadeMarrucos != null ? quantidadeMarrucos : 0;
        return m + f + r;
    }

    public Integer getDistanciaPercorridaKm() {
        if (kmChegadaDesembarcadouro != null && kmSaidaEmbarcadouro != null && kmChegadaDesembarcadouro >= kmSaidaEmbarcadouro) {
            return kmChegadaDesembarcadouro - kmSaidaEmbarcadouro;
        }
        return null;
    }

    public Long getDuracaoViagemMinutos() {
        LocalTime horarioInicio = horarioSaidaPropriedade != null
                ? horarioSaidaPropriedade : horarioEmbarque;
        if (dataEmbarque == null || horarioInicio == null) {
            return null;
        }
        LocalDateTime inicio = LocalDateTime.of(dataEmbarque, horarioInicio);
        LocalDateTime fim = null;
        if (dataChegadaUnidade != null) {
            if (horarioDesembarque != null) {
                fim = LocalDateTime.of(dataChegadaUnidade, horarioDesembarque);
            } else if (horarioChegadaUnidade != null) {
                fim = LocalDateTime.of(dataChegadaUnidade, horarioChegadaUnidade);
            }
        }
        if (fim != null && !fim.isBefore(inicio)) {
            return Duration.between(inicio, fim).toMinutes();
        }
        return null;
    }
}
