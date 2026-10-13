package com.example.efficientia.relatorioviagem.api;

import com.example.efficientia.cadastrobase.persistence.EnderecoEntity;
import com.example.efficientia.cadastrobase.persistence.EnderecoRepository;
import com.example.efficientia.cadastrobase.persistence.FazendaEntity;
import com.example.efficientia.cadastrobase.persistence.FazendaRepository;
import com.example.efficientia.cadastrobase.persistence.UnidadeFrigorificaEntity;
import com.example.efficientia.cadastrobase.persistence.UnidadeFrigorificaRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@Tag(name = "Locais do Diário", description = "Catálogo autenticado para selecionar origem e destino do diário de rota")
@RequestMapping("/api/v1/locais-diario")
public class LocaisDiarioController {
    private final FazendaRepository fazendas;
    private final UnidadeFrigorificaRepository unidades;
    private final EnderecoRepository enderecos;

    public LocaisDiarioController(FazendaRepository fazendas,
                                  UnidadeFrigorificaRepository unidades,
                                  EnderecoRepository enderecos) {
        this.fazendas = fazendas;
        this.unidades = unidades;
        this.enderecos = enderecos;
    }

    @Operation(summary = "Pesquisar fazendas por nome")
    @GetMapping("/fazendas")
    public PaginaLocaisResponse listarFazendas(@RequestParam(defaultValue = "") String q,
                                               @RequestParam(defaultValue = "0") int pagina,
                                               @RequestParam(defaultValue = "20") int tamanho,
                                               Authentication authentication) {
        requireAuthentication(authentication);
        Page<FazendaEntity> resultado = fazendas.findByNomeContainingIgnoreCase(
                query(q), pageable(pagina, tamanho));
        Map<Integer, EnderecoEntity> locais = enderecos(resultado.map(FazendaEntity::getEnderecoId).getContent());
        return new PaginaLocaisResponse(resultado.getContent().stream()
                .map(f -> LocalDiarioResponse.of(f.getId(), f.getNome(), locais.get(f.getEnderecoId())))
                .toList(), pagina, tamanho, resultado.getTotalElements(), resultado.getTotalPages());
    }

    @Operation(summary = "Pesquisar unidades frigoríficas por nome")
    @GetMapping("/unidades-frigorificas")
    public PaginaLocaisResponse listarUnidades(@RequestParam(defaultValue = "") String q,
                                               @RequestParam(defaultValue = "0") int pagina,
                                               @RequestParam(defaultValue = "20") int tamanho,
                                               Authentication authentication) {
        requireAuthentication(authentication);
        Page<UnidadeFrigorificaEntity> resultado = unidades.findByNomeContainingIgnoreCase(
                query(q), pageable(pagina, tamanho));
        Map<Integer, EnderecoEntity> locais = enderecos(resultado.map(UnidadeFrigorificaEntity::getEnderecoId).getContent());
        return new PaginaLocaisResponse(resultado.getContent().stream()
                .map(u -> LocalDiarioResponse.of(u.getId(), u.getNome(), locais.get(u.getEnderecoId())))
                .toList(), pagina, tamanho, resultado.getTotalElements(), resultado.getTotalPages());
    }

    private static void requireAuthentication(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated())
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação necessária.");
    }

    private static String query(String value) {
        if (value.length() > 100)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Busca deve ter até 100 caracteres.");
        return value.trim();
    }

    private static PageRequest pageable(int pagina, int tamanho) {
        if (pagina < 0 || tamanho < 1 || tamanho > 50)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Paginação inválida.");
        return PageRequest.of(pagina, tamanho, Sort.by("nome").ascending().and(Sort.by("id")));
    }

    private Map<Integer, EnderecoEntity> enderecos(List<Integer> ids) {
        return enderecos.findAllById(ids.stream().filter(id -> id != null).distinct().toList())
                .stream().collect(Collectors.toMap(EnderecoEntity::getId, Function.identity()));
    }

    public record LocalDiarioResponse(Integer id, String nome, String cidade, String uf) {
        static LocalDiarioResponse of(Integer id, String nome, EnderecoEntity endereco) {
            return new LocalDiarioResponse(id, nome,
                    endereco == null ? null : endereco.getCidade(),
                    endereco == null ? null : endereco.getEstado());
        }
    }

    public record PaginaLocaisResponse(List<LocalDiarioResponse> itens, int pagina, int tamanho,
                                       long total, int totalPaginas) { }
}
