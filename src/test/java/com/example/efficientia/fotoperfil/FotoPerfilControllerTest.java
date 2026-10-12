package com.example.efficientia.fotoperfil;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class FotoPerfilControllerTest {
    @Test
    void tokenDeAdministradorComMesmoIdNumericoNaoAcessaFotoDoMotorista() {
        FotoPerfilService service = mock(FotoPerfilService.class);
        FotoPerfilController controller = new FotoPerfilController(service);
        Jwt jwt = Jwt.withTokenValue("token").header("alg", "HS256")
                .claim("usuario_id", 7).build();
        JwtAuthenticationToken admin = new JwtAuthenticationToken(jwt,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));

        ResponseStatusException error = assertThrows(ResponseStatusException.class,
                () -> controller.conteudo(admin));

        assertEquals(HttpStatus.FORBIDDEN, error.getStatusCode());
        verifyNoInteractions(service);
    }
}
