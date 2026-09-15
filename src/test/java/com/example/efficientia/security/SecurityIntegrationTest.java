package com.example.efficientia.security;

import com.example.efficientia.api.ApiStatusController;
import com.example.efficientia.auth.service.JwtTokenService;
import com.example.efficientia.cadastrobase.domain.TipoUsuario;
import com.example.efficientia.cadastrobase.persistence.UsuarioEntity;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = ApiStatusController.class,
        properties = {
                "app.security.enabled=true",
                "app.security.audience=efficientia-api",
                "app.security.secret=efficientia-secret-key-must-be-at-least-32-bytes-long!"
        }
)
@Import({SecurityConfig.class, JwtTokenService.class})
@EnableConfigurationProperties(SecurityProperties.class)
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JwtTokenService jwtTokenService;

    @Test
    void rotaStatusDeveSerPublica() throws Exception {
        mockMvc.perform(get("/api/v1/status"))
                .andExpect(status().isOk());
    }

    @Test
    void rotasProtegidasDevemRetornar401SemToken() throws Exception {
        mockMvc.perform(get("/api/v1/documentos"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/relatorios-viagem"))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(get("/api/v1/usuarios"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenHs256ValidoDeveAcessarRotasPermitidas() throws Exception {
        mockMvc.perform(get("/api/v1/documentos")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_MOTORISTA"))))
                .andExpect(status().isNotFound()); // NotFound porque o controller Documentos não está carregado neste slice, mas 401/403 passou!

        mockMvc.perform(get("/api/v1/relatorios-viagem")
                        .with(jwt().authorities(new SimpleGrantedAuthority("ROLE_ANALISTA"))))
                .andExpect(status().isNotFound());
    }
}
