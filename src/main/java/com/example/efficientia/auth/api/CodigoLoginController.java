package com.example.efficientia.auth.api;

import com.example.efficientia.auth.service.CodigoLoginService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth/codigo")
public class CodigoLoginController {
    private final CodigoLoginService service;

    public CodigoLoginController(CodigoLoginService service) { this.service = service; }

    public record SolicitarRequest(@NotBlank @Email String email,
                                  @NotBlank @Pattern(regexp = "motorista|administrador") String perfil) { }
    public record ValidarRequest(@NotBlank @Email String email,
                                @NotBlank @Pattern(regexp = "motorista|administrador") String perfil,
                                @NotBlank @Pattern(regexp = "[0-9]{6}") String codigo) { }

    @PostMapping("/solicitar")
    public Map<String, String> solicitar(@Valid @RequestBody SolicitarRequest request) {
        service.solicitar(request.email(), request.perfil());
        return Map.of("mensagem", "Se o e-mail estiver cadastrado, o código será enviado.");
    }

    @PostMapping("/validar")
    public Object validar(@Valid @RequestBody ValidarRequest request) {
        return service.validar(request.email(), request.perfil(), request.codigo());
    }
}
