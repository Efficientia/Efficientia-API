package com.example.efficientia.fotoperfil;

import com.example.efficientia.cadastrobase.domain.TipoUsuario;
import com.example.efficientia.cadastrobase.persistence.UsuarioEntity;
import com.example.efficientia.cadastrobase.persistence.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class FotoPerfilServiceTest {
    private static final byte[] JPEG = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x00,
            (byte) 0xff, (byte) 0xd9};
    private UsuarioRepository repository;
    private FotoPerfilService service;
    private UsuarioEntity driver;

    @BeforeEach
    void setup() {
        repository = mock(UsuarioRepository.class);
        service = new FotoPerfilService(repository);
        driver = new UsuarioEntity();
        driver.setId(7);
        driver.setTipo(TipoUsuario.motorista);
        when(repository.findById(7)).thenReturn(Optional.of(driver));
    }

    @Test
    void salvaConsultaERemoveFotoDoMotorista() {
        service.salvar(7, JPEG, "image/jpeg");
        assertArrayEquals(JPEG, service.buscar(7).bytes());
        assertEquals("image/jpeg", service.buscar(7).mime());
        assertNotNull(driver.getFotoPerfilAtualizadaEm());
        service.remover(7);
        assertNull(driver.getFotoPerfil());
        assertEquals(HttpStatus.NOT_FOUND,
                assertThrows(ResponseStatusException.class, () -> service.buscar(7)).getStatusCode());
        verify(repository, times(2)).save(driver);
    }

    @Test
    void rejeitaArquivoGrandeEFormatoInvalido() {
        assertEquals(HttpStatus.PAYLOAD_TOO_LARGE,
                assertThrows(ResponseStatusException.class,
                        () -> service.salvar(7, new byte[FotoPerfilService.MAX_BYTES + 1], "image/jpeg"))
                        .getStatusCode());
        assertEquals(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                assertThrows(ResponseStatusException.class,
                        () -> service.salvar(7, JPEG, "image/png")).getStatusCode());
        verify(repository, never()).save(any());
    }

    @Test
    void impedePerfilQueNaoEDeMotorista() {
        driver.setTipo(TipoUsuario.administrador);
        assertEquals(HttpStatus.FORBIDDEN,
                assertThrows(ResponseStatusException.class,
                        () -> service.salvar(7, JPEG, "image/jpeg")).getStatusCode());
    }
}
