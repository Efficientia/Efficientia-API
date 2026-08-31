package com.example.efficientia;

import com.example.efficientia.cadastrobase.persistence.EnderecoRepository;
import com.example.efficientia.cadastrobase.persistence.FazendaRepository;
import com.example.efficientia.cadastrobase.persistence.UsuarioRepository;
import com.example.efficientia.cadastrobase.persistence.VeiculoCarretaRepository;
import com.example.efficientia.cadastrobase.persistence.VeiculoCavaloRepository;
import com.example.efficientia.documento.persistence.DocumentoRepository;
import com.example.efficientia.relatorioviagem.persistence.RelatorioViagemRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
@Import(EfficientiaApplicationTests.TestDependencies.class)
class EfficientiaApplicationTests {

	@Test
	void contextLoads() {
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class TestDependencies {

		@Bean
		RelatorioViagemRepository relatorioViagemRepository() {
			return Mockito.mock(RelatorioViagemRepository.class);
		}

		@Bean
		DocumentoRepository documentoRepository() {
			return Mockito.mock(DocumentoRepository.class);
		}

		@Bean
		UsuarioRepository usuarioRepository() {
			return Mockito.mock(UsuarioRepository.class);
		}

		@Bean
		EnderecoRepository enderecoRepository() {
			return Mockito.mock(EnderecoRepository.class);
		}

		@Bean
		FazendaRepository fazendaRepository() {
			return Mockito.mock(FazendaRepository.class);
		}

		@Bean
		VeiculoCavaloRepository veiculoCavaloRepository() {
			return Mockito.mock(VeiculoCavaloRepository.class);
		}

		@Bean
		VeiculoCarretaRepository veiculoCarretaRepository() {
			return Mockito.mock(VeiculoCarretaRepository.class);
		}
	}

}
