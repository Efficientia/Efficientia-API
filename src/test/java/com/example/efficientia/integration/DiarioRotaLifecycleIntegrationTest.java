package com.example.efficientia.integration;

import com.example.efficientia.caminhao.service.CaminhaoService;
import com.example.efficientia.cadastrobase.persistence.UsuarioEntity;
import com.example.efficientia.cadastrobase.persistence.UsuarioRepository;
import com.example.efficientia.relatorioviagem.api.RelatorioViagemController;
import com.example.efficientia.relatorioviagem.persistence.AnomaliaDesembarqueRepository;
import com.example.efficientia.relatorioviagem.persistence.AnomaliaEmbarqueRepository;
import com.example.efficientia.relatorioviagem.persistence.ParadaImprevistaRepository;
import com.example.efficientia.relatorioviagem.persistence.RelatorioViagemEntity;
import com.example.efficientia.relatorioviagem.persistence.RelatorioViagemRepository;
import com.example.efficientia.relatorioviagem.service.RelatorioViagemService;
import com.jayway.jsonpath.JsonPath;
import jakarta.persistence.EntityManagerFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import javax.sql.DataSource;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** HTTP binding, validation, transactional service and persistence; no production connection. */
@SpringJUnitConfig(DiarioRotaLifecycleIntegrationTest.PersistenceConfig.class)
class DiarioRotaLifecycleIntegrationTest {
    private static final String URL = "/api/v1/relatorios-viagem";
    private static final String ASSINATURA_FIXA_MOTORISTA = "/api/v1/usuarios/18/assinatura/conteudo";
    private static final String PRIMEIRA_ETAPA = """
            {"status":"rascunho","numeroGta":"6767a","numeroNotaFiscal":"56767",
             "nomeOrigem":"Fazenda Do Osmar","nomeDestino":"Fazenda Do Noé"}
            """;

    @Autowired RelatorioViagemService service;
    @Autowired RelatorioViagemRepository repository;
    @Autowired ParadaImprevistaRepository paradas;
    @Autowired AnomaliaEmbarqueRepository anomaliasEmbarque;
    @Autowired AnomaliaDesembarqueRepository anomaliasDesembarque;
    @Autowired LocalValidatorFactoryBean validator;
    private MockMvc mvc;
    private final JwtAuthenticationToken motorista = new JwtAuthenticationToken(
            Jwt.withTokenValue("jwt-sintetico-apenas-para-teste").header("alg", "none")
                    .subject("18").claim("usuario_id", 18).claim("empresa_id", 3).build(),
            List.of(new SimpleGrantedAuthority("ROLE_MOTORISTA")));

    @BeforeEach
    void preparar() {
        paradas.deleteAll();
        anomaliasEmbarque.deleteAll();
        anomaliasDesembarque.deleteAll();
        repository.deleteAll();
        mvc = MockMvcBuilders.standaloneSetup(new RelatorioViagemController(service, mock(CaminhaoService.class)))
                .setValidator(validator).build();
    }

    @Test
    void deveCriarAvancarSeisEtapasRetomarEditarESubmeterMesmoDiario() throws Exception {
        int id = criarRascunhoCompletoEmEtapas();
        mvc.perform(get(URL + "/me").principal(motorista))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(id));
        mvc.perform(get(URL + "/" + id).principal(motorista))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nomeOrigem").value("Fazenda Do Osmar"))
                .andExpect(jsonPath("$.paradasImprevistas[0].motivo").value("outro"));

        salvar(id, """
                {"status":"rascunho","comentarios":"Retomado pela home"}
                """).andExpect(status().isOk())
                .andExpect(jsonPath("$.numeroGta").value("6767a"))
                .andExpect(jsonPath("$.nomeDestino").value("Fazenda Do Noé"));

        UUID chave = UUID.randomUUID();
        mvc.perform(put(URL + "/" + id).principal(motorista).header("Idempotency-Key", chave)
                        .contentType(APPLICATION_JSON).content("{\"status\":\"pendente\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("pendente"));
        var enviadoEm = repository.findById(id).orElseThrow().getEnviadoEm();
        mvc.perform(get(URL + "/me").principal(motorista))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(1))
                .andExpect(jsonPath("$.itens[0].id").value(id))
                .andExpect(jsonPath("$.itens[0].status").value("pendente"));
        mvc.perform(get(URL + "/" + id).principal(motorista))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("pendente"));

        // A pending diary already has its submission key: a partial edit is not a new submission.
        salvar(id, """
                {"status":"pendente","comentarios":"Editado pela home","numeroNotaFiscal":"56768"}
                """).andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("pendente"))
                .andExpect(jsonPath("$.numeroNotaFiscal").value("56768"))
                .andExpect(jsonPath("$.paradasImprevistas.length()").value(1));
        var salvo = repository.findById(id).orElseThrow();
        assertThat(salvo.getIdempotencyKey()).isEqualTo(chave);
        assertThat(salvo.getEnviadoEm()).isEqualTo(enviadoEm);
        assertThat(salvo.getNomeOrigem()).isEqualTo("Fazenda Do Osmar");
        assertThat(salvo.getUrlAssinaturaMotorista()).isEqualTo(ASSINATURA_FIXA_MOTORISTA);
        assertThat(salvo.getFazendaId()).isNull();
        assertThat(salvo.getUnidadeFrigorificaId()).isNull();
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void deveRejeitarPrimeiraSubmissaoSemChaveSemAlterarRascunho() throws Exception {
        int id = criarRascunhoCompletoEmEtapas();
        salvar(id, "{\"status\":\"pendente\",\"comentarios\":\"Não salvar\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors['Idempotency-Key']").exists());
        var salvo = repository.findById(id).orElseThrow();
        assertThat(salvo.getStatus()).isEqualTo("rascunho");
        assertThat(salvo.getComentarios()).isEqualTo("Sexta etapa");
        assertThat(salvo.getIdempotencyKey()).isNull();
        assertThat(salvo.getEnviadoEm()).isNull();
    }

    @Test
    void deveRejeitarEdicaoIncoerenteDePendenteSemPersistirDadosRejeitados() throws Exception {
        int id = criarRascunhoCompletoEmEtapas();
        mvc.perform(put(URL + "/" + id).principal(motorista).header("Idempotency-Key", UUID.randomUUID())
                        .contentType(APPLICATION_JSON).content("{\"status\":\"pendente\"}"))
                .andExpect(status().isOk());
        salvar(id, "{\"status\":\"pendente\",\"kmChegadaDesembarcadouro\":60,\"comentarios\":\"Não salvar\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.kmChegadaDesembarcadouro").exists());
        var salvo = repository.findById(id).orElseThrow();
        assertThat(salvo.getStatus()).isEqualTo("pendente");
        assertThat(salvo.getKmSaidaEmbarcadouro()).isEqualTo(67);
        assertThat(salvo.getKmChegadaDesembarcadouro()).isEqualTo(167);
        assertThat(salvo.getComentarios()).isEqualTo("Sexta etapa");
        assertThat(paradas.findByRelatorioId(id)).hasSize(1);
    }

    @Test
    void deveCorrigirEtapasDeRascunhoRetomadoSeparadamenteEValidarConjuntoNaSubmissao() throws Exception {
        int id = criarRascunhoCompletoEmEtapas();
        mvc.perform(get(URL + "/me").principal(motorista))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens[0].id").value(id));
        mvc.perform(get(URL + "/" + id).principal(motorista))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.quantidadeMachos").value(2))
                .andExpect(jsonPath("$.quantidadeEmPe").value(3));

        // The driver corrects embarkation first and the later physical-condition stage afterwards.
        salvar(id, "{\"status\":\"rascunho\",\"quantidadeMachos\":1}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAnimais").value(2))
                .andExpect(jsonPath("$.quantidadeEmPe").value(3));
        UUID chave = UUID.randomUUID();
        mvc.perform(put(URL + "/" + id).principal(motorista).header("Idempotency-Key", chave)
                        .contentType(APPLICATION_JSON).content("{\"status\":\"pendente\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.condicaoAnimais").exists());
        var rascunho = repository.findById(id).orElseThrow();
        assertThat(rascunho.getStatus()).isEqualTo("rascunho");
        assertThat(rascunho.getQuantidadeMachos()).isEqualTo(1);
        assertThat(rascunho.getIdempotencyKey()).isNull();

        salvar(id, "{\"status\":\"rascunho\",\"quantidadeEmPe\":2}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalAnimais").value(2))
                .andExpect(jsonPath("$.quantidadeEmPe").value(2));
        mvc.perform(put(URL + "/" + id).principal(motorista).header("Idempotency-Key", chave)
                        .contentType(APPLICATION_JSON).content("{\"status\":\"pendente\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(id))
                .andExpect(jsonPath("$.status").value("pendente"));
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void deveExigirChaveEmPendenteLegadoSemChaveEPermitirEdicaoAposVincular() throws Exception {
        int id = criarRascunhoCompletoEmEtapas();
        var legado = repository.findById(id).orElseThrow();
        legado.setStatus("pendente");
        repository.saveAndFlush(legado);
        salvar(id, "{\"status\":\"pendente\",\"comentarios\":\"Edição legado\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors['Idempotency-Key']").exists());

        UUID chave = UUID.randomUUID();
        mvc.perform(put(URL + "/" + id).principal(motorista).header("Idempotency-Key", chave)
                        .contentType(APPLICATION_JSON).content("{\"status\":\"pendente\",\"comentarios\":\"Edição legado\"}"))
                .andExpect(status().isOk());
        salvar(id, "{\"status\":\"pendente\",\"comentarios\":\"Nova edição\"}")
                .andExpect(status().isOk());
        assertThat(repository.findById(id).orElseThrow().getIdempotencyKey()).isEqualTo(chave);
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void deveExigirEtapasFuturasApenasNoEnvioFinalESemVincularChaveQuandoRejeitado() throws Exception {
        int id = criarPrimeiraEtapa();
        mvc.perform(put(URL + "/" + id).principal(motorista).header("Idempotency-Key", UUID.randomUUID())
                        .contentType(APPLICATION_JSON).content("{\"status\":\"pendente\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.dataChegadaUnidade").exists())
                .andExpect(jsonPath("$.fieldErrors.horarioDesembarque").exists());
        var salvo = repository.findById(id).orElseThrow();
        assertThat(salvo.getStatus()).isEqualTo("rascunho");
        assertThat(salvo.getIdempotencyKey()).isNull();
        assertThat(salvo.getEnviadoEm()).isNull();
        assertThat(repository.count()).isEqualTo(1);
    }

    @Test
    void deveRetornarErrosDeCamposNoPutESemExigirEtapasFuturasNoPost() throws Exception {
        int id = criarPrimeiraEtapa();
        salvar(id, "{\"status\":\"rascunho\",\"kmSaidaEmbarcadouro\":-1}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.title").value("Campos Inválidos"))
                .andExpect(jsonPath("$.fieldErrors.kmSaidaEmbarcadouro").exists());
        assertThat(repository.findById(id).orElseThrow().getKmSaidaEmbarcadouro()).isNull();
        assertThat(repository.count()).isEqualTo(1);
    }

    private int criarPrimeiraEtapa() throws Exception {
        var resposta = mvc.perform(post(URL).principal(motorista).contentType(APPLICATION_JSON).content(PRIMEIRA_ETAPA))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNumber())
                .andExpect(jsonPath("$.status").value("rascunho"))
                .andExpect(jsonPath("$.motoristaId").value(18))
                .andExpect(jsonPath("$.nomeOrigem").value("Fazenda Do Osmar"))
                .andExpect(jsonPath("$.nomeDestino").value("Fazenda Do Noé"))
                .andExpect(jsonPath("$.dataChegadaUnidade").doesNotExist())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        return JsonPath.read(resposta, "$.id");
    }

    private int criarRascunhoCompletoEmEtapas() throws Exception {
        int id = criarPrimeiraEtapa();
        List<String> etapas = List.of("""
                {"status":"rascunho","dataEmbarque":"2026-09-11","horarioEmbarque":"08:44:00",
                 "horarioSaidaPropriedade":"10:12:00","kmSaidaEmbarcadouro":67}
                """, """
                {"status":"rascunho","quantidadeMachos":2,"quantidadeFemeas":1,"quantidadeMarrucos":0,
                 "anomaliasEmbarque":[]}
                """, """
                {"status":"rascunho","paradasImprevistas":[{"motivo":"outro","descricaoMotivo":"Pausa para água",
                 "dataHoraInicio":"2026-09-11T11:00:00","dataHoraFim":"2026-09-11T11:10:00"}]}
                """, """
                {"status":"rascunho","dataChegadaUnidade":"2026-09-11","horarioChegadaUnidade":"12:00:00",
                 "horarioDesembarque":"12:30:00","kmChegadaDesembarcadouro":167,"numeroCurral":"C1",
                 "sireneReFuncionou":true,"quantidadeEmPe":3,"quantidadeDeitado":0,"quantidadeMorto":0,
                 "quantidadeEmergencia":0,"anomaliasDesembarque":[]}
                """, """
                {"status":"rascunho","urlAssinaturaPecuarista":"/assinatura/pecuarista.png",
                 "urlAssinaturaMotorista":"/assinatura/forjada-pelo-cliente.png","urlAssinaturaManobrista":"/assinatura/manobrista.png",
                 "urlAssinaturaCurraleiro":"/assinatura/curraleiro.png","comentarios":"Sexta etapa"}
                """);
        for (String etapa : etapas) {
            salvar(id, etapa).andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(id))
                    .andExpect(jsonPath("$.status").value("rascunho"))
                    .andExpect(jsonPath("$.numeroGta").value("6767a"))
                    .andExpect(jsonPath("$.urlAssinaturaMotorista").value(ASSINATURA_FIXA_MOTORISTA));
            assertThat(repository.count()).isEqualTo(1);
        }
        return id;
    }

    private ResultActions salvar(int id, String corpo) throws Exception {
        return mvc.perform(put(URL + "/" + id).principal(motorista).contentType(APPLICATION_JSON).content(corpo));
    }

    @Configuration(proxyBeanMethods = false)
    @EnableTransactionManagement
    @EnableJpaRepositories(basePackageClasses = RelatorioViagemRepository.class)
    static class PersistenceConfig {
        @Bean DataSource dataSource() {
            return new EmbeddedDatabaseBuilder().generateUniqueName(true).setType(EmbeddedDatabaseType.H2).build();
        }

        @Bean LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource) {
            var factory = new LocalContainerEntityManagerFactoryBean();
            factory.setDataSource(dataSource);
            factory.setPackagesToScan(RelatorioViagemEntity.class.getPackageName());
            factory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
            factory.setJpaPropertyMap(Map.of("hibernate.hbm2ddl.auto", "create-drop",
                    "hibernate.hbm2ddl.create_namespaces", "true", "hibernate.show_sql", "false"));
            return factory;
        }

        @Bean PlatformTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
            return new JpaTransactionManager(entityManagerFactory);
        }

        @Bean RelatorioViagemService relatorioViagemService(RelatorioViagemRepository repository,
                UsuarioRepository usuarios,
                ParadaImprevistaRepository paradas, AnomaliaEmbarqueRepository embarque,
                AnomaliaDesembarqueRepository desembarque) {
            return new RelatorioViagemService(repository, usuarios, null, paradas, embarque, desembarque);
        }

        @Bean UsuarioRepository usuarioRepository() {
            var usuarios = mock(UsuarioRepository.class);
            var motorista = new UsuarioEntity();
            motorista.setId(18);
            motorista.setEmpresaId(3);
            motorista.setUrlAssinaturaGeral(ASSINATURA_FIXA_MOTORISTA);
            when(usuarios.findById(18)).thenReturn(Optional.of(motorista));
            return usuarios;
        }

        @Bean LocalValidatorFactoryBean validator() {
            return new LocalValidatorFactoryBean();
        }
    }
}
