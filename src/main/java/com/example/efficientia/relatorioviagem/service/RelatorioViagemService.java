package com.example.efficientia.relatorioviagem.service;

import com.example.efficientia.assinaturamotorista.persistence.AssinaturaMotoristaRepository;
import com.example.efficientia.cadastrobase.persistence.UsuarioEntity;
import com.example.efficientia.cadastrobase.persistence.UsuarioRepository;
import com.example.efficientia.relatorioviagem.api.AssinaturasIncompletasException;
import com.example.efficientia.relatorioviagem.api.CriarRelatorioViagemRequest;
import com.example.efficientia.relatorioviagem.api.NumeroGtaDuplicadoException;
import com.example.efficientia.relatorioviagem.api.RegistrarAssinaturasRequest;
import com.example.efficientia.relatorioviagem.api.RelatorioViagemNotFoundException;
import com.example.efficientia.relatorioviagem.api.RelatorioViagemPageResponse;
import com.example.efficientia.relatorioviagem.api.RelatorioViagemResponse;
import com.example.efficientia.relatorioviagem.persistence.RelatorioViagemEntity;
import com.example.efficientia.relatorioviagem.persistence.RelatorioViagemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
@Service
public class RelatorioViagemService {
    private static final int TAMANHO_MAXIMO_PAGINA = 100;
    private final RelatorioViagemRepository repository;
    private UsuarioRepository usuarioRepository;
    private AssinaturaMotoristaRepository assinaturaRepository;

    public RelatorioViagemService(RelatorioViagemRepository repository) {
        this.repository = repository;
    }

    @Autowired
    public RelatorioViagemService(
            RelatorioViagemRepository repository,
            @Autowired(required = false) UsuarioRepository usuarioRepository,
            @Autowired(required = false) AssinaturaMotoristaRepository assinaturaRepository
    ) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.assinaturaRepository = assinaturaRepository;
    }
    @Transactional
    public RelatorioViagemResponse criar(CriarRelatorioViagemRequest request) {
        validarRegrasDeNegocio(request);

        String numeroGta = request.numeroGta().trim();
        if (repository.existsByNumeroGta(numeroGta)) {
            throw new NumeroGtaDuplicadoException(numeroGta);
        }

        RelatorioViagemEntity entity = new RelatorioViagemEntity();
        entity.setFazendaId(request.fazendaId());
        entity.setMotoristaId(request.motoristaId());
        entity.setManobristaId(request.manobristaId());
        entity.setCurraleiroId(request.curraleiroId());
        entity.setCavaloId(request.cavaloId());
        entity.setCarretaId(request.carretaId());
        entity.setNumeroGta(numeroGta);
        entity.setNumeroNotaFiscal(request.numeroNotaFiscal().trim());
        entity.setDataEmbarque(request.dataEmbarque());
        entity.setHorarioEmbarque(request.horarioEmbarque());
        entity.setHorarioSaidaPropriedade(request.horarioSaidaPropriedade());
        entity.setKmSaidaEmbarcadouro(request.kmSaidaEmbarcadouro());
        entity.setDataChegadaUnidade(request.dataChegadaUnidade());
        entity.setHorarioChegadaUnidade(request.horarioChegadaUnidade());
        entity.setHorarioDesembarque(request.horarioDesembarque());
        entity.setKmChegadaDesembarcadouro(request.kmChegadaDesembarcadouro());
        entity.setNumeroCurral(request.numeroCurral().trim());
        entity.setSireneReFuncionou(request.sireneReFuncionou());
        entity.setQuantidadeMachos(request.quantidadeMachos());
        entity.setQuantidadeFemeas(request.quantidadeFemeas());
        entity.setQuantidadeMarrucos(request.quantidadeMarrucos());
        entity.setQuantidadeEmPe(request.quantidadeEmPe());
        entity.setQuantidadeDeitado(request.quantidadeDeitado());
        entity.setQuantidadeMorto(request.quantidadeMorto());
        entity.setQuantidadeEmergencia(request.quantidadeEmergencia());
        entity.setMotivoEmergencia(request.motivoEmergencia());
        entity.setComentarios(request.comentarios());
        String urlPecuarista = request.urlAssinaturaPecuarista() != null && !request.urlAssinaturaPecuarista().isBlank()
                ? request.urlAssinaturaPecuarista().trim() : null;
        String urlMotorista = request.urlAssinaturaMotorista() != null && !request.urlAssinaturaMotorista().isBlank()
                ? request.urlAssinaturaMotorista().trim() : null;
        String urlManobrista = request.urlAssinaturaManobrista() != null && !request.urlAssinaturaManobrista().isBlank()
                ? request.urlAssinaturaManobrista().trim() : null;
        String urlCurraleiro = request.urlAssinaturaCurraleiro() != null && !request.urlAssinaturaCurraleiro().isBlank()
                ? request.urlAssinaturaCurraleiro().trim() : null;

        if (urlMotorista == null && request.motoristaId() != null) {
            urlMotorista = buscarAssinaturaFixaMotorista(request.motoristaId());
        }

        entity.setUrlAssinaturaPecuarista(urlPecuarista);
        entity.setUrlAssinaturaMotorista(urlMotorista);
        entity.setUrlAssinaturaManobrista(urlManobrista);
        entity.setUrlAssinaturaCurraleiro(urlCurraleiro);
        entity.setCapacidadeCargaUtilizada(request.capacidadeCargaUtilizada());
        entity.setUrlLaudoMortalidade(request.urlLaudoMortalidade());
        entity.setCriadoEm(LocalDateTime.now());
        entity.setStatus("rascunho");

        return RelatorioViagemResponse.from(repository.save(entity));
    }

    public RelatorioViagemPageResponse listar(int pagina, int tamanho) {
        validarPaginacao(pagina, tamanho);
        var pageable = PageRequest.of(
                pagina, tamanho,
                Sort.by(Sort.Order.desc("dataEmbarque"), Sort.Order.desc("id"))
        );
        return RelatorioViagemPageResponse.from(
                repository.findAll(pageable).map(RelatorioViagemResponse::from)
        );
    }

    public RelatorioViagemResponse buscar(Integer id) {
        return repository.findById(id)
                .map(RelatorioViagemResponse::from)
                .orElseThrow(() -> new RelatorioViagemNotFoundException(id));
    }

    @Transactional
    public RelatorioViagemResponse finalizar(Integer id) {
        RelatorioViagemEntity entity = repository.findById(id)
                .orElseThrow(() -> new RelatorioViagemNotFoundException(id));
        validarAssinaturasObrigatorias(entity);
        return atualizarStatus(id, "aprovado");
    }

    @Transactional
    public RelatorioViagemResponse registrarAssinaturas(Integer id, RegistrarAssinaturasRequest request) {
        if (request == null) {
            throw new IllegalArgumentException("Os dados de assinatura não podem ser nulos.");
        }
        RelatorioViagemEntity entity = repository.findById(id)
                .orElseThrow(() -> new RelatorioViagemNotFoundException(id));

        if (request.urlAssinaturaPecuarista() != null && !request.urlAssinaturaPecuarista().isBlank()) {
            entity.setUrlAssinaturaPecuarista(request.urlAssinaturaPecuarista().trim());
        }

        if (request.urlAssinaturaMotorista() != null && !request.urlAssinaturaMotorista().isBlank()) {
            entity.setUrlAssinaturaMotorista(request.urlAssinaturaMotorista().trim());
        } else if (Boolean.TRUE.equals(request.usarAssinaturaFixaMotorista()) && entity.getMotoristaId() != null) {
            String urlFixa = buscarAssinaturaFixaMotorista(entity.getMotoristaId());
            if (urlFixa != null) {
                entity.setUrlAssinaturaMotorista(urlFixa);
            }
        }

        if (request.urlAssinaturaManobrista() != null && !request.urlAssinaturaManobrista().isBlank()) {
            entity.setUrlAssinaturaManobrista(request.urlAssinaturaManobrista().trim());
        }

        if (request.urlAssinaturaCurraleiro() != null && !request.urlAssinaturaCurraleiro().isBlank()) {
            entity.setUrlAssinaturaCurraleiro(request.urlAssinaturaCurraleiro().trim());
        }

        return RelatorioViagemResponse.from(repository.save(entity));
    }

    @Transactional
    public RelatorioViagemResponse registrarAssinaturaPapel(Integer id, String papel, String urlAssinatura) {
        if (papel == null || papel.isBlank()) {
            throw new IllegalArgumentException("O papel do assinante é obrigatório.");
        }
        RelatorioViagemEntity entity = repository.findById(id)
                .orElseThrow(() -> new RelatorioViagemNotFoundException(id));

        String papelNorm = papel.trim().toUpperCase();
        String url = (urlAssinatura != null && !urlAssinatura.isBlank()) ? urlAssinatura.trim() : null;

        switch (papelNorm) {
            case "MOTORISTA" -> {
                if (url == null && entity.getMotoristaId() != null) {
                    url = buscarAssinaturaFixaMotorista(entity.getMotoristaId());
                }
                entity.setUrlAssinaturaMotorista(url);
            }
            case "PECUARISTA" -> entity.setUrlAssinaturaPecuarista(url);
            case "MANOBRISTA" -> entity.setUrlAssinaturaManobrista(url);
            case "CURRALEIRO" -> entity.setUrlAssinaturaCurraleiro(url);
            default -> throw new IllegalArgumentException("Papel de assinante inválido: " + papel);
        }

        return RelatorioViagemResponse.from(repository.save(entity));
    }

    @Transactional
    public RelatorioViagemResponse enviarParaAnalise(Integer id) {
        RelatorioViagemEntity entity = repository.findById(id)
                .orElseThrow(() -> new RelatorioViagemNotFoundException(id));
        entity.setStatus("pendente");
        entity.setEnviadoEm(LocalDateTime.now());
        return RelatorioViagemResponse.from(repository.save(entity));
    }

    @Transactional
    public RelatorioViagemResponse atualizarStatus(Integer id, String novoStatus) {
        if (novoStatus == null || novoStatus.isBlank()) {
            throw new IllegalArgumentException("O status nao pode ser nulo ou vazio.");
        }
        RelatorioViagemEntity entity = repository.findById(id)
                .orElseThrow(() -> new RelatorioViagemNotFoundException(id));

        String statusNorm = novoStatus.trim().toLowerCase();
        if ("aprovado".equals(statusNorm) || "concluido".equals(statusNorm) || "finalizado".equals(statusNorm)) {
            validarAssinaturasObrigatorias(entity);
            entity.setFinalizadoEm(LocalDateTime.now());
        } else if ("pendente".equals(statusNorm) && entity.getEnviadoEm() == null) {
            entity.setEnviadoEm(LocalDateTime.now());
        }
        entity.setStatus(statusNorm);

        return RelatorioViagemResponse.from(repository.save(entity));
    }

    public void validarAssinaturasObrigatorias(RelatorioViagemEntity entity) {
        int qtdObrigatoria = 4;
        int coletadas = entity.contarAssinaturasPreenchidas();
        if (coletadas < qtdObrigatoria) {
            List<String> faltantes = new ArrayList<>();
            if (entity.getUrlAssinaturaPecuarista() == null || entity.getUrlAssinaturaPecuarista().isBlank()) {
                faltantes.add("Pecuarista");
            }
            if (entity.getUrlAssinaturaMotorista() == null || entity.getUrlAssinaturaMotorista().isBlank()) {
                faltantes.add("Motorista");
            }
            if (entity.getUrlAssinaturaManobrista() == null || entity.getUrlAssinaturaManobrista().isBlank()) {
                faltantes.add("Manobrista");
            }
            if (entity.getUrlAssinaturaCurraleiro() == null || entity.getUrlAssinaturaCurraleiro().isBlank()) {
                faltantes.add("Curraleiro");
            }
            throw new AssinaturasIncompletasException(entity.getId(), qtdObrigatoria, coletadas, faltantes);
        }
    }

    private String buscarAssinaturaFixaMotorista(Integer motoristaId) {
        if (motoristaId == null) {
            return null;
        }
        if (usuarioRepository != null) {
            String url = usuarioRepository.findById(motoristaId)
                    .map(UsuarioEntity::getUrlAssinaturaGeral)
                    .filter(u -> !u.isBlank())
                    .orElse(null);
            if (url != null) {
                return url;
            }
        }
        if (assinaturaRepository != null) {
            return assinaturaRepository.findByMotoristaIdAndAtivaTrue(motoristaId)
                    .map(a -> "/api/v1/usuarios/" + motoristaId + "/assinatura/conteudo")
                    .orElse(null);
        }
        return null;
    }

    private void validarRegrasDeNegocio(CriarRelatorioViagemRequest request) {
        LocalDateTime embarque = LocalDateTime.of(request.dataEmbarque(), request.horarioEmbarque());
        LocalDateTime saida = LocalDateTime.of(request.dataEmbarque(), request.horarioSaidaPropriedade());
        LocalDateTime chegada = LocalDateTime.of(request.dataChegadaUnidade(), request.horarioChegadaUnidade());

        if (saida.isBefore(embarque)) {
            throw new IllegalArgumentException("A saída da propriedade não pode ocorrer antes do embarque.");
        }
        if (chegada.isBefore(saida)) {
            throw new IllegalArgumentException("A chegada à unidade não pode ocorrer antes da saída.");
        }
        if (request.dataChegadaUnidade().equals(request.dataEmbarque())
                && request.horarioDesembarque().isBefore(request.horarioChegadaUnidade())) {
            throw new IllegalArgumentException("O desembarque não pode ocorrer antes da chegada à unidade.");
        }
        if (request.kmChegadaDesembarcadouro() < request.kmSaidaEmbarcadouro()) {
            throw new IllegalArgumentException("A quilometragem de chegada não pode ser menor que a de saída.");
        }
        if (request.quantidadeEmergencia() > 0
                && (request.motivoEmergencia() == null || request.motivoEmergencia().isBlank())) {
            throw new IllegalArgumentException("O motivo da emergência é obrigatório quando há emergência registrada.");
        }
    }

    private void validarPaginacao(int pagina, int tamanho) {
        if (pagina < 0) {
            throw new IllegalArgumentException("O parâmetro pagina não pode ser negativo.");
        }
        if (tamanho < 1 || tamanho > TAMANHO_MAXIMO_PAGINA) {
            throw new IllegalArgumentException("O parâmetro tamanho deve estar entre 1 e 100.");
        }
    }
}
