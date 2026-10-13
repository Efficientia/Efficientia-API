package com.example.efficientia.security;

import com.example.efficientia.api.ApiStatusController;
import com.example.efficientia.auth.service.JwtTokenService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ApiStatusController.class, properties = {
        "app.security.enabled=false",
        "app.security.secret=efficientia-secret-key-must-be-at-least-32-bytes-long!"
})
@Import({SecurityConfig.class, JwtTokenService.class})
@EnableConfigurationProperties(SecurityProperties.class)
class DiarioLocalSecurityTest {
    @Autowired MockMvc mockMvc;

    @Test
    void diarioECatalogoContinuamProtegidosNoModoLocal() throws Exception {
        mockMvc.perform(get("/api/v1/relatorios-viagem/me")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/locais-diario/fazendas")).andExpect(status().isUnauthorized());
        mockMvc.perform(get("/api/v1/relatorios-viagem/me").with(jwt()))
                .andExpect(status().isNotFound());
    }
}
