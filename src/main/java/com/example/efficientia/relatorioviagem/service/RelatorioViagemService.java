package com.example.efficientia.relatorioviagem.service;

import com.example.efficientia.relatorioviagem.api.CriarRelatorioViagemRequest;
import com.example.efficientia.relatorioviagem.api.RelatorioViagemNotFoundException;
import com.example.efficientia.relatorioviagem.api.NumeroGtaDuplicadoException;
import com.example.efficientia.relatorioviagem.api.RelatorioViagemPageResponse;
import com.example.efficientia.relatorioviagem.api.RelatorioViagemResponse;
import com.example.efficientia.relatorioviagem.persistence.RelatorioViagemEntity;
import com.example.efficientia.relatorioviagem.persistence.RelatorioViagemRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class RelatorioViagemService {
    private static final int TAMANHO_MAXIMO_PAGINA = 100;
    private final RelatorioViagemRepository repository;

    public RelatorioViagemService(RelatorioViagemRepository repository) {
        this.repository = repository;
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
        entity.setUrlAssinaturaPecuarista(request.urlAssinaturaPecuarista().trim());
        entity.setUrlAssinaturaMotorista(request.urlAssinaturaMotorista().trim());
        entity.setUrlAssinaturaManobrista(request.urlAssinaturaManobrista().trim());
        entity.setUrlAssinaturaCurraleiro(request.urlAssinaturaCurraleiro().trim());
        entity.setCriadoEm(LocalDateTime.now());

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
