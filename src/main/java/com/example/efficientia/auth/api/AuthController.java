package com.example.efficientia.auth.api;

import com.example.efficientia.auth.api.AuthContracts.LoginRequest;
import com.example.efficientia.auth.api.AuthContracts.LoginResponse;
import com.example.efficientia.auth.api.AuthContracts.SignupRequest;
import com.example.efficientia.auth.service.AuthService;
import com.example.efficientia.cadastrobase.api.CadastroBaseContracts.UsuarioResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/signup")
    @ResponseStatus(HttpStatus.CREATED)
    public UsuarioResponse signup(@Valid @RequestBody SignupRequest request) {
        return authService.cadastrar(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.autenticar(request);
    }
}
