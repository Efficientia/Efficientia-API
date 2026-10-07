package com.example.efficientia.empresa.service;

import com.example.efficientia.auth.api.AutenticacaoInvalidaException;
import com.example.efficientia.auth.service.JwtTokenService;
import com.example.efficientia.empresa.api.EmpresaContracts.LoginEmpresaRequest;
import com.example.efficientia.empresa.api.EmpresaContracts.LoginEmpresaResponse;
import com.example.efficientia.empresa.domain.Empresa;
import com.example.efficientia.empresa.domain.EmpresaAdmin;
import com.example.efficientia.empresa.persistence.EmpresaAdminRepository;
import com.example.efficientia.empresa.persistence.EmpresaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class EmpresaServiceAuthenticationTest {

    private final EmpresaRepository empresaRepository = mock(EmpresaRepository.class);
    private final EmpresaAdminRepository adminRepository = mock(EmpresaAdminRepository.class);
    private final CodigoEmpresaGenerator codigoGenerator = mock(CodigoEmpresaGenerator.class);
    private final JwtTokenService jwtTokenService = mock(JwtTokenService.class);
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    private EmpresaService service;
    private Empresa empresa;
    private EmpresaAdmin admin;

    @BeforeEach
    void setUp() {
        service = new EmpresaService(empresaRepository, adminRepository, codigoGenerator, jwtTokenService);
        empresa = new Empresa(7L, "FRI12345", "Friboi", "12345678000195", "contato@friboi.com.br",
                passwordEncoder.encode("senha-corporativa"), true, Instant.now(), Instant.now());
        admin = new EmpresaAdmin(11L, 7L, "FRI12345", empresa.getCnpj(), "Ana Admin", "ana@friboi.com.br",
                "12345678901", "67999990000", "Gestora", passwordEncoder.encode("senha-admin"), true,
                Instant.now(), Instant.now());
        when(adminRepository.contarPorEmpresaId(7L)).thenReturn(1L);
        when(adminRepository.listarPorEmpresaId(7L)).thenReturn(List.of(admin));
        when(empresaRepository.buscarPorEmail("contato@friboi.com.br")).thenReturn(Optional.of(empresa));
        when(empresaRepository.buscarPorCnpj(empresa.getCnpj())).thenReturn(Optional.of(empresa));
        when(empresaRepository.buscarPorCodigo(empresa.getCodigoEmpresa())).thenReturn(Optional.of(empresa));
        when(empresaRepository.buscarPorId(7L)).thenReturn(Optional.of(empresa));
        when(jwtTokenService.gerarTokenAdmin(admin, empresa)).thenReturn("jwt-admin");
    }

    @Test
    void autenticaAdministradorAtivoEEmiteToken() {
        LoginEmpresaResponse response = service.autenticarEmpresa(new LoginEmpresaRequest(" CONTATO@FRIBOI.COM.BR ", "senha-admin"));

        assertEquals("ATIVO", response.status());
        assertEquals("jwt-admin", response.token());
        assertEquals("PAINEL_ADMINISTRATIVO", response.proximoPasso());
        verify(jwtTokenService).gerarTokenAdmin(admin, empresa);
    }

    @Test
    void autenticaComSenhaCorporativaEAssociaPrimeiroAdminAtivo() {
        LoginEmpresaResponse response = service.autenticarEmpresa(new LoginEmpresaRequest("FRI12345", "senha-corporativa"));

        assertEquals("ATIVO", response.status());
        assertEquals("jwt-admin", response.token());
        verify(jwtTokenService).gerarTokenAdmin(admin, empresa);
    }

    @Test
    void autenticaComSenhaCorporativaMesmoSemAdminAtivo() {
        when(adminRepository.listarPorEmpresaId(7L)).thenReturn(List.of(
                new EmpresaAdmin(12L, 7L, "FRI12345", empresa.getCnpj(), "Inativo", "inativo@friboi.com.br",
                        null, null, null, passwordEncoder.encode("outra-senha"), false, Instant.now(), Instant.now())
        ));

        LoginEmpresaResponse response = service.autenticarEmpresa(new LoginEmpresaRequest("12345678000195", "senha-corporativa"));

        assertEquals("ATIVO", response.status());
        assertNull(response.token());
        verifyNoInteractions(jwtTokenService);
    }

    @Test
    void rejeitaSenhaIncorretaEEmpresaInexistente() {
        assertThrows(AutenticacaoInvalidaException.class,
                () -> service.autenticarEmpresa(new LoginEmpresaRequest("7", "senha-incorreta")));

        when(empresaRepository.buscarPorCodigo("NAOEXISTE")).thenReturn(Optional.empty());
        assertThrows(ResponseStatusException.class,
                () -> service.autenticarEmpresa(new LoginEmpresaRequest("NAOEXISTE", null)));
    }

    @Test
    void consultaStatusSemSenhaEUsaCnpjFormatadoComoIdentificador() {
        LoginEmpresaResponse response = service.autenticarEmpresa(
                new LoginEmpresaRequest("12.345.678/0001-95", " "));

        assertEquals("ATIVO", response.status());
        assertEquals("LOGIN_ADMINISTRADOR", response.proximoPasso());
        verifyNoInteractions(jwtTokenService);
    }
}
