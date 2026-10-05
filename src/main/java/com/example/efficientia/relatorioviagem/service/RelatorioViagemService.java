package com.example.efficientia.relatorioviagem.service;

import com.example.efficientia.assinaturamotorista.persistence.AssinaturaMotoristaRepository;
import com.example.efficientia.cadastrobase.persistence.UsuarioEntity;
import com.example.efficientia.cadastrobase.persistence.UsuarioRepository;
import com.example.efficientia.relatorioviagem.api.AnomaliaItemDto;
import com.example.efficientia.relatorioviagem.api.AssinaturasIncompletasException;
import com.example.efficientia.relatorioviagem.api.CriarRelatorioViagemRequest;
import com.example.efficientia.relatorioviagem.api.NumeroGtaDuplicadoException;
import com.example.efficientia.relatorioviagem.api.ParadaImprevistaDto;
import com.example.efficientia.relatorioviagem.api.RegistrarAssinaturasRequest;
import com.example.efficientia.relatorioviagem.api.RelatorioViagemNotFoundException;
import com.example.efficientia.relatorioviagem.api.RelatorioViagemPageResponse;
import com.example.efficientia.relatorioviagem.api.RelatorioViagemResponse;
import com.example.efficientia.relatorioviagem.api.ValidacaoDiarioRotaException;
import com.example.efficientia.relatorioviagem.persistence.AnomaliaDesembarqueEntity;
import com.example.efficientia.relatorioviagem.persistence.AnomaliaDesembarqueRepository;
import com.example.efficientia.relatorioviagem.persistence.AnomaliaEmbarqueEntity;
import com.example.efficientia.relatorioviagem.persistence.AnomaliaEmbarqueRepository;
import com.example.efficientia.relatorioviagem.persistence.ParadaImprevistaEntity;
import com.example.efficientia.relatorioviagem.persistence.ParadaImprevistaRepository;
import com.example.efficientia.relatorioviagem.persistence.RelatorioViagemEntity;
import com.example.efficientia.relatorioviagem.persistence.RelatorioViagemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class RelatorioViagemService {
    private static final int TAMANHO_MAXIMO_PAGINA = 100;
    private final RelatorioViagemRepository repository;
    private UsuarioRepository usuarioRepository;
    private AssinaturaMotoristaRepository assinaturaRepository;
    private ParadaImprevistaRepository paradaImprevistaRepository;
    private AnomaliaEmbarqueRepository anomaliaEmbarqueRepository;
    private AnomaliaDesembarqueRepository anomaliaDesembarqueRepository;

    public RelatorioViagemService(RelatorioViagemRepository repository) {
        this.repository = repository;
    }

    public RelatorioViagemService(
            RelatorioViagemRepository repository,
            UsuarioRepository usuarioRepository,
            AssinaturaMotoristaRepository assinaturaRepository
    ) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.assinaturaRepository = assinaturaRepository;
    }

    @Autowired
    public RelatorioViagemService(
            RelatorioViagemRepository repository,
            @Autowired(required = false) UsuarioRepository usuarioRepository,
            @Autowired(required = false) AssinaturaMotoristaRepository assinaturaRepository,
            @Autowired(required = false) ParadaImprevistaRepository paradaImprevistaRepository,
            @Autowired(required = false) AnomaliaEmbarqueRepository anomaliaEmbarqueRepository,
            @Autowired(required = false) AnomaliaDesembarqueRepository anomaliaDesembarqueRepository
    ) {
        this.repository = repository;
        this.usuarioRepository = usuarioRepository;
        this.assinaturaRepository = assinaturaRepository;
        this.paradaImprevistaRepository = paradaImprevistaRepository;
        this.anomaliaEmbarqueRepository = anomaliaEmbarqueRepository;
        this.anomaliaDesembarqueRepository = anomaliaDesembarqueRepository;
    }

    @Transactional
    public RelatorioViagemResponse criar(CriarRelatorioViagemRequest request) {
        return criar(request, null, null);
    }

    @Transactional
    public RelatorioViagemResponse criar(CriarRelatorioViagemRequest request, Authentication authentication, UUID idempotencyKey) {
        String statusInformado = request.status() != null && !request.status().isBlank()
                ? request.status().trim().toLowerCase()
                : "rascunho";
        validarStatus(statusInformado);
        boolean isEnvioFinal = "pendente".equals(statusInformado) || "aprovado".equals(statusInformado)
                || "concluido".equals(statusInformado);

        if (isEnvioFinal && idempotencyKey == null) {
            throw new ValidacaoDiarioRotaException("A submissão final exige o header Idempotency-Key.",
                    Map.of("Idempotency-Key", "Informe uma chave UUID para evitar relatórios duplicados."));
        }
        if (!isEnvioFinal && idempotencyKey != null) {
            throw new ValidacaoDiarioRotaException("Idempotency-Key só pode ser usada na submissão final.",
                    Map.of("Idempotency-Key", "Informe a chave apenas ao enviar o relatório para análise."));
        }
        if (isEnvioFinal) {
            Optional<RelatorioViagemEntity> existente = repository.findByIdempotencyKey(idempotencyKey);
            if (existente.isPresent()) {
                return montarResponseCompleta(existente.get());
            }
        }

        validarCoerenciaDiarioRota(request, isEnvioFinal);

        String numeroGta = request.numeroGta() != null ? request.numeroGta().trim() : null;
        if (numeroGta != null && !numeroGta.isBlank() && repository.existsByNumeroGta(numeroGta)) {
            throw new NumeroGtaDuplicadoException(numeroGta);
        }

        Integer motoristaIdResolvido = request.motoristaId();
        Integer empresaIdResolvido = null;
        if (authentication != null) {
            Integer authUserId = extrairUsuarioId(authentication);
            if (authUserId == null) {
                throw new ValidacaoDiarioRotaException("Não foi possível identificar o usuário autenticado.",
                        Map.of("motoristaId", "O JWT precisa identificar o usuário responsável pelo relatório."));
            }
            motoristaIdResolvido = authUserId;
            empresaIdResolvido = extrairEmpresaId(authentication, authUserId);
        }

        RelatorioViagemEntity entity = new RelatorioViagemEntity();
        popularDadosEntidade(entity, request, motoristaIdResolvido, empresaIdResolvido, idempotencyKey, statusInformado);
        if (isEnvioFinal) {
            validarAssinaturasObrigatorias(entity);
        }
        entity.setCriadoEm(LocalDateTime.now());
        entity.setAtualizadoEm(LocalDateTime.now());

        if (isEnvioFinal && entity.getEnviadoEm() == null) {
            entity.setEnviadoEm(LocalDateTime.now());
        }
        if ("aprovado".equals(statusInformado) || "concluido".equals(statusInformado)) {
            entity.setFinalizadoEm(LocalDateTime.now());
        }

        RelatorioViagemEntity salva = repository.save(entity);
        salvarParadasEAnomalias(salva.getId(), request);

        return montarResponseCompleta(salva);
    }

    @Transactional
    public RelatorioViagemResponse atualizar(Integer id, CriarRelatorioViagemRequest request, Authentication authentication, UUID idempotencyKey) {
        RelatorioViagemEntity entity = repository.findById(id)
                .orElseThrow(() -> new RelatorioViagemNotFoundException(id));

        if ("aprovado".equalsIgnoreCase(entity.getStatus())) {
            throw new IllegalArgumentException("Relatórios de viagem já aprovados e finalizados não podem ser alterados.");
        }

        String statusNovo = request.status() != null && !request.status().isBlank()
                ? request.status().trim().toLowerCase()
                : entity.getStatus();
        validarStatus(statusNovo);
        boolean isEnvioFinal = "pendente".equals(statusNovo) || "aprovado".equals(statusNovo)
                || "concluido".equals(statusNovo);

        if (isEnvioFinal && idempotencyKey == null && entity.getIdempotencyKey() == null) {
            throw new ValidacaoDiarioRotaException("A submissão final exige o header Idempotency-Key.",
                    Map.of("Idempotency-Key", "Informe uma chave UUID para evitar relatórios duplicados."));
        }
        if (idempotencyKey != null && entity.getIdempotencyKey() != null
                && !entity.getIdempotencyKey().equals(idempotencyKey)) {
            throw new ValidacaoDiarioRotaException("A chave de idempotência do relatório é imutável.",
                    Map.of("Idempotency-Key", "Use a chave já vinculada a este relatório."));
        }
        if (idempotencyKey != null) {
            Optional<RelatorioViagemEntity> existente = repository.findByIdempotencyKey(idempotencyKey);
            if (existente.isPresent() && !existente.get().getId().equals(id)) {
                throw new ValidacaoDiarioRotaException("Idempotency-Key já utilizada por outro relatório.",
                        Map.of("Idempotency-Key", "Informe uma chave UUID ainda não utilizada."));
            }
        }

        validarCoerenciaDiarioRota(request, isEnvioFinal);

        if (request.numeroGta() != null && !request.numeroGta().isBlank()) {
            String gtaTrim = request.numeroGta().trim();
            if (!gtaTrim.equalsIgnoreCase(entity.getNumeroGta()) && repository.existsByNumeroGta(gtaTrim)) {
                throw new NumeroGtaDuplicadoException(gtaTrim);
            }
        }

        Integer motoristaIdResolvido = entity.getMotoristaId();
        Integer empresaIdResolvido = entity.getEmpresaId();
        if (authentication != null) {
            Integer authUserId = extrairUsuarioId(authentication);
            if (authUserId == null) {
                throw new ValidacaoDiarioRotaException("Não foi possível identificar o usuário autenticado.",
                        Map.of("motoristaId", "O JWT precisa identificar o usuário responsável pelo relatório."));
            }
            if (isPerfilMotorista(authentication) && !authUserId.equals(entity.getMotoristaId())) {
                throw new ValidacaoDiarioRotaException("O relatório não pertence ao motorista autenticado.",
                        Map.of("motoristaId", "O motorista do relatório deve corresponder ao usuário autenticado."));
            }
            if (empresaIdResolvido == null) {
                empresaIdResolvido = extrairEmpresaId(authentication, authUserId);
            }
        }

        popularDadosEntidade(entity, request, motoristaIdResolvido, empresaIdResolvido, idempotencyKey, statusNovo);
        if (isEnvioFinal) {
            validarAssinaturasObrigatorias(entity);
        }
        entity.setAtualizadoEm(LocalDateTime.now());

        if (isEnvioFinal && entity.getEnviadoEm() == null) {
            entity.setEnviadoEm(LocalDateTime.now());
        }
        if ("aprovado".equals(statusNovo) || "concluido".equals(statusNovo)) {
            entity.setFinalizadoEm(LocalDateTime.now());
        }

        RelatorioViagemEntity salva = repository.save(entity);
        salvarParadasEAnomalias(salva.getId(), request);

        return montarResponseCompleta(salva);
    }

    public RelatorioViagemPageResponse listar(int pagina, int tamanho) {
        validarPaginacao(pagina, tamanho);
        var pageable = PageRequest.of(
                pagina, tamanho,
                Sort.by(Sort.Order.desc("dataEmbarque"), Sort.Order.desc("id"))
        );
        return RelatorioViagemPageResponse.from(
                repository.findAll(pageable).map(this::montarResponseCompleta)
        );
    }

    public RelatorioViagemResponse buscar(Integer id) {
        return repository.findById(id)
                .map(this::montarResponseCompleta)
                .orElseThrow(() -> new RelatorioViagemNotFoundException(id));
    }

    @Transactional
    public RelatorioViagemResponse finalizar(Integer id) {
        return finalizar(id, null);
    }

    @Transactional
    public RelatorioViagemResponse finalizar(Integer id, UUID idempotencyKey) {
        RelatorioViagemEntity entity = repository.findById(id)
                .orElseThrow(() -> new RelatorioViagemNotFoundException(id));
        return atualizarStatus(id, "aprovado", idempotencyKey);
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

        if (entity.getUrlAssinaturaMotorista() == null && entity.getMotoristaId() != null) {
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

        entity.setAtualizadoEm(LocalDateTime.now());
        return montarResponseCompleta(repository.save(entity));
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
                if (entity.getUrlAssinaturaMotorista() == null && entity.getMotoristaId() != null) {
                    entity.setUrlAssinaturaMotorista(buscarAssinaturaFixaMotorista(entity.getMotoristaId()));
                }
            }
            case "PECUARISTA" -> entity.setUrlAssinaturaPecuarista(url);
            case "MANOBRISTA" -> entity.setUrlAssinaturaManobrista(url);
            case "CURRALEIRO" -> entity.setUrlAssinaturaCurraleiro(url);
            default -> throw new IllegalArgumentException("Papel de assinante inválido: " + papel);
        }

        entity.setAtualizadoEm(LocalDateTime.now());
        return montarResponseCompleta(repository.save(entity));
    }

    @Transactional
    public RelatorioViagemResponse enviarParaAnalise(Integer id) {
        return enviarParaAnalise(id, null);
    }

    @Transactional
    public RelatorioViagemResponse enviarParaAnalise(Integer id, UUID idempotencyKey) {
        return atualizarStatus(id, "pendente", idempotencyKey);
    }

    @Transactional
    public RelatorioViagemResponse atualizarStatus(Integer id, String novoStatus) {
        return atualizarStatus(id, novoStatus, null);
    }

    @Transactional
    public RelatorioViagemResponse atualizarStatus(Integer id, String novoStatus, UUID idempotencyKey) {
        if (novoStatus == null || novoStatus.isBlank()) {
            throw new IllegalArgumentException("O status nao pode ser nulo ou vazio.");
        }
        RelatorioViagemEntity entity = repository.findById(id)
                .orElseThrow(() -> new RelatorioViagemNotFoundException(id));

        String statusNorm = novoStatus.trim().toLowerCase();
        validarStatus(statusNorm);
        boolean transicaoFinal = "pendente".equals(statusNorm) || "aprovado".equals(statusNorm)
                || "concluido".equals(statusNorm);
        if (transicaoFinal && idempotencyKey == null && entity.getIdempotencyKey() == null) {
            throw new ValidacaoDiarioRotaException("A submissão final exige o header Idempotency-Key.",
                    Map.of("Idempotency-Key", "Informe uma chave UUID para evitar relatórios duplicados."));
        }
        if (idempotencyKey != null && entity.getIdempotencyKey() != null
                && !entity.getIdempotencyKey().equals(idempotencyKey)) {
            throw new ValidacaoDiarioRotaException("A chave de idempotência do relatório é imutável.",
                    Map.of("Idempotency-Key", "Use a chave já vinculada a este relatório."));
        }
        if (idempotencyKey != null) {
            Optional<RelatorioViagemEntity> existente = repository.findByIdempotencyKey(idempotencyKey);
            if (existente.isPresent() && !existente.get().getId().equals(id)) {
                throw new ValidacaoDiarioRotaException("Idempotency-Key já utilizada por outro relatório.",
                        Map.of("Idempotency-Key", "Informe uma chave UUID ainda não utilizada."));
            }
            entity.setIdempotencyKey(idempotencyKey);
        }
        if (transicaoFinal) {
            if (entity.getUrlAssinaturaMotorista() == null && entity.getMotoristaId() != null) {
                entity.setUrlAssinaturaMotorista(buscarAssinaturaFixaMotorista(entity.getMotoristaId()));
            }
            validarCamposObrigatorios(entity);
            validarAssinaturasObrigatorias(entity);
        }
        if ("aprovado".equals(statusNorm) || "concluido".equals(statusNorm)) {
            validarAssinaturasObrigatorias(entity);
            entity.setFinalizadoEm(LocalDateTime.now());
        } else if ("pendente".equals(statusNorm) && entity.getEnviadoEm() == null) {
            validarAssinaturasObrigatorias(entity);
            entity.setEnviadoEm(LocalDateTime.now());
        }
        entity.setStatus(statusNorm);
        entity.setAtualizadoEm(LocalDateTime.now());

        return montarResponseCompleta(repository.save(entity));
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

    private void validarCamposObrigatorios(RelatorioViagemEntity entity) {
        Map<String, String> errors = new LinkedHashMap<>();
        exigir(entity.getFazendaId() != null, "fazendaId", "A propriedade de origem é obrigatória.", errors);
        exigir(entity.getUnidadeFrigorificaId() != null, "unidadeFrigorificaId", "A unidade frigorífica de destino é obrigatória.", errors);
        exigir(presente(entity.getNumeroGta()), "numeroGta", "O número da GTA é obrigatório.", errors);
        exigir(presente(entity.getNumeroNotaFiscal()), "numeroNotaFiscal", "O número da nota fiscal é obrigatório.", errors);
        exigir(entity.getDataEmbarque() != null, "dataEmbarque", "A data de embarque é obrigatória.", errors);
        exigir(entity.getHorarioEmbarque() != null, "horarioEmbarque", "O horário de embarque é obrigatório.", errors);
        exigir(entity.getHorarioSaidaPropriedade() != null, "horarioSaidaPropriedade", "O horário de saída é obrigatório.", errors);
        exigir(entity.getKmSaidaEmbarcadouro() != null, "kmSaidaEmbarcadouro", "A quilometragem de saída é obrigatória.", errors);
        exigir(entity.getDataChegadaUnidade() != null, "dataChegadaUnidade", "A data de chegada é obrigatória.", errors);
        exigir(entity.getHorarioChegadaUnidade() != null, "horarioChegadaUnidade", "O horário de chegada é obrigatório.", errors);
        exigir(entity.getHorarioDesembarque() != null, "horarioDesembarque", "O horário de desembarque é obrigatório.", errors);
        exigir(entity.getKmChegadaDesembarcadouro() != null, "kmChegadaDesembarcadouro", "A quilometragem de chegada é obrigatória.", errors);
        exigir(presente(entity.getNumeroCurral()), "numeroCurral", "O número do curral é obrigatório.", errors);
        exigir(entity.getSireneReFuncionou() != null, "sireneReFuncionou", "Informe se a sirene de ré funcionou.", errors);
        exigir(entity.getQuantidadeMachos() != null, "quantidadeMachos", "Informe a quantidade de machos.", errors);
        exigir(entity.getQuantidadeFemeas() != null, "quantidadeFemeas", "Informe a quantidade de fêmeas.", errors);
        exigir(entity.getQuantidadeMarrucos() != null, "quantidadeMarrucos", "Informe a quantidade de marrucos.", errors);
        exigir(entity.getQuantidadeEmPe() != null, "quantidadeEmPe", "Informe a quantidade em pé.", errors);
        exigir(entity.getQuantidadeDeitado() != null, "quantidadeDeitado", "Informe a quantidade deitados.", errors);
        exigir(entity.getQuantidadeMorto() != null, "quantidadeMorto", "Informe a quantidade de mortos.", errors);
        exigir(entity.getQuantidadeEmergencia() != null, "quantidadeEmergencia", "Informe a quantidade em emergência.", errors);
        if (entity.getKmSaidaEmbarcadouro() != null && entity.getKmChegadaDesembarcadouro() != null
                && entity.getKmChegadaDesembarcadouro() < entity.getKmSaidaEmbarcadouro()) {
            errors.put("kmChegadaDesembarcadouro", "O quilômetro de chegada não pode ser inferior ao de saída.");
        }
        int total = entity.getTotalAnimais();
        int condicoes = nullAsZero(entity.getQuantidadeEmPe()) + nullAsZero(entity.getQuantidadeDeitado())
                + nullAsZero(entity.getQuantidadeMorto()) + nullAsZero(entity.getQuantidadeEmergencia());
        exigir(total > 0, "totalAnimais", "O total de animais deve ser maior que zero.", errors);
        exigir(total == condicoes, "condicaoAnimais", "A soma das condições físicas deve corresponder ao total de animais.", errors);
        if (!errors.isEmpty()) {
            throw new ValidacaoDiarioRotaException("Inconsistência nos dados do diário de rota.", errors);
        }
    }

    private int nullAsZero(Integer valor) {
        return valor == null ? 0 : valor;
    }

    public void validarCoerenciaDiarioRota(CriarRelatorioViagemRequest request, boolean isEnvioFinal) {
        Map<String, String> errors = new LinkedHashMap<>();

        // 1. Horários
        if (request.dataEmbarque() != null && request.horarioEmbarque() != null && request.horarioSaidaPropriedade() != null) {
            if (request.horarioSaidaPropriedade().isBefore(request.horarioEmbarque())) {
                errors.put("horarioSaidaPropriedade", "O horário de saída da propriedade deve ser posterior ou igual ao horário de embarque.");
            }
        }
        if (request.dataEmbarque() != null && request.horarioSaidaPropriedade() != null
                && request.dataChegadaUnidade() != null && request.horarioChegadaUnidade() != null) {
            LocalDateTime saida = LocalDateTime.of(request.dataEmbarque(), request.horarioSaidaPropriedade());
            LocalDateTime chegada = LocalDateTime.of(request.dataChegadaUnidade(), request.horarioChegadaUnidade());
            if (chegada.isBefore(saida)) {
                errors.put("horarioChegadaUnidade", "A data/horário de chegada na unidade frigorífica deve ser posterior à saída da propriedade.");
            }
        }
        if (request.horarioChegadaUnidade() != null && request.horarioDesembarque() != null) {
            if (request.horarioDesembarque().isBefore(request.horarioChegadaUnidade())) {
                errors.put("horarioDesembarque", "O horário de desembarque deve ser posterior ou igual ao horário de chegada na unidade.");
            }
        }

        // 2. Quilometragem
        if (request.kmSaidaEmbarcadouro() != null && request.kmChegadaDesembarcadouro() != null) {
            if (request.kmChegadaDesembarcadouro() < request.kmSaidaEmbarcadouro()) {
                errors.put("kmChegadaDesembarcadouro", "O quilômetro de chegada não pode ser inferior ao quilômetro de saída.");
            }
        }

        // 3. Totais e Condições de Animais
        int machos = request.quantidadeMachos() != null ? request.quantidadeMachos() : 0;
        int femeas = request.quantidadeFemeas() != null ? request.quantidadeFemeas() : 0;
        int marrucos = request.quantidadeMarrucos() != null ? request.quantidadeMarrucos() : 0;
        int totalAnimais = machos + femeas + marrucos;

        int emPe = request.quantidadeEmPe() != null ? request.quantidadeEmPe() : 0;
        int deitado = request.quantidadeDeitado() != null ? request.quantidadeDeitado() : 0;
        int morto = request.quantidadeMorto() != null ? request.quantidadeMorto() : 0;
        int emergencia = request.quantidadeEmergencia() != null ? request.quantidadeEmergencia() : 0;
        int somaCondicao = emPe + deitado + morto + emergencia;

        if (isEnvioFinal) {
            exigir(request.fazendaId() != null, "fazendaId", "A propriedade de origem é obrigatória.", errors);
            exigir(request.unidadeFrigorificaId() != null, "unidadeFrigorificaId", "A unidade frigorífica de destino é obrigatória.", errors);
            exigir(presente(request.numeroGta()), "numeroGta", "O número da GTA é obrigatório.", errors);
            exigir(presente(request.numeroNotaFiscal()), "numeroNotaFiscal", "O número da nota fiscal é obrigatório.", errors);
            exigir(request.dataEmbarque() != null, "dataEmbarque", "A data de embarque é obrigatória.", errors);
            exigir(request.horarioEmbarque() != null, "horarioEmbarque", "O horário de embarque é obrigatório.", errors);
            exigir(request.horarioSaidaPropriedade() != null, "horarioSaidaPropriedade", "O horário de saída é obrigatório.", errors);
            exigir(request.kmSaidaEmbarcadouro() != null, "kmSaidaEmbarcadouro", "A quilometragem de saída é obrigatória.", errors);
            exigir(request.dataChegadaUnidade() != null, "dataChegadaUnidade", "A data de chegada é obrigatória.", errors);
            exigir(request.horarioChegadaUnidade() != null, "horarioChegadaUnidade", "O horário de chegada é obrigatório.", errors);
            exigir(request.horarioDesembarque() != null, "horarioDesembarque", "O horário de desembarque é obrigatório.", errors);
            exigir(request.kmChegadaDesembarcadouro() != null, "kmChegadaDesembarcadouro", "A quilometragem de chegada é obrigatória.", errors);
            exigir(presente(request.numeroCurral()), "numeroCurral", "O número do curral é obrigatório.", errors);
            exigir(request.sireneReFuncionou() != null, "sireneReFuncionou", "Informe se a sirene de ré funcionou.", errors);
            exigir(request.quantidadeMachos() != null, "quantidadeMachos", "Informe a quantidade de machos.", errors);
            exigir(request.quantidadeFemeas() != null, "quantidadeFemeas", "Informe a quantidade de fêmeas.", errors);
            exigir(request.quantidadeMarrucos() != null, "quantidadeMarrucos", "Informe a quantidade de marrucos.", errors);
            exigir(request.quantidadeEmPe() != null, "quantidadeEmPe", "Informe a quantidade em pé.", errors);
            exigir(request.quantidadeDeitado() != null, "quantidadeDeitado", "Informe a quantidade deitados.", errors);
            exigir(request.quantidadeMorto() != null, "quantidadeMorto", "Informe a quantidade de mortos.", errors);
            exigir(request.quantidadeEmergencia() != null, "quantidadeEmergencia", "Informe a quantidade em emergência.", errors);
        }

        if (isEnvioFinal && totalAnimais <= 0) {
            errors.put("totalAnimais", "O total de animais transportados (machos + fêmeas + marrucos) deve ser maior que zero na submissão final.");
        }
        if (totalAnimais > 0 && somaCondicao > totalAnimais) {
            errors.put("condicaoAnimais", String.format("A soma das condições físicas (%d) não pode exceder o total de animais (%d).", somaCondicao, totalAnimais));
        }
        if (isEnvioFinal && totalAnimais > 0 && somaCondicao != totalAnimais) {
            errors.put("condicaoAnimais", String.format("A soma das condições físicas (%d) deve corresponder ao total de animais (%d).", somaCondicao, totalAnimais));
        }
        if (emergencia > 0 && (request.motivoEmergencia() == null || request.motivoEmergencia().isBlank())) {
            errors.put("motivoEmergencia", "O motivo de emergência é obrigatório quando há animais em estado de emergência.");
        }

        if (!errors.isEmpty()) {
            throw new ValidacaoDiarioRotaException("Inconsistência nos dados do diário de rota.", errors);
        }
    }

    private void popularDadosEntidade(
            RelatorioViagemEntity entity,
            CriarRelatorioViagemRequest request,
            Integer motoristaIdResolvido,
            Integer empresaIdResolvido,
            UUID idempotencyKey,
            String statusNovo
    ) {
        if (request.fazendaId() != null) entity.setFazendaId(request.fazendaId());
        if (request.unidadeFrigorificaId() != null) entity.setUnidadeFrigorificaId(request.unidadeFrigorificaId());
        if (motoristaIdResolvido != null) entity.setMotoristaId(motoristaIdResolvido);
        if (empresaIdResolvido != null) entity.setEmpresaId(empresaIdResolvido);
        if (request.manobristaId() != null) entity.setManobristaId(request.manobristaId());
        if (request.curraleiroId() != null) entity.setCurraleiroId(request.curraleiroId());
        if (request.cavaloId() != null) entity.setCavaloId(request.cavaloId());
        if (request.carretaId() != null) entity.setCarretaId(request.carretaId());
        if (idempotencyKey != null) entity.setIdempotencyKey(idempotencyKey);

        if (request.numeroGta() != null) entity.setNumeroGta(request.numeroGta().trim());
        if (request.numeroNotaFiscal() != null) entity.setNumeroNotaFiscal(request.numeroNotaFiscal().trim());
        if (request.dataEmbarque() != null) entity.setDataEmbarque(request.dataEmbarque());
        if (request.horarioEmbarque() != null) entity.setHorarioEmbarque(request.horarioEmbarque());
        if (request.horarioSaidaPropriedade() != null) entity.setHorarioSaidaPropriedade(request.horarioSaidaPropriedade());
        if (request.kmSaidaEmbarcadouro() != null) entity.setKmSaidaEmbarcadouro(request.kmSaidaEmbarcadouro());
        if (request.dataChegadaUnidade() != null) entity.setDataChegadaUnidade(request.dataChegadaUnidade());
        if (request.horarioChegadaUnidade() != null) entity.setHorarioChegadaUnidade(request.horarioChegadaUnidade());
        if (request.horarioDesembarque() != null) entity.setHorarioDesembarque(request.horarioDesembarque());
        if (request.kmChegadaDesembarcadouro() != null) entity.setKmChegadaDesembarcadouro(request.kmChegadaDesembarcadouro());
        if (request.numeroCurral() != null) entity.setNumeroCurral(request.numeroCurral().trim());
        if (request.sireneReFuncionou() != null) entity.setSireneReFuncionou(request.sireneReFuncionou());

        if (request.quantidadeMachos() != null) entity.setQuantidadeMachos(request.quantidadeMachos());
        if (request.quantidadeFemeas() != null) entity.setQuantidadeFemeas(request.quantidadeFemeas());
        if (request.quantidadeMarrucos() != null) entity.setQuantidadeMarrucos(request.quantidadeMarrucos());
        if (request.quantidadeEmPe() != null) entity.setQuantidadeEmPe(request.quantidadeEmPe());
        if (request.quantidadeDeitado() != null) entity.setQuantidadeDeitado(request.quantidadeDeitado());
        if (request.quantidadeMorto() != null) entity.setQuantidadeMorto(request.quantidadeMorto());
        if (request.quantidadeEmergencia() != null) entity.setQuantidadeEmergencia(request.quantidadeEmergencia());

        if (request.motivoEmergencia() != null) entity.setMotivoEmergencia(request.motivoEmergencia());
        if (request.comentarios() != null) entity.setComentarios(request.comentarios());
        if (request.capacidadeCargaUtilizada() != null) entity.setCapacidadeCargaUtilizada(request.capacidadeCargaUtilizada());
        if (request.urlLaudoMortalidade() != null) entity.setUrlLaudoMortalidade(request.urlLaudoMortalidade());

        if (request.urlAssinaturaPecuarista() != null && !request.urlAssinaturaPecuarista().isBlank()) {
            entity.setUrlAssinaturaPecuarista(request.urlAssinaturaPecuarista().trim());
        }

        // A assinatura do motorista é atributo da conta. O app não pode substituir
        // essa referência por uma URL escolhida pelo cliente.
        String urlMotorista = entity.getUrlAssinaturaMotorista();
        if (urlMotorista == null && entity.getMotoristaId() != null) {
            urlMotorista = buscarAssinaturaFixaMotorista(entity.getMotoristaId());
        }
        if (urlMotorista != null) {
            entity.setUrlAssinaturaMotorista(urlMotorista);
        }

        if (request.urlAssinaturaManobrista() != null && !request.urlAssinaturaManobrista().isBlank()) {
            entity.setUrlAssinaturaManobrista(request.urlAssinaturaManobrista().trim());
        }

        if (request.urlAssinaturaCurraleiro() != null && !request.urlAssinaturaCurraleiro().isBlank()) {
            entity.setUrlAssinaturaCurraleiro(request.urlAssinaturaCurraleiro().trim());
        }

        entity.setStatus(statusNovo != null ? statusNovo : "rascunho");
    }

    private void salvarParadasEAnomalias(Integer relatorioId, CriarRelatorioViagemRequest request) {
        if (relatorioId == null || request == null) {
            return;
        }

        if (paradaImprevistaRepository != null && request.paradasImprevistas() != null) {
            paradaImprevistaRepository.deleteByRelatorioId(relatorioId);
            for (ParadaImprevistaDto dto : request.paradasImprevistas()) {
                if (dto != null && dto.motivo() != null && dto.dataHoraInicio() != null && dto.dataHoraFim() != null) {
                    paradaImprevistaRepository.save(new ParadaImprevistaEntity(
                            relatorioId,
                            dto.motivo().trim(),
                            dto.dataHoraInicio(),
                            dto.dataHoraFim()
                    ));
                }
            }
        }

        if (anomaliaEmbarqueRepository != null && request.anomaliasEmbarque() != null) {
            anomaliaEmbarqueRepository.deleteByRelatorioId(relatorioId);
            for (AnomaliaItemDto dto : request.anomaliasEmbarque()) {
                if (dto != null && dto.anomalia() != null && !dto.anomalia().isBlank()) {
                    anomaliaEmbarqueRepository.save(new AnomaliaEmbarqueEntity(
                            relatorioId,
                            dto.anomalia().trim(),
                            dto.descricaoOutros(),
                            dto.quantidadeAnimais()
                    ));
                }
            }
        }

        if (anomaliaDesembarqueRepository != null && request.anomaliasDesembarque() != null) {
            anomaliaDesembarqueRepository.deleteByRelatorioId(relatorioId);
            for (AnomaliaItemDto dto : request.anomaliasDesembarque()) {
                if (dto != null && dto.anomalia() != null && !dto.anomalia().isBlank()) {
                    anomaliaDesembarqueRepository.save(new AnomaliaDesembarqueEntity(
                            relatorioId,
                            dto.anomalia().trim(),
                            dto.descricaoOutros(),
                            dto.quantidadeAnimais()
                    ));
                }
            }
        }
    }

    private RelatorioViagemResponse montarResponseCompleta(RelatorioViagemEntity entity) {
        List<ParadaImprevistaDto> paradas = List.of();
        if (paradaImprevistaRepository != null) {
            paradas = paradaImprevistaRepository.findByRelatorioId(entity.getId())
                    .stream().map(ParadaImprevistaDto::from).toList();
        }

        List<AnomaliaItemDto> anomaliasEmb = List.of();
        if (anomaliaEmbarqueRepository != null) {
            anomaliasEmb = anomaliaEmbarqueRepository.findByRelatorioId(entity.getId())
                    .stream().map(AnomaliaItemDto::fromEmbarque).toList();
        }

        List<AnomaliaItemDto> anomaliasDesemb = List.of();
        if (anomaliaDesembarqueRepository != null) {
            anomaliasDesemb = anomaliaDesembarqueRepository.findByRelatorioId(entity.getId())
                    .stream().map(AnomaliaItemDto::fromDesembarque).toList();
        }

        return RelatorioViagemResponse.from(entity, paradas, anomaliasEmb, anomaliasDesemb);
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

    public Integer extrairUsuarioId(Authentication authentication) {
        if (authentication == null) return null;
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Object usuarioIdClaim = jwtAuth.getToken().getClaim("usuario_id");
            if (usuarioIdClaim instanceof Number n) return n.intValue();
            if (usuarioIdClaim instanceof String s && !s.isBlank()) {
                try { return Integer.parseInt(s.trim()); } catch (NumberFormatException ignored) {}
            }
            String sub = jwtAuth.getToken().getSubject();
            if (sub != null && !sub.isBlank()) {
                try { return Integer.parseInt(sub.trim()); } catch (NumberFormatException ignored) {}
            }
        }
        return null;
    }

    public Integer extrairEmpresaId(Authentication authentication, Integer usuarioId) {
        if (authentication instanceof JwtAuthenticationToken jwtAuth) {
            Object empresaIdClaim = jwtAuth.getToken().getClaim("empresa_id");
            if (empresaIdClaim instanceof Number n) return n.intValue();
            if (empresaIdClaim instanceof String s && !s.isBlank()) {
                try { return Integer.parseInt(s.trim()); } catch (NumberFormatException ignored) {}
            }
        }
        if (usuarioId != null && usuarioRepository != null) {
            return usuarioRepository.findById(usuarioId)
                    .map(UsuarioEntity::getEmpresaId)
                    .orElse(null);
        }
        return null;
    }

    private boolean isPerfilMotorista(Authentication authentication) {
        if (authentication == null) return false;
        return authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().contains("MOTORISTA"));
    }

    private void exigir(boolean condicao, String campo, String mensagem, Map<String, String> errors) {
        if (!condicao) errors.put(campo, mensagem);
    }

    private boolean presente(String valor) {
        return valor != null && !valor.isBlank();
    }

    private void validarStatus(String status) {
        if (status == null || !List.of("rascunho", "pendente", "aprovado", "concluido", "reprovado")
                .contains(status)) {
            throw new ValidacaoDiarioRotaException("Status de relatório inválido.",
                    Map.of("status", "Use rascunho, pendente, aprovado, concluido ou reprovado."));
        }
    }

    private void validarPaginacao(int pagina, int tamanho) {
        if (pagina < 0) {
            throw new IllegalArgumentException("O numero da pagina nao pode ser negativo.");
        }
        if (tamanho <= 0 || tamanho > TAMANHO_MAXIMO_PAGINA) {
            throw new IllegalArgumentException("O tamanho da pagina deve ser entre 1 e " + TAMANHO_MAXIMO_PAGINA + ".");
        }
    }
}
