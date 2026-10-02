package com.example.efficientia.caminhao.service;

import com.example.efficientia.cadastrobase.api.CadastroDuplicadoException;
import com.example.efficientia.cadastrobase.api.CadastroInvalidoException;
import com.example.efficientia.cadastrobase.persistence.UsuarioEntity;
import com.example.efficientia.cadastrobase.persistence.UsuarioRepository;
import com.example.efficientia.cadastrobase.persistence.VeiculoCarretaEntity;
import com.example.efficientia.cadastrobase.persistence.VeiculoCarretaRepository;
import com.example.efficientia.cadastrobase.persistence.VeiculoCavaloEntity;
import com.example.efficientia.cadastrobase.persistence.VeiculoCavaloRepository;
import com.example.efficientia.caminhao.api.CaminhaoContracts.AtualizarCaminhaoRequest;
import com.example.efficientia.caminhao.api.CaminhaoContracts.CaminhaoAppResponse;
import com.example.efficientia.caminhao.api.CaminhaoContracts.CaminhaoRelatorioResponse;
import com.example.efficientia.caminhao.api.CaminhaoContracts.CaminhaoResponse;
import com.example.efficientia.caminhao.api.CaminhaoContracts.CriarCaminhaoRequest;
import com.example.efficientia.caminhao.api.CaminhaoContracts.TipoVeiculo;
import com.example.efficientia.caminhao.api.CaminhaoContracts.VincularCaminhaoRelatorioRequest;
import com.example.efficientia.caminhao.api.CaminhaoEmUsoException;
import com.example.efficientia.caminhao.api.CaminhaoNotFoundException;
import com.example.efficientia.caminhao.api.InspecaoVencidaException;
import com.example.efficientia.relatorioviagem.api.RelatorioViagemNotFoundException;
import com.example.efficientia.relatorioviagem.persistence.RelatorioViagemEntity;
import com.example.efficientia.relatorioviagem.persistence.RelatorioViagemRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class CaminhaoService {

    private static final List<String> STATUSES_EM_USO = List.of("rascunho", "pendente", "em_andamento");

    private final VeiculoCavaloRepository veiculoCavaloRepository;
    private final VeiculoCarretaRepository veiculoCarretaRepository;
    private final RelatorioViagemRepository relatorioViagemRepository;
    private final UsuarioRepository usuarioRepository;

    public CaminhaoService(
            VeiculoCavaloRepository veiculoCavaloRepository,
            VeiculoCarretaRepository veiculoCarretaRepository,
            RelatorioViagemRepository relatorioViagemRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.veiculoCavaloRepository = veiculoCavaloRepository;
        this.veiculoCarretaRepository = veiculoCarretaRepository;
        this.relatorioViagemRepository = relatorioViagemRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional
    public CaminhaoResponse criarCaminhao(CriarCaminhaoRequest request) {
        String placaPrincipal = normalizarPlaca(request.placa());

        if (request.tipo() == TipoVeiculo.CAVALO) {
            if (veiculoCavaloRepository.existsByPlaca(placaPrincipal)) {
                throw new CadastroDuplicadoException("Placa de cavalo ja cadastrada: " + placaPrincipal);
            }
            VeiculoCavaloEntity cavalo = new VeiculoCavaloEntity();
            cavalo.setPlaca(placaPrincipal);
            cavalo.setEmpresaId(request.empresaId());
            cavalo.setAtivo(request.ativo() == null || request.ativo());
            cavalo.setDataVencimentoInspecao(request.dataVencimentoInspecao() != null
                    ? request.dataVencimentoInspecao()
                    : LocalDate.now().plusDays(30));
            cavalo.setKmAcumulado(request.kmAcumulado() != null ? request.kmAcumulado() : 0);
            cavalo.setMarca(request.marca());
            cavalo.setModelo(request.modelo());
            cavalo.setAnoFabricacao(request.anoFabricacao());

            VeiculoCavaloEntity salvo = veiculoCavaloRepository.save(cavalo);
            return CaminhaoResponse.fromCavalo(salvo, "DISPONIVEL", null, null, null);
        } else if (request.tipo() == TipoVeiculo.CARRETA) {
            if (veiculoCarretaRepository.existsByPlaca(placaPrincipal)) {
                throw new CadastroDuplicadoException("Placa de carreta ja cadastrada: " + placaPrincipal);
            }
            if (request.capacidadeCabecas() == null || request.capacidadeCabecas() <= 0) {
                throw new CadastroInvalidoException("Capacidade de cabecas da carreta deve ser informada e positiva");
            }
            VeiculoCarretaEntity carreta = new VeiculoCarretaEntity();
            carreta.setPlaca(placaPrincipal);
            carreta.setCapacidadeCabecas(request.capacidadeCabecas());
            carreta.setEmpresaId(request.empresaId());
            carreta.setAtivo(request.ativo() == null || request.ativo());
            carreta.setDataVencimentoInspecao(request.dataVencimentoInspecao() != null
                    ? request.dataVencimentoInspecao()
                    : LocalDate.now().plusDays(30));
            carreta.setMarca(request.marca());
            carreta.setModelo(request.modelo());
            carreta.setTipoCarreta(request.tipoCarreta());

            VeiculoCarretaEntity salva = veiculoCarretaRepository.save(carreta);
            return CaminhaoResponse.fromCarreta(salva, "DISPONIVEL", null, null, null);
        } else if (request.tipo() == TipoVeiculo.CONJUNTO) {
            if (request.placaCarreta() == null || request.placaCarreta().isBlank()) {
                throw new CadastroInvalidoException("Para cadastrar um conjunto de caminhao, informe a placa da carreta");
            }
            String placaCarreta = normalizarPlaca(request.placaCarreta());
            if (veiculoCavaloRepository.existsByPlaca(placaPrincipal)) {
                throw new CadastroDuplicadoException("Placa de cavalo ja cadastrada: " + placaPrincipal);
            }
            if (veiculoCarretaRepository.existsByPlaca(placaCarreta)) {
                throw new CadastroDuplicadoException("Placa de carreta ja cadastrada: " + placaCarreta);
            }
            if (request.capacidadeCabecas() == null || request.capacidadeCabecas() <= 0) {
                throw new CadastroInvalidoException("Capacidade de cabecas da carreta deve ser informada e positiva");
            }

            VeiculoCavaloEntity cavalo = new VeiculoCavaloEntity();
            cavalo.setPlaca(placaPrincipal);
            cavalo.setEmpresaId(request.empresaId());
            cavalo.setAtivo(request.ativo() == null || request.ativo());
            cavalo.setDataVencimentoInspecao(request.dataVencimentoInspecao() != null
                    ? request.dataVencimentoInspecao()
                    : LocalDate.now().plusDays(30));
            cavalo.setKmAcumulado(request.kmAcumulado() != null ? request.kmAcumulado() : 0);
            cavalo.setMarca(request.marca());
            cavalo.setModelo(request.modelo());
            cavalo.setAnoFabricacao(request.anoFabricacao());
            VeiculoCavaloEntity salvoCavalo = veiculoCavaloRepository.save(cavalo);

            VeiculoCarretaEntity carreta = new VeiculoCarretaEntity();
            carreta.setPlaca(placaCarreta);
            carreta.setCapacidadeCabecas(request.capacidadeCabecas());
            carreta.setEmpresaId(request.empresaId());
            carreta.setAtivo(request.ativo() == null || request.ativo());
            carreta.setDataVencimentoInspecao(request.dataVencimentoInspecao() != null
                    ? request.dataVencimentoInspecao()
                    : LocalDate.now().plusDays(30));
            carreta.setMarca(request.marca());
            carreta.setModelo(request.modelo());
            carreta.setTipoCarreta(request.tipoCarreta());
            veiculoCarretaRepository.save(carreta);

            return CaminhaoResponse.fromCavalo(salvoCavalo, "DISPONIVEL", null, null, null);
        }

        throw new CadastroInvalidoException("Tipo de veiculo invalido: " + request.tipo());
    }

    @Transactional(readOnly = true)
    public List<CaminhaoResponse> listarCaminhoes(Integer empresaId, Boolean ativo, String tipo) {
        List<RelatorioViagemEntity> relatoriosAbertos = relatorioViagemRepository.findByStatusIn(STATUSES_EM_USO);

        Map<Integer, RelatorioViagemEntity> cavaloParaRelatorio = new HashMap<>();
        Map<Integer, RelatorioViagemEntity> carretaParaRelatorio = new HashMap<>();
        for (RelatorioViagemEntity rel : relatoriosAbertos) {
            if (rel.getCavaloId() != null) {
                cavaloParaRelatorio.put(rel.getCavaloId(), rel);
            }
            if (rel.getCarretaId() != null) {
                carretaParaRelatorio.put(rel.getCarretaId(), rel);
            }
        }

        Set<Integer> motoristasIds = relatoriosAbertos.stream()
                .map(RelatorioViagemEntity::getMotoristaId)
                .collect(Collectors.toSet());

        Map<Integer, String> motoristasNomes = new HashMap<>();
        if (!motoristasIds.isEmpty()) {
            usuarioRepository.findAllById(motoristasIds).forEach(u ->
                    motoristasNomes.put(u.getId(), u.getNome()));
        }

        List<CaminhaoResponse> resultados = new ArrayList<>();
        boolean incluirCavalo = tipo == null || tipo.equalsIgnoreCase("TODOS") || tipo.equalsIgnoreCase("CAVALO");
        boolean incluirCarreta = tipo == null || tipo.equalsIgnoreCase("TODOS") || tipo.equalsIgnoreCase("CARRETA");

        if (incluirCavalo) {
            List<VeiculoCavaloEntity> cavalos = veiculoCavaloRepository.findAll();
            for (VeiculoCavaloEntity c : cavalos) {
                if (empresaId != null && !empresaId.equals(c.getEmpresaId())) {
                    continue;
                }
                if (ativo != null && !ativo.equals(c.getAtivo())) {
                    continue;
                }
                RelatorioViagemEntity rel = cavaloParaRelatorio.get(c.getId());
                String statusUso = rel != null ? "EM_USO" : "DISPONIVEL";
                Integer relId = rel != null ? rel.getId() : null;
                Integer motId = rel != null ? rel.getMotoristaId() : null;
                String motNome = motId != null ? motoristasNomes.get(motId) : null;

                resultados.add(CaminhaoResponse.fromCavalo(c, statusUso, relId, motId, motNome));
            }
        }

        if (incluirCarreta) {
            List<VeiculoCarretaEntity> carretas = veiculoCarretaRepository.findAll();
            for (VeiculoCarretaEntity c : carretas) {
                if (empresaId != null && !empresaId.equals(c.getEmpresaId())) {
                    continue;
                }
                if (ativo != null && !ativo.equals(c.getAtivo())) {
                    continue;
                }
                RelatorioViagemEntity rel = carretaParaRelatorio.get(c.getId());
                String statusUso = rel != null ? "EM_USO" : "DISPONIVEL";
                Integer relId = rel != null ? rel.getId() : null;
                Integer motId = rel != null ? rel.getMotoristaId() : null;
                String motNome = motId != null ? motoristasNomes.get(motId) : null;

                resultados.add(CaminhaoResponse.fromCarreta(c, statusUso, relId, motId, motNome));
            }
        }

        return resultados;
    }

    @Transactional(readOnly = true)
    public List<CaminhaoAppResponse> listarCaminhoesApp(Integer empresaId) {
        return listarCaminhoes(empresaId, true, null).stream()
                .map(CaminhaoAppResponse::fromCaminhaoResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<CaminhaoResponse> listarCaminhoesDisponiveis(Integer empresaId, String tipo) {
        return listarCaminhoes(empresaId, true, tipo).stream()
                .filter(c -> "DISPONIVEL".equalsIgnoreCase(c.statusUso()))
                .filter(c -> c.dataVencimentoInspecao() == null || !c.dataVencimentoInspecao().isBefore(LocalDate.now()))
                .toList();
    }

    @Transactional(readOnly = true)
    public CaminhaoResponse buscarCaminhaoPorId(String tipo, Integer id) {
        if ("CARRETA".equalsIgnoreCase(tipo)) {
            VeiculoCarretaEntity carreta = veiculoCarretaRepository.findById(id)
                    .orElseThrow(() -> new CaminhaoNotFoundException("Carreta nao encontrada com id " + id));
            return mapearCarretaResponse(carreta);
        } else if ("CAVALO".equalsIgnoreCase(tipo)) {
            VeiculoCavaloEntity cavalo = veiculoCavaloRepository.findById(id)
                    .orElseThrow(() -> new CaminhaoNotFoundException("Cavalo nao encontrado com id " + id));
            return mapearCavaloResponse(cavalo);
        }

        Optional<VeiculoCavaloEntity> cavaloOpt = veiculoCavaloRepository.findById(id);
        if (cavaloOpt.isPresent()) {
            return mapearCavaloResponse(cavaloOpt.get());
        }

        VeiculoCarretaEntity carreta = veiculoCarretaRepository.findById(id)
                .orElseThrow(() -> new CaminhaoNotFoundException("Caminhao/Veiculo nao encontrado com id " + id));
        return mapearCarretaResponse(carreta);
    }

    @Transactional(readOnly = true)
    public CaminhaoResponse buscarCaminhaoPorPlaca(String placa) {
        String placaNorm = normalizarPlaca(placa);
        Optional<VeiculoCavaloEntity> cavaloOpt = veiculoCavaloRepository.findByPlaca(placaNorm);
        if (cavaloOpt.isPresent()) {
            return mapearCavaloResponse(cavaloOpt.get());
        }

        VeiculoCarretaEntity carreta = veiculoCarretaRepository.findByPlaca(placaNorm)
                .orElseThrow(() -> new CaminhaoNotFoundException("Caminhao/Veiculo nao encontrado com placa " + placaNorm));
        return mapearCarretaResponse(carreta);
    }

    @Transactional
    public CaminhaoResponse atualizarCaminhao(String tipo, Integer id, AtualizarCaminhaoRequest request) {
        if ("CARRETA".equalsIgnoreCase(tipo)) {
            VeiculoCarretaEntity carreta = veiculoCarretaRepository.findById(id)
                    .orElseThrow(() -> new CaminhaoNotFoundException("Carreta nao encontrada com id " + id));

            if (request.placa() != null && !request.placa().isBlank()) {
                String novaPlaca = normalizarPlaca(request.placa());
                if (veiculoCarretaRepository.existsByPlacaAndIdNot(novaPlaca, id)) {
                    throw new CadastroDuplicadoException("Placa de carreta ja em uso: " + novaPlaca);
                }
                carreta.setPlaca(novaPlaca);
            }
            if (request.capacidadeCabecas() != null) {
                if (request.capacidadeCabecas() <= 0) {
                    throw new CadastroInvalidoException("Capacidade de cabecas deve ser positiva");
                }
                carreta.setCapacidadeCabecas(request.capacidadeCabecas());
            }
            if (request.empresaId() != null) carreta.setEmpresaId(request.empresaId());
            if (request.ativo() != null) carreta.setAtivo(request.ativo());
            if (request.dataVencimentoInspecao() != null) carreta.setDataVencimentoInspecao(request.dataVencimentoInspecao());
            if (request.marca() != null) carreta.setMarca(request.marca());
            if (request.modelo() != null) carreta.setModelo(request.modelo());
            if (request.tipoCarreta() != null) carreta.setTipoCarreta(request.tipoCarreta());

            return mapearCarretaResponse(veiculoCarretaRepository.save(carreta));
        }

        VeiculoCavaloEntity cavalo = veiculoCavaloRepository.findById(id)
                .orElseThrow(() -> new CaminhaoNotFoundException("Cavalo nao encontrado com id " + id));

        if (request.placa() != null && !request.placa().isBlank()) {
            String novaPlaca = normalizarPlaca(request.placa());
            if (veiculoCavaloRepository.existsByPlacaAndIdNot(novaPlaca, id)) {
                throw new CadastroDuplicadoException("Placa de cavalo ja em uso: " + novaPlaca);
            }
            cavalo.setPlaca(novaPlaca);
        }
        if (request.empresaId() != null) cavalo.setEmpresaId(request.empresaId());
        if (request.ativo() != null) cavalo.setAtivo(request.ativo());
        if (request.dataVencimentoInspecao() != null) cavalo.setDataVencimentoInspecao(request.dataVencimentoInspecao());
        if (request.kmAcumulado() != null) cavalo.setKmAcumulado(request.kmAcumulado());
        if (request.marca() != null) cavalo.setMarca(request.marca());
        if (request.modelo() != null) cavalo.setModelo(request.modelo());
        if (request.anoFabricacao() != null) cavalo.setAnoFabricacao(request.anoFabricacao());

        return mapearCavaloResponse(veiculoCavaloRepository.save(cavalo));
    }

    @Transactional
    public void removerCaminhao(String tipo, Integer id) {
        if ("CARRETA".equalsIgnoreCase(tipo)) {
            VeiculoCarretaEntity carreta = veiculoCarretaRepository.findById(id)
                    .orElseThrow(() -> new CaminhaoNotFoundException("Carreta nao encontrada com id " + id));
            List<RelatorioViagemEntity> vinculados = relatorioViagemRepository.findByCarretaId(id);
            if (!vinculados.isEmpty()) {
                carreta.setAtivo(false);
                veiculoCarretaRepository.save(carreta);
            } else {
                veiculoCarretaRepository.delete(carreta);
            }
            return;
        }

        VeiculoCavaloEntity cavalo = veiculoCavaloRepository.findById(id)
                .orElseThrow(() -> new CaminhaoNotFoundException("Cavalo nao encontrado com id " + id));
        List<RelatorioViagemEntity> vinculados = relatorioViagemRepository.findByCavaloId(id);
        if (!vinculados.isEmpty()) {
            cavalo.setAtivo(false);
            veiculoCavaloRepository.save(cavalo);
        } else {
            veiculoCavaloRepository.delete(cavalo);
        }
    }

    @Transactional(readOnly = true)
    public CaminhaoRelatorioResponse buscarCaminhaoDoRelatorio(Integer relatorioId) {
        RelatorioViagemEntity relatorio = relatorioViagemRepository.findById(relatorioId)
                .orElseThrow(() -> new RelatorioViagemNotFoundException(relatorioId));

        VeiculoCavaloEntity cavalo = veiculoCavaloRepository.findById(relatorio.getCavaloId())
                .orElseThrow(() -> new CaminhaoNotFoundException("Cavalo do relatorio nao encontrado"));
        VeiculoCarretaEntity carreta = veiculoCarretaRepository.findById(relatorio.getCarretaId())
                .orElseThrow(() -> new CaminhaoNotFoundException("Carreta do relatorio nao encontrada"));

        UsuarioEntity motorista = usuarioRepository.findById(relatorio.getMotoristaId()).orElse(null);
        String motoristaNome = motorista != null ? motorista.getNome() : "Desconhecido";

        boolean emUso = STATUSES_EM_USO.contains(relatorio.getStatus() != null ? relatorio.getStatus().toLowerCase() : "rascunho");

        CaminhaoResponse cavaloRes = CaminhaoResponse.fromCavalo(
                cavalo, emUso ? "EM_USO" : "DISPONIVEL", relatorio.getId(), relatorio.getMotoristaId(), motoristaNome);
        CaminhaoResponse carretaRes = CaminhaoResponse.fromCarreta(
                carreta, emUso ? "EM_USO" : "DISPONIVEL", relatorio.getId(), relatorio.getMotoristaId(), motoristaNome);

        return new CaminhaoRelatorioResponse(
                relatorio.getId(),
                relatorio.getStatus(),
                relatorio.getMotoristaId(),
                motoristaNome,
                cavaloRes,
                carretaRes,
                cavalo.getPlaca(),
                carreta.getPlaca(),
                emUso
        );
    }

    @Transactional(readOnly = true)
    public CaminhaoRelatorioResponse buscarCaminhaoAtivoDoMotorista(Integer motoristaId) {
        RelatorioViagemEntity relatorio = relatorioViagemRepository
                .findFirstByMotoristaIdAndStatusInOrderByCriadoEmDesc(motoristaId, STATUSES_EM_USO)
                .orElseThrow(() -> new CaminhaoNotFoundException("Nenhum relatorio ativo com caminhao encontrado para o motorista " + motoristaId));

        return buscarCaminhaoDoRelatorio(relatorio.getId());
    }

    @Transactional
    public CaminhaoRelatorioResponse vincularCaminhaoAoRelatorio(Integer relatorioId, VincularCaminhaoRelatorioRequest request) {
        RelatorioViagemEntity relatorio = relatorioViagemRepository.findById(relatorioId)
                .orElseThrow(() -> new RelatorioViagemNotFoundException(relatorioId));

        VeiculoCavaloEntity cavalo;
        if (request.cavaloId() != null) {
            cavalo = veiculoCavaloRepository.findById(request.cavaloId())
                    .orElseThrow(() -> new CaminhaoNotFoundException("Cavalo nao encontrado com id " + request.cavaloId()));
        } else if (request.placaCavalo() != null && !request.placaCavalo().isBlank()) {
            String placaNorm = normalizarPlaca(request.placaCavalo());
            cavalo = veiculoCavaloRepository.findByPlaca(placaNorm)
                    .orElseThrow(() -> new CaminhaoNotFoundException("Cavalo nao encontrado com placa " + placaNorm));
        } else {
            cavalo = veiculoCavaloRepository.findById(relatorio.getCavaloId())
                    .orElseThrow(() -> new CaminhaoNotFoundException("Cavalo do relatorio nao encontrado"));
        }

        VeiculoCarretaEntity carreta;
        if (request.carretaId() != null) {
            carreta = veiculoCarretaRepository.findById(request.carretaId())
                    .orElseThrow(() -> new CaminhaoNotFoundException("Carreta nao encontrada com id " + request.carretaId()));
        } else if (request.placaCarreta() != null && !request.placaCarreta().isBlank()) {
            String placaNorm = normalizarPlaca(request.placaCarreta());
            carreta = veiculoCarretaRepository.findByPlaca(placaNorm)
                    .orElseThrow(() -> new CaminhaoNotFoundException("Carreta nao encontrada com placa " + placaNorm));
        } else {
            carreta = veiculoCarretaRepository.findById(relatorio.getCarretaId())
                    .orElseThrow(() -> new CaminhaoNotFoundException("Carreta do relatorio nao encontrada"));
        }

        // Regra de Negocio do Banco de Dados: Validar se inspecao esta vencida (fn_validar_alocacao_viagem)
        LocalDate hoje = LocalDate.now();
        if (cavalo.getDataVencimentoInspecao() != null && cavalo.getDataVencimentoInspecao().isBefore(hoje)) {
            throw new InspecaoVencidaException("Operacao Bloqueada: Cavalo selecionado esta com a inspecao vencida desde "
                    + cavalo.getDataVencimentoInspecao());
        }
        if (carreta.getDataVencimentoInspecao() != null && carreta.getDataVencimentoInspecao().isBefore(hoje)) {
            throw new InspecaoVencidaException("Operacao Bloqueada: Carreta selecionada esta com a inspecao vencida desde "
                    + carreta.getDataVencimentoInspecao());
        }

        if (request.motoristaId() != null) {
            relatorio.setMotoristaId(request.motoristaId());
        }

        relatorio.setCavaloId(cavalo.getId());
        relatorio.setCarretaId(carreta.getId());
        relatorioViagemRepository.save(relatorio);

        return buscarCaminhaoDoRelatorio(relatorioId);
    }

    private CaminhaoResponse mapearCavaloResponse(VeiculoCavaloEntity cavalo) {
        Optional<RelatorioViagemEntity> relOpt = relatorioViagemRepository
                .findFirstByCavaloIdAndStatusInOrderByCriadoEmDesc(cavalo.getId(), STATUSES_EM_USO);
        String statusUso = relOpt.isPresent() ? "EM_USO" : "DISPONIVEL";
        Integer relId = relOpt.map(RelatorioViagemEntity::getId).orElse(null);
        Integer motId = relOpt.map(RelatorioViagemEntity::getMotoristaId).orElse(null);
        String motNome = motId != null ? usuarioRepository.findById(motId).map(UsuarioEntity::getNome).orElse(null) : null;
        return CaminhaoResponse.fromCavalo(cavalo, statusUso, relId, motId, motNome);
    }

    private CaminhaoResponse mapearCarretaResponse(VeiculoCarretaEntity carreta) {
        Optional<RelatorioViagemEntity> relOpt = relatorioViagemRepository
                .findFirstByCarretaIdAndStatusInOrderByCriadoEmDesc(carreta.getId(), STATUSES_EM_USO);
        String statusUso = relOpt.isPresent() ? "EM_USO" : "DISPONIVEL";
        Integer relId = relOpt.map(RelatorioViagemEntity::getId).orElse(null);
        Integer motId = relOpt.map(RelatorioViagemEntity::getMotoristaId).orElse(null);
        String motNome = motId != null ? usuarioRepository.findById(motId).map(UsuarioEntity::getNome).orElse(null) : null;
        return CaminhaoResponse.fromCarreta(carreta, statusUso, relId, motId, motNome);
    }

    private String normalizarPlaca(String placa) {
        if (placa == null) {
            throw new CadastroInvalidoException("Placa nao pode ser nula");
        }
        return placa.trim().toUpperCase(Locale.ROOT);
    }
}
