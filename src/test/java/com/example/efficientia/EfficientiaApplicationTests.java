package com.example.efficientia;

import com.example.efficientia.cadastrobase.persistence.EnderecoRepository;
import com.example.efficientia.cadastrobase.persistence.FazendaRepository;
import com.example.efficientia.cadastrobase.persistence.UsuarioRepository;
import com.example.efficientia.cadastrobase.persistence.VeiculoCarretaRepository;
import com.example.efficientia.cadastrobase.persistence.VeiculoCavaloRepository;
import com.example.efficientia.documento.persistence.DocumentoRepository;
import com.example.efficientia.documento.audit.DocumentoAuditRepository;
import com.example.efficientia.relatorioviagem.persistence.RelatorioViagemRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(EfficientiaApplicationTests.TestDependencies.class)
class EfficientiaApplicationTests {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void contextLoads() {
	}

	@Test
	void devePublicarContratoOpenApiDosDocumentos() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.info.title").value("Efficientia API REST Principal"))
				.andExpect(jsonPath("$.paths['/api/v1/documentos/{id}/conteudo'].get.responses['409']").exists())
				.andExpect(jsonPath("$.paths['/api/v1/documentos/{id}'].patch.responses['409']").exists());
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
		DocumentoAuditRepository documentoAuditRepository() {
			return Mockito.mock(DocumentoAuditRepository.class);
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

		@Bean
		com.example.efficientia.exportacao.persistence.ExportacaoRepository exportacaoRepository() {
			return Mockito.mock(com.example.efficientia.exportacao.persistence.ExportacaoRepository.class);
		}

		@Bean
		@org.springframework.context.annotation.Primary
		com.example.efficientia.documento.storage.StorageService storageService() {
			return Mockito.mock(com.example.efficientia.documento.storage.StorageService.class);
		}
	}

}
