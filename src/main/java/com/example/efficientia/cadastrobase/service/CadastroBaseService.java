package com.example.efficientia.cadastrobase.service;

import com.example.efficientia.cadastrobase.api.CadastroDuplicadoException;
import com.example.efficientia.cadastrobase.api.CadastroInvalidoException;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.CarretaResponse;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.CavaloResponse;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.CriarCarretaRequest;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.CriarCavaloRequest;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.CriarEnderecoRequest;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.CriarFazendaRequest;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.CriarUsuarioRequest;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.EnderecoResponse;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.FazendaResponse;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.UsuarioResponse;
import com.example.efficientia.cadastrobase.domain.TipoUsuario;
import com.example.efficientia.cadastrobase.persistence.EnderecoEntity;
import com.example.efficientia.cadastrobase.persistence.EnderecoRepository;
import com.example.efficientia.cadastrobase.persistence.FazendaEntity;
import com.example.efficientia.cadastrobase.persistence.FazendaRepository;
import com.example.efficientia.cadastrobase.persistence.UsuarioEntity;
import com.example.efficientia.cadastrobase.persistence.UsuarioRepository;
import com.example.efficientia.cadastrobase.persistence.VeiculoCarretaEntity;
import com.example.efficientia.cadastrobase.persistence.VeiculoCarretaRepository;
import com.example.efficientia.cadastrobase.persistence.VeiculoCavaloEntity;
import com.example.efficientia.cadastrobase.persistence.VeiculoCavaloRepository;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.AtualizarCarretaRequest;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.AtualizarCavaloRequest;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.CarretaDetalhadaResponse;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.CavaloDetalhadoResponse;
import java.util.List;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Locale;

@Service
public class CadastroBaseService {

    private final UsuarioRepository usuarioRepository;
    private final EnderecoRepository enderecoRepository;
    private final FazendaRepository fazendaRepository;
    private final VeiculoCavaloRepository cavaloRepository;
    private final VeiculoCarretaRepository carretaRepository;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public CadastroBaseService(
            UsuarioRepository usuarioRepository,
            EnderecoRepository enderecoRepository,
            FazendaRepository fazendaRepository,
            VeiculoCavaloRepository cavaloRepository,
            VeiculoCarretaRepository carretaRepository
    ) {
        this.usuarioRepository = usuarioRepository;
        this.enderecoRepository = enderecoRepository;
        this.fazendaRepository = fazendaRepository;
        this.cavaloRepository = cavaloRepository;
        this.carretaRepository = carretaRepository;
    }

    @Transactional
    public UsuarioResponse criarUsuario(CriarUsuarioRequest request) {
        validarIdade(request.dataNascimento());

        String cpf = request.cpf().trim();
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByCpf(cpf)) {
            throw new CadastroDuplicadoException("Já existe um usuário com o CPF informado.");
        }
        if (usuarioRepository.existsByEmail(email)) {
            throw new CadastroDuplicadoException("Já existe um usuário com o e-mail informado.");
        }

        UsuarioEntity entity = new UsuarioEntity();
        entity.setTipo(request.tipo());
        entity.setCpf(cpf);
        entity.setCodigoInterno(normalizarOpcional(request.codigoInterno()));
        entity.setNome(request.nome().trim());
        entity.setDataNascimento(request.dataNascimento());
        entity.setEmail(email);
        entity.setTelefone(request.telefone().trim());
        entity.setSenhaHash(passwordEncoder.encode(request.senha()));
        entity.setAtivo(true);

        return UsuarioResponse.from(usuarioRepository.save(entity));
    }

    @Transactional
    public EnderecoResponse criarEndereco(CriarEnderecoRequest request) {
        EnderecoEntity entity = new EnderecoEntity();
        entity.setCep(request.cep().trim());
        entity.setLogradouro(request.logradouro().trim());
        entity.setNumero(request.numero().trim());
        entity.setCidade(request.cidade().trim());
        entity.setEstado(request.estado().trim().toUpperCase(Locale.ROOT));
        return EnderecoResponse.from(enderecoRepository.save(entity));
    }

    @Transactional
    public FazendaResponse criarFazenda(CriarFazendaRequest request) {
        if (!usuarioRepository.existsByIdAndTipo(request.pecuaristaId(), TipoUsuario.pecuarista)) {
            throw new CadastroInvalidoException("O pecuarista informado não existe ou possui outro tipo.");
        }
        if (!enderecoRepository.existsById(request.enderecoId())) {
            throw new CadastroInvalidoException("O endereço informado não existe.");
        }

        FazendaEntity entity = new FazendaEntity();
        entity.setPecuaristaId(request.pecuaristaId());
        entity.setEnderecoId(request.enderecoId());
        entity.setNome(request.nome().trim());
        return FazendaResponse.from(fazendaRepository.save(entity));
    }

    @Transactional
    public CavaloResponse criarCavalo(CriarCavaloRequest request) {
        String placa = normalizarPlaca(request.placa());
        if (cavaloRepository.existsByPlaca(placa)) {
            throw new CadastroDuplicadoException("Já existe um cavalo mecânico com a placa informada.");
        }

        VeiculoCavaloEntity entity = new VeiculoCavaloEntity();
        entity.setPlaca(placa);
        entity.setAtivo(request.ativo());
        return CavaloResponse.from(cavaloRepository.save(entity));
    }

    @Transactional
    public CarretaResponse criarCarreta(CriarCarretaRequest request) {
        String placa = normalizarPlaca(request.placa());
        if (carretaRepository.existsByPlaca(placa)) {
            throw new CadastroDuplicadoException("Já existe uma carreta com a placa informada.");
        }

        VeiculoCarretaEntity entity = new VeiculoCarretaEntity();
        entity.setPlaca(placa);
        entity.setCapacidadeCabecas(request.capacidadeCabecas());
        return CarretaResponse.from(carretaRepository.save(entity));
    }

    @Transactional(readOnly = true)
    public List<CavaloDetalhadoResponse> listarCavalos() {
        return cavaloRepository.findAll().stream()
                .map(CavaloDetalhadoResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CavaloDetalhadoResponse buscarCavalo(Integer id) {
        return cavaloRepository.findById(id)
                .map(CavaloDetalhadoResponse::from)
                .orElseThrow(() -> new CadastroInvalidoException("Cavalo mecânico não encontrado: " + id));
    }

    @Transactional
    public CavaloDetalhadoResponse atualizarCavalo(Integer id, AtualizarCavaloRequest request) {
        VeiculoCavaloEntity entity = cavaloRepository.findById(id)
                .orElseThrow(() -> new CadastroInvalidoException("Cavalo mecânico não encontrado: " + id));

        if (request.placa() != null && !request.placa().isBlank()) {
            String placa = normalizarPlaca(request.placa());
            if (cavaloRepository.existsByPlacaAndIdNot(placa, id)) {
                throw new CadastroDuplicadoException("Já existe um cavalo mecânico com a placa informada.");
            }
            entity.setPlaca(placa);
        }
        if (request.ativo() != null) entity.setAtivo(request.ativo());
        if (request.empresaId() != null) entity.setEmpresaId(request.empresaId());
        if (request.dataVencimentoInspecao() != null) entity.setDataVencimentoInspecao(request.dataVencimentoInspecao());
        if (request.kmAcumulado() != null) entity.setKmAcumulado(request.kmAcumulado());
        if (request.marca() != null) entity.setMarca(request.marca());
        if (request.modelo() != null) entity.setModelo(request.modelo());
        if (request.anoFabricacao() != null) entity.setAnoFabricacao(request.anoFabricacao());

        return CavaloDetalhadoResponse.from(cavaloRepository.save(entity));
    }

    @Transactional
    public void removerCavalo(Integer id) {
        VeiculoCavaloEntity entity = cavaloRepository.findById(id)
                .orElseThrow(() -> new CadastroInvalidoException("Cavalo mecânico não encontrado: " + id));
        cavaloRepository.delete(entity);
    }

    @Transactional(readOnly = true)
    public List<CarretaDetalhadaResponse> listarCarretas() {
        return carretaRepository.findAll().stream()
                .map(CarretaDetalhadaResponse::from)
                .toList();
    }

    @Transactional(readOnly = true)
    public CarretaDetalhadaResponse buscarCarreta(Integer id) {
        return carretaRepository.findById(id)
                .map(CarretaDetalhadaResponse::from)
                .orElseThrow(() -> new CadastroInvalidoException("Carreta não encontrada: " + id));
    }

    @Transactional
    public CarretaDetalhadaResponse atualizarCarreta(Integer id, AtualizarCarretaRequest request) {
        VeiculoCarretaEntity entity = carretaRepository.findById(id)
                .orElseThrow(() -> new CadastroInvalidoException("Carreta não encontrada: " + id));

        if (request.placa() != null && !request.placa().isBlank()) {
            String placa = normalizarPlaca(request.placa());
            if (carretaRepository.existsByPlacaAndIdNot(placa, id)) {
                throw new CadastroDuplicadoException("Já existe uma carreta com a placa informada.");
            }
            entity.setPlaca(placa);
        }
        if (request.capacidadeCabecas() != null) {
            if (request.capacidadeCabecas() <= 0) {
                throw new CadastroInvalidoException("Capacidade de cabeças deve ser positiva.");
            }
            entity.setCapacidadeCabecas(request.capacidadeCabecas());
        }
        if (request.ativo() != null) entity.setAtivo(request.ativo());
        if (request.empresaId() != null) entity.setEmpresaId(request.empresaId());
        if (request.dataVencimentoInspecao() != null) entity.setDataVencimentoInspecao(request.dataVencimentoInspecao());
        if (request.marca() != null) entity.setMarca(request.marca());
        if (request.modelo() != null) entity.setModelo(request.modelo());
        if (request.tipoCarreta() != null) entity.setTipoCarreta(request.tipoCarreta());

        return CarretaDetalhadaResponse.from(carretaRepository.save(entity));
    }

    @Transactional
    public void removerCarreta(Integer id) {
        VeiculoCarretaEntity entity = carretaRepository.findById(id)
                .orElseThrow(() -> new CadastroInvalidoException("Carreta não encontrada: " + id));
        carretaRepository.delete(entity);
    }

    private void validarIdade(LocalDate dataNascimento) {
        if (dataNascimento.isAfter(LocalDate.now().minusYears(18))) {
            throw new CadastroInvalidoException("O usuário deve ter pelo menos 18 anos.");
        }
    }

    private String normalizarPlaca(String placa) {
        return placa.trim().toUpperCase(Locale.ROOT);
    }

    private String normalizarOpcional(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
