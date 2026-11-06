# Documentacao Tecnica e Consolidado Arquitetural do Sistema Efficientia

## 1. Introducao e Visao Geral do Sistema

O **Efficientia** e uma plataforma integrada de engenharia de software voltada a logistica de transporte pecuario de gado bovino vivo, rastreamento operacional de viagens, conformidade regulatoria e monitoramento de bem-estar animal. O sistema atua como o elo de comunicacao e controle entre as propriedades rurais de origem (fazendas produtoras) e as unidades industriais de abate e processamento frigorifico.

O projeto foi concebido e implementado no contexto da disciplina de **Desenvolvimento de Sistemas 2 (DS2)** do **Instituto J&F / Anima Educacao**, consolidado sob o epico estrutural **EFFICIENTI-94**.

### 1.1 Objetivo e Problema de Negocio
Historicamente, o transporte de gado bovino no Brasil depende de preenchimento manual em guias fisicas de papel e formularios impressos (como Guias de Transito Animal - GTA, notas fiscais, fichas de ocorrencia de estrada e comprovantes de pesagem). Esse modelo tradicional acarreta:
1. Elevado risco de perda ou adulteracao de informacoes operacionais.
2. Inconsistencias de transcricao de dados de quilometragem, horarios e contagens de rebanho.
3. Dificuldade de fiscalizacao e rastreabilidade quanto ao bem-estar dos animais (animais deitados, mortos ou em sofrimento emergencial).
4. Ausencia de auditoria juridica e forense das assinaturas coletadas nas etapas de carga, trajeto e descarga.

A solucao Efficientia resolve esses gargalos por meio da digitalizacao completa do fluxo, oferecendo coleta estruturada em campo, armazenamento com integridade criptografica (SHA-256), validacao estrita de tipos MIME por analise de magic bytes, resiliencia para conexoes instaveis com rascunhos progressivos e despacho de eventos para telemetria em tempo real.

### 1.2 Divisao de Responsabilidades no Ecossistema

O ecossistema Efficientia e composto por quatro camadas cooperantes:

1. **Aplicativo Mobile (Motorista e Operacao em Campo):**
   - Execucao nos sistemas operacionais Android e iOS.
   - Autenticacao simplificada validando vinculo empregaticio e perfil operacional.
   - Preenchimento do Diario de Rota em 6 etapas com salvamento automatico como rascunho parcial.
   - Selecao dinamica e checagem de disponibilidade de veiculos da frota (cavalos mecanicos e carretas boiadeiras).
   - Coleta de assinatura digital fixa do motorista e assinaturas contextuais de pecuaristas, manobristas e curraleiros.
   - Submissao final garantida por cabecalho de idempotencia (`Idempotency-Key`).

2. **Portal Web de Gestao Corporativa (Efficientia FrontEnd):**
   - Painel administrativo corporativo para analistas logisticos, auditores e administradores de transportadoras.
   - Fluxo de Onboarding corporativo de empresas parceiras, geracao de codigos empresariais de 8 digitos e upload de logotipo.
   - Gestao do primeiro acesso de administradores corporativos e controle de funcionarios vinculados.
   - Cadastro, inspecao sanitaria e manutencao da frota de caminhoes.
   - Consulta detalhada, aprovacao/reprovacao de relatorios de viagem e exportacao de lotes documentais.

3. **API REST Central (Efficientia-API):**
   - Servico em Spring Boot 4 / Java 17 responsavel pela aplicacao das regras de negocio, autorizacao RBAC, controle de transacoes, persistencia relacional e despacho de eventos.
   - Implanatado continuamente em infraestrutura de nuvem no provedor Render (`https://efficientia-api.onrender.com`).

4. **Camada de Dados e Mensageria (PostgreSQL e Redis):**
   - PostgreSQL (Supabase / local / Testcontainers): banco relacional multi-schema para ACID estrito.
   - Redis / Upstash: filas assincronas de streaming (Redis Streams) via padrao Transactional Outbox e tabelas classificatorias de motoristas (leaderboards).

Para a especificacao detalhada da integracao entre o aplicativo mobile e a API, consulte o documento [INTEGRACAO_DIARIO_MOBILE_2026-10-13.md](docs/INTEGRACAO_DIARIO_MOBILE_2026-10-13.md) e o mapeamento de contratos em [API_MAP.md](docs/API_MAP.md).

---

## 2. Arquitetura Tecnica e Stack Tecnologica

A API foi projetada segundo os principios do Domain-Driven Design (DDD) e Clean Architecture, adotando camadas bem delimitadas de controladores REST, servicos de aplicacao, contratos de integracao, entidades de persistencia e manipuladores globais de excecao.

### 2.1 Especificacao da Stack

| Camada | Tecnologia | Versao / Detalhes |
| :--- | :--- | :--- |
| **Linguagem de Programacao** | Java (OpenJDK / Eclipse Temurin) | 17 (LTS) |
| **Framework Central** | Spring Boot | 4.1.0 |
| **Seguranca e Identidade** | Spring Security 6 / OAuth2 Resource Server | JWT simetrico HS256, BCrypt, RBAC |
| **Persistencia e ORM** | Spring Data JPA / Hibernate | 7.x com HikariCP connection pool |
| **Banco de Dados Relacional** | PostgreSQL | 15+ (Suporte a schemas segregados) |
| **Versionamento de Banco** | Flyway Database Migrations | 10+ (Migracoes V1 a V20) |
| **Mensageria e Caching** | Spring Data Redis (Driver Lettuce) | Redis Streams, Sorted Sets |
| **Documentacao Interativa** | SpringDoc OpenAPI | OpenAPI 3.0 / Swagger UI 3.1.0 |
| **Observabilidade e Metricas** | Spring Boot Actuator, Micrometer, Prometheus | Tracing via MDC (`X-Correlation-Id`) |
| **Conteinerizacao** | Docker | Build multi-stage com `eclipse-temurin:17-jre-jammy` |
| **Testes Automatizados** | JUnit 5, Mockito, Testcontainers, H2 | JaCoCo Coverage Enforcement (80% Linhas, 60% Branches) |

### 2.2 Estrutura de Pacotes do Codigo-Fonte

O projeto organiza seus componentes sob o pacote raiz `com.example.efficientia`:

```
com.example.efficientia
├── actuator/              # Health indicators customizados (StorageHealthIndicator)
├── api/                   # Controladores globais de status da aplicacao
├── assinaturamotorista/   # Gestao de assinatura fixa do motorista (PNG, magic bytes, SHA-256)
├── auth/                  # Emissao, decodificacao de JWT e fluxos de login/signup
├── cadastrobase/          # Cadastros mestres (Usuarios, Fazendas, Frigorificos, Enderecos)
├── caminhao/              # Gestao da frota veicular (Cavalos e Carretas) e inspecoes
├── config/                # Processamento de URLs de banco, Dotenv e configuracoes Redis
├── documento/             # Upload multipart, storage privado, download e paginacao por cursor
├── empresa/               # Multi-tenant corporativo, onboarding, logos e administradores
├── exportacao/            # Processamento assincrono de lote e geracao de arquivos compactados ZIP
├── feedbackrota/          # Coleta de avaliacoes e apontamentos dos motoristas sobre o trajeto
├── fotoperfil/            # Upload, transmissao e exclusao de foto de perfil de usuarios
├── metrics/               # Metricas customizadas registradas no Micrometer/Prometheus
├── outbox/                # Relay transacional de eventos para Redis Streams
├── redis/                 # Servicos de ranking e leaderboards de motoristas
├── relatorioviagem/       # Diario de Rota em 6 etapas, rascunhos, paradas e anomalias
├── security/              # Cadeia de filtros do Spring Security e conversor de papeis JWT
└── temp/                  # Endpoints analiticos e visualizacao provisoria de tabelas
```

Para a descricao de todos os endpoints e contratos, consulte o arquivo [README.md](README.md) e o catalogo [docs/API_MAP.md](docs/API_MAP.md).

---

## 3. Historico Consolidado de Sprints e Entregas

A evolucao do backend foi estruturada em sprints sequenciais planejadas no Jira (`EFFICIENTI-94`). Cada entrega agregou funcionalidades independentes e retrocompativeis.

### 3.1 Detalhamento Sprint a Sprint

#### Sprint 01: Fundacao da API REST Principal, Spring Boot e CI (`EFFICIENTI-218`)
- **Escopo:** Criacao da estrutura base do projeto Java 17 com Spring Boot, configuracao do Maven Wrapper, endpoint de verificacao de saude operacional (`GET /api/v1/status`) e esteira automatizada no GitHub Actions (`maven-verify.yml`, `ci.yml`, `container-build.yml`).
- **Entregaveis:** Endpoints de health check, configuracao de profiles Spring e testes unitarios de inicializacao.
- **Especificacao de Referencia:** [docs/SPEC_INITIAL-STRUCTURE.md](docs/SPEC_INITIAL-STRUCTURE.md).

#### Sprint 02: Banco de Dados PostgreSQL e Migracoes Flyway (V1) (`EFFICIENTI-219`)
- **Escopo:** Modelagem relacional inicial em 3FN para PostgreSQL, integracao com pooler de conexoes HikariCP, scripts Flyway `V1__create_relational_schema.sql` e mapeamento JPA das entidades de usuarios operacionais, enderecos, fazendas, unidades frigorificas e veiculos base.
- **Entregaveis:** Migration V1, entidades `UsuarioEntity`, `EnderecoEntity`, `FazendaEntity`, `UnidadeFrigorificaEntity`, repositorios JPA e endpoints CRUD basicos em `/api/v1/...`.
- **Especificacao de Referencia:** [docs/SPEC-SPRINT-02-POSTGRESQL-FLYWAY.md](docs/SPEC-SPRINT-02-POSTGRESQL-FLYWAY.md).

#### Sprint 03: Dominio e Persistencia de Documentos Operacionais (V2) (`EFFICIENTI-220`)
- **Escopo:** Criacao da entidade `DocumentoEntity` e da migration `V2__create_documento.sql` para suportar metadados de GTAs, tickets de pesagem e notas fiscais vinculadas aos relatorios de viagem.
- **Entregaveis:** Migration V2, identificadores unicos publicos baseados em UUID e indices parciais em `viagem_id` e `idempotency_key`.
- **Especificacao de Referencia:** [docs/SPEC-SPRINT-03-DOMINIO-DOCUMENTOS.md](docs/SPEC-SPRINT-03-DOMINIO-DOCUMENTOS.md).

#### Sprint 04: Storage Privado e Integridade Criptografica (SHA-256) (`EFFICIENTI-221`)
- **Escopo:** Desenvolvimento do subsistema de armazenamento seguro de arquivos binarios em disco desacoplado (`StorageService` e `LocalStorageService`), calculo de hash SHA-256 em streaming (sem consumo excessivo de heap) e blindagem estrita contra ataques de *Path Traversal* (`../`).
- **Entregaveis:** Camada de storage, hashing continuo e testes unitarios de validacao de caminhos canonicos.
- **Especificacao de Referencia:** [docs/SPEC-SPRINT-04-STORAGE-PRIVADO.md](docs/SPEC-SPRINT-04-STORAGE-PRIVADO.md).

#### Sprint 05: Upload Multipart de PDF/PNG e Idempotencia (`EFFICIENTI-222`)
- **Escopo:** Criacao do endpoint HTTP `POST /api/v1/documentos` com `multipart/form-data`, validacao de metadados em JSON, limites de tamanho e tratamento de requisicoes duplicadas atraves do cabecalho `Idempotency-Key`.
- **Entregaveis:** Controller multipart, DTOs Jakarta Validation e garantia de idempotencia evitando regravacao de arquivos fisicos identicos.
- **Especificacao de Referencia:** [docs/SPEC-SPRINT-05-UPLOAD-MULTIPART.md](docs/SPEC-SPRINT-05-UPLOAD-MULTIPART.md).

#### Sprint 06: Assinaturas Digitais Multimodais (`EFFICIENTI-223`)
- **Escopo:** Captura de assinaturas nos formatos de Foto, Desenho (Canvas) e Texto digitado para motoristas, manobristas e curraleiros, com exclusividade mutua entre arquivo binario e conteudo textual.
- **Entregaveis:** Validador `AssinaturaValidator` e suporte ao enum `PapelAssinante`.
- **Especificacao de Referencia:** [docs/SPEC-SPRINT-06-ASSINATURAS.md](docs/SPEC-SPRINT-06-ASSINATURAS.md).

#### Sprint 07: Listagem Paginada, Filtros e Cursor Opaco para Mobile/IA (`EFFICIENTI-224`)
- **Escopo:** Consulta de documentos (`GET /api/v1/documentos`) oferecendo tanto paginacao por offset/limit quanto paginacao continua estavel por cursor opaco codificado em Base64 (`DocumentoCursorCodec`), otimizada para o app mobile e ingestao por modelos de inteligencia artificial.
- **Entregaveis:** `DocumentoQueryRepositoryImpl`, Criteria API otimizada e decodificacao de cursores opacos.
- **Especificacao de Referencia:** [docs/SPEC-SPRINT-07-LISTAGEM-CURSOR.md](docs/SPEC-SPRINT-07-LISTAGEM-CURSOR.md).

#### Sprint 08: Download Streaming Seguro e Pre-visualizacao Inline (`EFFICIENTI-225`)
- **Escopo:** Endpoint `GET /api/v1/documentos/{id}/conteudo` permitindo download com transferencia binaria assincrona via `StreamingResponseBody` e cabecalhos dinamicos de `Content-Disposition` suportando o parametro `inline=true` para visualizacao direta em navegadores.
- **Entregaveis:** Endpoint de transmissao binaria direta do disco para o canal HTTP com liberacao imediata de threads.
- **Especificacao de Referencia:** [docs/SPEC-SPRINT-08-DOWNLOAD-STREAMING.md](docs/SPEC-SPRINT-08-DOWNLOAD-STREAMING.md).

#### Sprint 09: Ciclo de Vida de Documentos: CRUD, Exclusao e Auditoria (V3) (`EFFICIENTI-226`)
- **Escopo:** Atualizacao com bloqueio otimista via `@Version` (`PATCH /api/v1/documentos/{id}`), exclusao com remocao transacional do arquivo fisico (`DELETE /api/v1/documentos/{id}`) e trilha de auditoria completa com Flyway `V3__create_documento_auditoria.sql`.
- **Entregaveis:** Tabela `documento_auditoria`, deteccao de conflito de concorrencia (HTTP 409) e delecao segura.
- **Especificacao de Referencia:** [docs/SPEC-SPRINT-09-CRUD-AUDITORIA.md](docs/SPEC-SPRINT-09-CRUD-AUDITORIA.md).

#### Sprint 10: Validacoes Estritas, RFC 7807 e OpenAPI/Swagger UI (`EFFICIENTI-227`)
- **Escopo:** Padronizacao universal de erros da API segundo a RFC 7807 (`ProblemDetail`), inspecao de magic bytes para PDFs (`%PDF`) e PNGs (`\x89PNG\r\n\x1a\n`) e publicacao da documentacao interativa no Swagger UI (`/swagger-ui.html`).
- **Entregaveis:** `DocumentoExceptionHandler`, `ArquivoValidator` e documentacao de schemas OpenAPI 3.0.
- **Especificacao de Referencia:** [docs/SPEC-SPRINT-10-VALIDACOES-ERROS-OPENAPI.md](docs/SPEC-SPRINT-10-VALIDACOES-ERROS-OPENAPI.md).

#### Sprint 11: Seguranca Spring Security, Autenticacao JWT e CORS (`EFFICIENTI-228`)
- **Escopo:** Blindagem das rotas da API com Spring Security 6, autenticacao JWT Bearer stateless com algoritmo simetrico HS256, controle de acesso baseado em papeis (RBAC) e configuracao centralizada de CORS para aplicacoes web.
- **Entregaveis:** `JwtTokenService`, `SecurityConfig`, `JwtRoleConverter` e protecao contra acessos indevidos.
- **Especificacao de Referencia:** [docs/SPEC-SPRINT-11-SEGURANCA-JWT-CORS.md](docs/SPEC-SPRINT-11-SEGURANCA-JWT-CORS.md).

#### Sprint 12: Exportacao Assincrona de Lote e Enfileiramento (V4) (`EFFICIENTI-229`)
- **Escopo:** Infraestrutura de persistencia (`V4__create_exportacao.sql`) e endpoint `POST /api/v1/exportacoes` para requisicao de consolidacao assincrona de documentos em lote, respondendo de imediato com HTTP 202 Accepted.
- **Entregaveis:** Migration V4, tabelas `exportacao` e `exportacao_documento` e controle de status (`PENDENTE`, `EM_PROCESSAMENTO`, `CONCLUIDO`, `FALHA`).
- **Especificacao de Referencia:** [docs/SPEC-SPRINT-12-EXPORTACAO-ASSINCRONA.md](docs/SPEC-SPRINT-12-EXPORTACAO-ASSINCRONA.md).

#### Sprint 13: Worker ZIP em Background e Download Consolidado (`EFFICIENTI-230`)
- **Escopo:** Worker agendado (`ExportacaoWorker`) para compactacao de PDFs e PNGs em arquivos `.zip` via `ZipOutputStream`, job de expirao automatica (`ExportacaoExpirationWorker`) e rota de download `GET /api/v1/exportacoes/{id}/conteudo`.
- **Entregaveis:** Processamento assincrono em background, limpeza periodica de downloads expirados e retorno HTTP 410 Gone para itens expirados.
- **Especificacao de Referencia:** [docs/SPEC-SPRINT-13-WORKER-ZIP-DOWNLOAD.md](docs/SPEC-SPRINT-13-WORKER-ZIP-DOWNLOAD.md).

#### Sprint 14: Observabilidade, Logs Distribuidos e Metricas Prometheus (`EFFICIENTI-231`)
- **Escopo:** Rastreabilidade distribuida com filtro `CorrelationIdFilter` injetando `X-Correlation-Id` no MDC do SLF4J, metricas customizadas de tempo e quantidade no Micrometer (`EfficientiaMetricsService`) e exposicao de telemetria no Actuator (`/actuator/prometheus`).
- **Entregaveis:** Correlation IDs propagados nos logs, contadores de exportacao e tempo de upload registrados.
- **Especificacao de Referencia:** [docs/SPEC-SPRINT-14-OBSERVABILIDADE-METRICAS-IA.md](docs/SPEC-SPRINT-14-OBSERVABILIDADE-METRICAS-IA.md).

#### Sprint 15: Homologacao E2E Testcontainers, Release 1.0.0 e Deploy Render (`EFFICIENTI-232`)
- **Escopo:** Validacao ponta a ponta com instncias reais de PostgreSQL via Testcontainers (`PostgresIntegrationTest`), construcao de imagem conteinerizada Docker multi-stage com usuario nao-root e deploy automatizado no Render.
- **Entregaveis:** Dockerfile enxuto com JDK headless, suite de testes de integracao verde e publicacao do servico em nuvem.
- **Especificacao de Referencia:** [docs/SPEC-SPRINT-15-HOMOLOGACAO-PRODUCTION-RELEASE.md](docs/SPEC-SPRINT-15-HOMOLOGACAO-PRODUCTION-RELEASE.md).

#### Sprint 16: Fluxo de Autenticacao Operacional e Login Mobile (`EFFICIENTI-233`)
- **Escopo:** Endpoints operacionais de login e cadastro (`POST /api/v1/auth/login` e `POST /api/v1/auth/signup`), verificacao de hashes BCrypt, integracao com o codigo de empresa e retorno de dados cadastrais sanitizados sem exposicao de senhas.
- **Entregaveis:** Endpoints de autenticacao de motoristas e tratamento de credenciais com padronizacao RFC 7807.
- **Especificacao de Referencia:** [docs/SPEC-SPRINT-16-FLUXO-AUTENTICACAO-MOBILE.md](docs/SPEC-SPRINT-16-FLUXO-AUTENTICACAO-MOBILE.md).

#### Sprint 17: Cadastro de Empresas e Gerador de Codigo de 8 Digitos (V5) (`EFFICIENTI-234`)
- **Escopo:** Estruturacao multi-tenant SaaS com a migration `V5__create_empresa_and_configuracao.sql`, endpoint `POST /api/v1/empresas` e algoritmo `CodigoEmpresaGenerator` para geracao autonoma e unica de codigo com 3 letras e 5 numeros (exemplo: `FRI48291`).
- **Entregaveis:** Migration V5, busca publica por codigo (`GET /api/v1/empresas/codigo/{codigo}`) e sinalizacao de obrigatoriedade do primeiro acesso administrativo.
- **Especificacao de Referencia:** [docs/SPEC-SPRINT-17-CADASTRO-EMPRESAS-CODIGO-ACESSO.md](docs/SPEC-SPRINT-17-CADASTRO-EMPRESAS-CODIGO-ACESSO.md).

#### Sprint 18: Gestao Multi-tenant SaaS, 1º Acesso ADM e Adesao de Gestores (`EFFICIENTI-235`)
- **Escopo:** Esteira obrigatoria de primeiro acesso do administrador corporativo (`POST /api/v1/auth/adm/primeiro-acesso`) concedendo `ROLE_ADMIN`, login administrativo (`POST /api/v1/auth/adm/login`), adesao de novos gestores e pre-login de funcionarios vinculados a empresa (motoristas e analistas).
- **Entregaveis:** `EmpresaAdminController`, isolamento de visibilidade por tenant e controle de administradores.
- **Especificacao de Referencia:** [docs/SPEC-SPRINT-18-LOGIN-ADESAO-ADMINISTRADORES-EMPRESA.md](docs/SPEC-SPRINT-18-LOGIN-ADESAO-ADMINISTRADORES-EMPRESA.md).

#### Sprint 19: Onboarding Corporativo: Etapa 1 de 3 (Dados, Endereco e Logo) (V6) (`EFFICIENTI-236`)
- **Escopo:** Persistencia dos dados complementares da transportadora (razao social, nome fantasia, CNPJ, telefone, e-mail), vinculo de endereco com tabela normalizada, upload multipart de logotipo corporativo PNG/SVG ate 5 MB (`POST /api/v1/empresas/{id}/logo`), streaming publico de exibicao (`GET /api/v1/empresas/{id}/logo/conteudo`) e migration Flyway `V6__empresa_dados_complementares.sql`.
- **Entregaveis:** Migration V6, endpoints de atualizacao de etapa cadastral e armazenamento e entrega de logotipos com cabecalhos de cache.
- **Especificacao de Referencia:** [docs/SPEC-SPRINT-19-ONBOARDING-EMPRESA-DADOS-COMPLEMENTARES.md](docs/SPEC-SPRINT-19-ONBOARDING-EMPRESA-DADOS-COMPLEMENTARES.md).

#### Sprint 20: Gestao de Frota (Caminhoes) e Integracao Web/Mobile
- **Escopo:** Modelagem independente de caminhoes com separacao estrita entre Cavalos Mecanicos (`veiculo_cavalo`) e Carretas Boiadeiras (`veiculo_carreta`). Desvinculacao do caminhao em relacao ao motorista no cadastro e alocacao dinamica durante a vigencia do relatorio de viagem. Bloqueio automatico de veiculos com inspecao veicular vencida (`InspecaoVencidaException`).
- **Entregaveis:** Migration Flyway V7, `CaminhaoController`, endpoints dedicados para mobile (`GET /api/v1/caminhoes/app` e `/disponiveis`), vinculacao por placas (`POST /vincular`) e liberacao automatica de status (`EM_USO` para `DISPONIVEL`) no encerramento da viagem.
- **Especificacao de Referencia:** [docs/SPEC-SPRINT-20-GESTAO-FROTA-CAMINHOES-RELATORIO-MOBILE.md](docs/SPEC-SPRINT-20-GESTAO-FROTA-CAMINHOES-RELATORIO-MOBILE.md).

#### Sprint 21: Assinatura Fixa do Motorista como Imagem (PNG) e Versionamento Imutavel
- **Escopo:** Subsistema de assinatura fixa no perfil do motorista (`assinatura_motorista`), coletada no primeiro acesso ou atualizada administrativamente. Suporte a geracao grafica via desenho em tela ou nome digitado formatado. Validacao estrita de magic bytes de PNG, tamanho maximo de 1 MB e calculo de hash SHA-256 no banco relacional. Vinculacao imutavel com relatorios de viagem garantindo que alteracoes posteriores no perfil nao alterem relatorios historicos ja finalizados.
- **Entregaveis:** Migration Flyway V8, `AssinaturaMotoristaController`, modelo de versionamento append-only e endpoints `/api/v1/usuarios/me/assinatura` e `/conteudo`.
- **Especificacao de Referencia:** [docs/SPEC-SPRINT-21-ASSINATURA-FIXA-MOTORISTA-IMAGEM.md](docs/SPEC-SPRINT-21-ASSINATURA-FIXA-MOTORISTA-IMAGEM.md).

#### Sprint 22: Integracao do Diario de Rota Mobile e Ciclo de Vida em 6 Etapas
- **Escopo:** Digitalizacao completa do relatorio de transporte pecuario da fazenda ao frigorifico. Suporte ao ciclo de 6 etapas: configuracao inicial, carregamento e saida da propriedade, paradas imprevistas no trajeto, chegada e desembarque no frigorifico, avaliacao de animais (em pe, deitados, mortos, emergencia) e assinaturas digitais. Salvamento progressivo com status `rascunho`, retomada de dados parciais, derivacao automatica do motorista e empresa atraves do JWT e validacoes de coerencia temporal e hodometrica na submissao para `pendente`.
- **Entregaveis:** `RelatorioViagemController`, DTOs de integracao, tratamento de nomes livres ou IDs de catalogo e tratamento robusto de erros por campo (`fieldErrors`).
- **Especificacao de Referencia:** [docs/SPEC-SPRINT-22-DIARIO-ROTA-API.md](docs/SPEC-SPRINT-22-DIARIO-ROTA-API.md).

#### Sprint 23: Assinaturas de Participantes do Diario de Rota (Upload Binario e Streaming PNG)
- **Escopo:** Upload binario multipart e streaming inline das 3 assinaturas contextuais coletadas em campo: Pecuarista (produtor na fazenda), Manobrista (patio do frigorifico) e Curraleiro (curral de recepcao). Validacao estrita de completude das 4 assinaturas na transicao para `pendente` ou `finalizar` (retornando HTTP 422 com lista de `papeisFaltantes` caso alguma esteja ausente).
- **Entregaveis:** Endpoints `PUT /api/v1/relatorios-viagem/{id}/assinaturas/{papel}` e `GET .../conteudo`, migration Flyway V13 e integracao com o storage privado.
- **Especificacao de Referencia:** [docs/SPEC-SPRINT-23-ASSINATURAS-PARTICIPANTES-RELATORIO.md](docs/SPEC-SPRINT-23-ASSINATURAS-PARTICIPANTES-RELATORIO.md).

Para visualizar o mapeamento formal de todas as historias e tarefas no Jira, consulte [docs/JIRA_ESTRUTURA_DS2_SPRINTS.md](docs/JIRA_ESTRUTURA_DS2_SPRINTS.md).

---

## 4. Modulos Funcionais e Dominios da API

### 4.1 Autenticacao, Identidade e Controle de Acesso (RBAC)

O subsistema de seguranca da API opera de forma stateless baseada em tokens JWT simetricos (`HS256`).

- **Chave Criptografica:** Segredo HMAC de no minimo 32 bytes configuravel via variavel de ambiente `JWT_SECRET`.
- **Claims Padronizadas:** O token emitido contem identificador de usuario (`usuario_id`), CPF, codigo interno da empresa (`codigo_interno`), papel operacional (`roles`), emissor (`iss=efficientia-api`), audiencia (`aud=efficientia-api`) e tempo de expiracao de 24 horas.
- **Roles Operacionais Suportadas (7 Perfis):**
  1. `MOTORISTA`: Operador de campo responsavel pela conducao do transporte, preenchimento do diario e assinatura fixa.
  2. `MANOBRISTA`: Operador de patio no frigorifico responsavel por manobra de conjuntos e assinatura de patio.
  3. `ANALISTA`: Profissional de gestao operacional que audita, aprova ou reprova relatorios de viagem.
  4. `PECUARISTA`: Produtor rural que assina a liberacao da boiada na fazenda.
  5. `CURRALEIRO`: Operador do curral que inspeciona o desembarque e assina a recepcao dos animais.
  6. `FUNCIONARIO_FRIBOI`: Perfil corporativo com privilegios estendidos de auditoria.
  7. `ADMIN`: Administrador corporativo com permissao total de gerenciamento de frota, funcionarios e configuracoes.
- **Protecao contra Enumeracao:** No endpoint de login corporativo e operacional (`POST /api/v1/auth/login`), tentativas com senhas incorretas ou identificadores inexistentes geram logs detalhados internamente, mas respondem externamente com a mensagem generica `"Credenciais invalidas."` (`HTTP 401 Unauthorized`), impedindo a descoberta de usuarios cadastrados por forca bruta.
- **Documento de Referencia:** [docs/FIX-ADEQUACAO-SEGURANCA-INTEGRACAO-MOBILE.md](docs/FIX-ADEQUACAO-SEGURANCA-INTEGRACAO-MOBILE.md).

### 4.2 Gestao Multi-tenant e Onboarding Corporativo

A API suporta arquitetura multi-inquilino (multi-tenant) onde cada transportadora opera de forma isolada com sua frota e funcionarios.

- **Codigo Unico Empresarial:** Gerado de forma autonoma pelo componente `CodigoEmpresaGenerator` durante o cadastro da empresa (`POST /api/v1/empresas`), seguindo o padrao de 3 letras maiusculas e 5 digitos numericos (ex: `FRI48291`). Esse codigo e utilizado pelos funcionarios no login operacional mobile.
- **Esteira Obrigatoria de Primeiro Acesso:** A empresa recem-criada recebe a flag `requerPrimeiroAdmin: true`. O primeiro gestor a acessar registra nome, CPF, e-mail e senha atraves do endpoint `POST /api/v1/auth/adm/primeiro-acesso`, tornando-se o administrador mestre do tenant com emissao imediata de token `ROLE_ADMIN`.
- **Dados Complementares e Logotipo:** Na etapa 1 de onboarding, a empresa atualiza razao social, nome fantasia, telefone corporativo e endereco. E possivel realizar upload multipart do logotipo da empresa (`POST /api/v1/empresas/{id}/logo`), aceitando formatos PNG ou SVG de ate 5 MB, com entrega publica via streaming (`GET /api/v1/empresas/{id}/logo/conteudo`) para renderizacao em paineis web.

### 4.3 Gestao de Frota e Portao Sanitario Veicular

O dominio de frota (`sc_frota` / `public.veiculo_cavalo` e `veiculo_carreta`) modela as especificidades do transporte de carga viva:

- **Desacoplamento e Alocacao Dinamica:** Cavalos e carretas pertencem a empresa e nao possuem motoristas fixos no cadastro. A associacao ocorre no inicio do relatorio de viagem e o caminhao permanece em estado `EM_USO`. Ao finalizar a viagem, o caminhao e liberado e retorna ao estado `DISPONIVEL`.
- **Validacao de Inspecao Sanitaria e Mecanica:** Antes de vincular um veiculo a um relatorio, o servico `CaminhaoService` verifica o campo `data_vencimento_inspecao`. Caso a data esteja vencida, a requisicao e bloqueada com HTTP 422 (`InspecaoVencidaException`), garantindo conformidade com normas zoossanitarias.
- **Endpoints de Suporte ao Mobile:**
  - `GET /api/v1/caminhoes/app`: Lista completa de caminhoes da empresa pronta para exibicao no app, trazendo indicadores visuais de validade de inspecao, dias restantes para o vencimento e status de ocupacao.
  - `POST /api/v1/caminhoes/relatorio/{id}/vincular`: Permite ao motorista vincular o veiculo simplesmente informando a placa do cavalo e a placa da carreta.

### 4.4 Diario de Rota Operacional e Ciclo de Vida em 6 Etapas

O Diario de Rota (`RelatorioViagem`) registra integralmente a jornada do transporte pecuario:

1. **Etapa 1 - Configuracao e Identificacao:**
   - Vinculacao de veiculo (cavalo e carreta).
   - Local de origem (Fazenda) e local de destino (Unidade Frigorifica). O sistema aceita a selecao por identificadores numericos de catalogo (`fazendaId`, `unidadeFrigorificaId`) ou nomes digitados livremente (`nomeOrigem`, `nomeDestino`), sem forcar criacao automatica de cadastros mestres.
   - Numeros de documento: GTA (obrigatoriamente unico no sistema) e Nota Fiscal.
2. **Etapa 2 - Carga e Partida da Propriedade:**
   - Data e horario de inicio do embarque dos animais.
   - Data e horario de saida da fazenda.
   - Odometro de saida do embarcadouro (`kmSaidaEmbarcadouro`).
3. **Etapa 3 - Trajeto e Paradas Imprevistas:**
   - Registro de paradas decorrentes de pneus furados, problemas mecanicos, fiscalizacao de transito, condicoes climaticas adversas ou bem-estar animal.
   - Horarios em formato ISO local (`dataHoraInicio`, `dataHoraFim`) e descricao detalhada quando o motivo for classificado como `outro`.
4. **Etapa 4 - Chegada e Desembarque no Frigorifico:**
   - Data e horario de chegada a unidade industrial.
   - Horario de desembarque e numero do curral de recepcao.
   - Odometro de chegada (`kmChegadaDesembarcadouro`).
   - Verificacao de seguranca operacional: confirmacao se a sirene de marcha a re funcionou durante o posicionamento no desembarcadouro.
5. **Etapa 5 - Conferencia de Animais e Bem-Estar:**
   - Contagem por categoria de rebanho: quantidade de machos, femeas e marrucos (total embarcado).
   - Contagem por condicao fisica na descarga: quantidade em pe, deitados, mortos e em atendimento emergencial.
   - Registro obrigatorio de motivo quando houver animais em estado de emergencia.
   - Listas detalhadas de anomalias ocorridas durante o embarque ou desembarque.
6. **Etapa 6 - Coleta de Assinaturas e Finalizacao:**
   - Conferencia das 4 assinaturas obrigatorias: Pecuarista, Motorista, Manobrista e Curraleiro.
   - Envio final garantido por `Idempotency-Key`.

#### Salvamento Progressivo e Validacao de Rascunho
A API implementa logica diferenciada entre operacoes em rascunho e submissao definitiva:
- **Status `rascunho`:** Aceita dados parciais em qualquer uma das etapas, validando apenas a coerencia interna dos dados fornecidos sem exigir o preenchimento de campos futuros.
- **Transicao para `pendente`:** Realizada atraves de copia isolada do estado persistido mais os dados enviados. A API aplica validacao estrita: datas e horarios cronologicos (embarque <= saida <= chegada <= desembarque), hodometro nao regressivo (km chegada >= km saida), soma de animais coerente e presenca comprovada das 4 assinaturas.
- **Documento de Referencia:** [docs/FIX-DIARIO-ROTA-HTTP-400.md](docs/FIX-DIARIO-ROTA-HTTP-400.md).

### 4.5 Subsistema de Assinaturas Digitais e Biometria Grafica

O tratamento de assinaturas digitais na plataforma Efficientia assegura valor juridico e rastreabilidade:

- **Assinatura Fixa do Motorista (`assinatura_motorista`):**
  - Armazenada em coluna relacional `BYTEA` no PostgreSQL com metadados de tamanho em bytes, tipo MIME estrito (`image/png`), timestamp de criacao e hash SHA-256 (`CHAR(64)`).
  - Validacao binaria dos primeiros 8 bytes (*magic bytes* `89 50 4E 47 0D 0A 1A 0A`). Arquivos com extensoes renomeadas sao rejeitados com HTTP 415.
  - Modelo append-only imutavel: ao atualizar a assinatura no perfil, a versao anterior e desativada (`ativa = false`) e a nova versao e inserida com versao incremental, mantendo integridade com relatorios passados.
- **Assinaturas de Participantes da Rota:**
  - O aplicativo realiza o upload multipart das assinaturas em desenho do pecuarista, manobrista e curraleiro atraves de `PUT /api/v1/relatorios-viagem/{id}/assinaturas/{papel}`.
  - A API grava o arquivo no storage gerenciado e vincula a URL relativa de streaming no relatorio.
  - A assinatura do motorista e copiada automaticamente da assinatura ativa do perfil no momento do lancamento, impedindo que o aplicativo force o envio de URLs de terceiros.
- **Streaming PNG Inline:**
  - Transmissao direta dos bytes PNG via endpoints `/conteudo` com cabecalho `Content-Type: image/png`, `Content-Disposition: inline` e politicas de cache privadas.

### 4.6 Armazenamento Privado e Gestao de Documentos Operacionais

Para documentos anexos (GTAs digitalizadas, notas fiscais e tickets de pesagem de balanca):

- **Storage Privado Local:** Os arquivos sao armazenados em diretorio privado isolado (`DOCUMENT_STORAGE_PATH`, padrao `./data/documentos`), completamente inacessivel via requisicoes HTTP diretas.
- **Seguranca de Acesso e Path Traversal:** Validacao rigorosa contra tentativas de manipulacao de caminho de arquivo (caracteres `..`, barras invertidas ou caminhos absolutos fora do root do storage).
- **Paginacao por Cursor Opaco:** O endpoint `GET /api/v1/documentos` oferece navegacao por cursor opaco Base64 (`DocumentoCursorCodec`), ideal para navegacao infinita no mobile e ingestao por pipelines de machine learning sem a sobrecarga de queries com `OFFSET` elevado no PostgreSQL.
- **Auditoria Transacional:** Todas as operacoes de criacao, alteracao e exclusao gravam registros imutaveis na tabela `documento_auditoria`.

### 4.7 Exportacao Assincrona e Worker ZIP

Para atender as demandas de auditoria fiscal e contabilidade das transportadoras:

- **Enfileiramento com Resposta Imediata:** A solicitacao de consolidacao de documentos (`POST /api/v1/exportacoes`) valida a existencia dos IDs informados, grava a solicitacao com estado `PENDENTE` e responde imediatamente com HTTP 202 Accepted contendo o cabecalho `Location` para acompanhamento do progresso.
- **Processamento em Background (`ExportacaoWorker`):** Processo agendado que recupera os arquivos fisicos correspondentes no storage, compacta o lote utilizando `ZipOutputStream` diretamente para um arquivo `.zip` temporario e atualiza o estado para `CONCLUIDO`.
- **Politica de Retencao e Limpeza (`ExportacaoExpirationWorker`):** Rotina periodica que identifica lotes cuja janela de retencao expirou, removendo o arquivo `.zip` do disco e marcando o status como expirado (retornando HTTP 410 Gone em solicitacoes de download posteriores).

### 4.8 Arquitetura Dirigida a Eventos: Transactional Outbox Pattern e Redis

Para viabilizar telemetria em tempo real, paineis de monitoramento de frota e classificacao de motoristas sem comprometer a transacao relacional ACID:

- **Transactional Outbox Pattern:** Integracao concebida na migracao `V16__outbox_evento_e_ajustes_procedures.sql`. Ao criar ou atualizar o status de um relatorio de viagem na tabela `sc_operacao.tb_relatorio_viagem`, um gatilho de banco (`tg_outbox_relatorio_viagem`) grava atomicamente o evento correspondente na tabela `sc_operacao.tb_outbox_evento`.
- **Garantia de Empresa Vinculada (V20):** A migracao `V20__outbox_apenas_relatorios_com_empresa.sql` assegura que eventos sejam emitidos apenas quando o relatorio possui uma empresa real associada, prevenindo violacoes de chave estrangeira em rascunhos operacionais de motoristas legados.
- **Relay de Despacho (`OutboxRelayService`):** Servico responsavel por consultar os eventos pendentes na outbox via indice parcial de alto desempenho (`idx_outbox_pendentes`), despacha-los para topicos do Redis Streams (`efficientia:events:viagens`) e marcar o registro como processado com timestamp e controle de tentativas de erro.
- **Gamificacao e Leaderboard (`RedisLeaderboardService`):** Consome eventos de viagens concluidas e atualiza estruturas de dados Redis Sorted Sets (`ZADD`, `ZINCRBY`), mantendo rankings em tempo real de pontuacao e conformidade de motoristas.

### 4.9 Observabilidade, Metricas e Diagnosticos

A API possui instrumentacao completa para operacao em ambientes de producao:

- **Rastreabilidade Distribuida:** O filtro `CorrelationIdFilter` extrai o cabecalho `X-Correlation-Id` recebido ou gera um novo UUID, injetando-o no contexto do Mapped Diagnostic Context (MDC) do SLF4J, garantindo que todas as linhas de log de uma mesma requisicao compartilhem o mesmo identificador.
- **Metricas com Micrometer e Prometheus:** O servico `EfficientiaMetricsService` publica cronometros e contadores customizados para medicoes de latencia de upload, tempo de empacotamento de exportacoes e contagem de relatorios finalizados no endpoint `/actuator/prometheus`.
- **Health Indicators de Infraestrutura:** O componente `StorageHealthIndicator` expoe a integridade do armazenamento de disco no `/actuator/health` e implementa criacao automatica recursiva de diretorios no primeiro ciclo de checagem.

---

## 5. Evolucao da Modelagem Relacional e Migracoes Flyway (V1 a V20)

O banco de dados relacional evoluiu ao longo de 20 migracoes versionadas do Flyway, fazendo a ponte entre os esquemas cannicos multi-tenant do dominio (`sc_corporativo`, `sc_frota`, `sc_operacao`, `sc_auditoria`) e as entidades de persistencia mapeadas pela aplicacao.

### 5.1 Esquemas de Banco de Dados

```
├── public
│   ├── flyway_schema_history
│   ├── usuario / empresa / configuracao_operacao
│   ├── veiculo_cavalo / veiculo_carreta
│   ├── relatorio_viagem / parada_imprevista / anomalia_embarque / anomalia_desembarque
│   └── documento / documento_auditoria / exportacao / assinatura_motorista
├── sc_corporativo
│   ├── tb_empresa
│   ├── tb_empresa_admin
│   ├── tb_usuario
│   ├── tb_endereco
│   ├── tb_fazenda
│   └── tb_unidade_frigorifica
├── sc_frota
│   ├── tb_veiculo_base
│   ├── tb_veiculo_cavalo
│   ├── tb_veiculo_carreta
│   └── tb_manutencao_inspecao
├── sc_operacao
│   ├── tb_relatorio_viagem
│   ├── tb_parada_imprevista
│   ├── tb_anomalia_embarque
│   ├── tb_anomalia_desembarque
│   └── tb_outbox_evento
└── sc_auditoria
    ├── tb_auditoria_analise
    └── tb_monitoramento_dau
```

Views de compatibilidade no schema `public` mantem retrocompatibilidade transparente entre identificadores legados (tipo `INTEGER`) e identificadores canicos (tipo `UUID`). Para o diagrama visual e especificacao de colunas, consulte [docs/DATABASE_DIAGRAM.md](docs/DATABASE_DIAGRAM.md).

### 5.2 Registro Completo das Migracoes Flyway

| Versao | Arquivo de Migracao | Finalidade Tecnica e Mudancas Estruturais |
| :--- | :--- | :--- |
| **V1** | `V1__create_relational_schema.sql` | Criacao da modelagem relacional inicial normalizada: tabelas `usuario`, `endereco`, `fazenda`, `unidade_frigorifica`, `veiculo_cavalo`, `veiculo_carreta` e `relatorio_viagem`. |
| **V2** | `V2__create_documento.sql` | Criacao da tabela `documento` com chave primeria UUID, metadados de viagens, tipo de documento e restricao unica em `idempotency_key`. |
| **V3** | `V3__create_documento_auditoria.sql` | Criacao da tabela `documento_auditoria` com registro de operacoes de insercao, atualizacao e remocao de arquivos. |
| **V4** | `V4__create_exportacao.sql` | Criacao das tabelas `exportacao` e `exportacao_documento` para controle de lotes de download assincrono. |
| **V5** | `V5__create_empresa_and_configuracao.sql` | Criacao da tabela corporativa `empresa` com codigo unico de 8 digitos, CNPJ, status multi-tenant e `configuracao_operacao`. |
| **V6** | `V6__empresa_dados_complementares.sql` | Inclusao de colunas de dados corporativos complementares em `empresa`: `telefone`, `nome_fantasia`, `logo_url`, `etapa_cadastro` e `cadastro_completo`. |
| **V7** | `V7__frota_caminhoes_e_vinculos.sql` | Reestruturacao idempotente das tabelas `veiculo_cavalo` e `veiculo_carreta` adicionando colunas de inspecao veicular, odometro acumulado e vinculos com relatorios. |
| **V8** | `V8__create_assinatura_motorista.sql` | Criacao da tabela `assinatura_motorista` com armazenamento `BYTEA`, validacao de magic bytes PNG, hash SHA-256 e indice unico parcial para versao ativa. |
| **V9** | `V9__adapta_banco_e_processo_assinaturas.sql` | Adaptacao do banco para criacao segura de views de compatibilidade sem conflito com tabelas fisicas preexistentes. |
| **V10** | `V10__alinha_tipos_de_ocorrencia_com_jpa.sql` | Alinhamento dos campos de motivo de parada e tipo de anomalia com os tipos `String` do JPA atraves de colunas `VARCHAR(50)` com constraints `CHECK`. |
| **V11** | `V11__adequacao_schemas_pdf.sql` | Adequacao e provisionamento dos schemas segregados (`sc_corporativo`, `sc_frota`, `sc_operacao`, `sc_auditoria`) preservando views de compatibilidade. |
| **V12** | `V12__restaura_tabelas_relacionais_compativeis.sql` | Restauracao de compatibilidade estrutural entre tabelas relacionais primarias e entidades JPA da aplicacao. |
| **V13** | `V13__assinaturas_participantes_e_ajustes_schemas.sql` | Extensao da constraint de papeis de assinatura para aceitar `PECUARISTA` e correcao de chaves estrangeiras com `sc_corporativo.tb_usuario`. |
| **V14** | `V14__foto_perfil_motorista.sql` | Adicao do suporte a foto de perfil do motorista em disco gerenciado e colunas de metadados em `sc_corporativo.tb_usuario`. |
| **V15** | `V15__rascunhos_diario_rota_parciais.sql` | Flexibilizacao de 28 colunas do diario de rota para permitir valores nulos em rascunho, adicao dos campos de nomes livres (`nomeOrigem`, `nomeDestino`) e motivo detalhado de paradas. |
| **V16** | `V16__outbox_evento_e_ajustes_procedures.sql` | Implementacao da tabela de mensageria `sc_operacao.tb_outbox_evento`, triggers de geracao de eventos e procedimentos de validacao veicular. |
| **V17** | `V17__alinha_unidade_frigorifica_inteira.sql` | Alinhamento da tabela de unidades frigorificas com identificador inteiro preservando tabelas fisicas legadas sem perda de dados. |
| **V18** | `V18__sincronizacao_efficientia_database.sql` | Criacao de indices parciais para buscas paginadas de relatorios de viagem e criacao da tabela `sc_auditoria.tb_monitoramento_dau`. |
| **V19** | `V19__converter_monitoramento_dau_uuid.sql` | Conversao da coluna `usuario_id` na tabela `tb_monitoramento_dau` para o tipo UUID com garantia de indice unico para a clausula `ON CONFLICT`. |
| **V20** | `V20__outbox_apenas_relatorios_com_empresa.sql` | Ajuste da trigger `tg_outbox_relatorio_viagem` para despachar eventos apenas quando o relatorio possui uma empresa valida, prevenindo erros em rascunhos legados. |

---

## 6. Incidentes Tecnicos, Correcoes Criticas e Acoes de Engenharia

Ao longo do desenvolvimento e da esteira de integracao com a nuvem do Render, diversos impedimentos tecnicos foram investigados, diagnosticados e solucionados de forma definitiva.

### 6.1 Bloqueios de Deploy no Render (StorageHealthIndicator)
- **Cenario do Incidente:** Durante o deploy efemero no Render, o container tentava verificar a saude do disco antes que o diretorio `./data/documentos` tivesse sido criado pela aplicacao. O Spring Boot Actuator respondia com status `DOWN`, fazendo com que a esteira do Render encerrasse a execucao por falha de inicializacao.
- **Resolucao:** O componente `StorageHealthIndicator` foi alterado para executar a criacao recursiva do diretorio (`Files.createDirectories()`) no primeiro teste de integridade.
- **Documento de Referencia:** [docs/FIX-TESTES-COBERTURA-JACOCO-RENDER.md](docs/FIX-TESTES-COBERTURA-JACOCO-RENDER.md).

### 6.2 Validacao de Audience no JWT e Unificacao Criptografica HS256
- **Cenario do Incidente:** Ao emitir tokens de acesso, a falta da claim de audiencia (`aud`) fazia com que o validador interno do Spring Security (`AudienceValidator`) rejeitasse as requisicoes com HTTP 401 Unauthorized. Alem disso, havia divergencia entre o servico de emissao (HMAC) e a decodificacao (JWKS).
- **Resolucao:** Configuracao unificada do `SecurityConfig` utilizando `NimbusJwtDecoder.withSecretKey` com chave HMAC estavel, inclusao da claim `aud=efficientia-api` na geracao de tokens e ampliacao do `JwtRoleConverter` para cobrir todos os 7 perfis do enum `TipoUsuario`.
- **Documento de Referencia:** [docs/FIX-ADEQUACAO-SEGURANCA-INTEGRACAO-MOBILE.md](docs/FIX-ADEQUACAO-SEGURANCA-INTEGRACAO-MOBILE.md).

### 6.3 Falha de Validacao de Rascunhos Parciais no Diario de Rota (HTTP 400)
- **Cenario do Incidente:** Requisicoes de atualizacao por `PUT` preservavam campos omitidos no banco, mas a camada de validacao verificava a completude apenas nos dados da requisicao HTTP recebida. Assim, a atualizacao parcial de uma etapa de um diario resultava em erro 400 indevido, exigindo o reenvio de todas as etapas anteriores.
- **Resolucao:** A arquitetura do `RelatorioViagemService` foi refatorada para gerar uma copia isolada da entidade persistida, fundir os novos campos recebidos e submeter o **estado resultante consolidado** a validacao antes de efetivar qualquer alteracao no banco de dados. Os contratos de salvamento foram unificados aceitando rascunhos sem chave e exigindo `Idempotency-Key` obrigatoriamente na transicao para `pendente`.
- **Documento de Referencia:** [docs/FIX-DIARIO-ROTA-HTTP-400.md](docs/FIX-DIARIO-ROTA-HTTP-400.md).

### 6.4 Incompatibilidade de Tipos entre PostgreSQL e Mapeamento JPA
- **Cenario do Incidente:** A coluna `assinatura_motorista.sha256` foi criada no banco como `CHAR(64)`, enquanto a entidade Java utilizava `VARCHAR(64)`. A validacao automatica do Hibernate (`ddl-auto=validate`) bloqueava a subida da aplicacao. Em paralelo, a utilizacao de tipos enum nativos do PostgreSQL causava conflitos com colunas manipuladas como strings puras no JPA.
- **Resolucao:** Mapeamento explicito com `@Column(columnDefinition = "bpchar")` / `CHAR(64)` no Hibernate e aplicacao da migracao Flyway V10 convertendo enums para `VARCHAR(50)` com constraints de checagem (`CHECK`), mantendo compatibilidade bilateral.

### 6.5 Atingimento dos Limites Estritos de Qualidade no JaCoCo
- **Cenario do Incidente:** A esteira de CI (`maven-verify.yml`) impunha limites minimos obrigatorios de 80% de cobertura de linhas e 60% de cobertura de ramificacoes (branches). Inicialmente, o projeto apresentava 73% de linhas e 53% de branches, impedindo merges na branch `main`.
- **Resolucao:** Foram desenvolvidos testes unitarios e integrados massivos, cobrindo cenarios criticos de excecao, fluxos de branch de seguranca, repositorios em memoria e servicos administrativos (`EmpresaAdminServiceTest`, `CadastroBaseServiceTest`, `JwtTokenServiceTest`), elevando a cobertura final para **93,1% de linhas** e **80,9% de ramificacoes**.

---

## 7. Homologacao e Contas Operacionais de Teste

Para permitir os testes de ponta a ponta das equipes de desenvolvimento Mobile e Web contra o ambiente oficial de producao hospedado no Render (`https://efficientia-api.onrender.com`), foram criadas e validadas contas de homologacao dedicadas.

### 7.1 Procedimentos de Validacao em Producao
O ambiente em nuvem executa sob o perfil de producao com PostgreSQL gerenciado e HTTPS ativo. A verificacao operacional pode ser realizada a qualquer momento atraves de:
- **Verificacao de Status da API:** `GET https://efficientia-api.onrender.com/api/v1/status` (devolve status `UP` e versao da release).
- **Verificacao de Integridade Geral:** `GET https://efficientia-api.onrender.com/actuator/health` (devolve status de conexao com PostgreSQL e storage privado).
- **Interface OpenAPI Interativa:** `https://efficientia-api.onrender.com/swagger-ui.html`.

### 7.2 Conta de Homologacao de Administrador Mobile
Criada para possibilitar o login no aplicativo Mobile sob o perfil administrativo:
- **Metodo de Autenticacao:** Login via CNPJ corporativo e senha cadastrada.
- **Endpoint Utilizado:** `POST https://efficientia-api.onrender.com/api/v1/auth/adm/login`.
- **Permissoes:** Emissao de token JWT com autoridade `ROLE_ADMIN`, liberando acesso de gestao de frota, auditoria de relatorios e cadastro de novos motoristas.

### 7.3 Conta de Homologacao de Motorista Operacional
Criada para os testes do fluxo de 6 etapas no aplicativo mobile:
- **Metodo de Autenticacao:** Login via CPF/e-mail, senha e codigo de empresa de 8 digitos.
- **Endpoint Utilizado:** `POST https://efficientia-api.onrender.com/api/v1/auth/login`.
- **Permissoes:** Emissao de token JWT com perfil `ROLE_MOTORISTA`, permitindo salvar rascunhos em `/api/v1/relatorios-viagem`, consultar historico em `/api/v1/relatorios-viagem/me`, alocar caminhoes disponiveis e cadastrar a assinatura fixa de primeiro acesso.

---

## 8. Indice Geral de Documentacoes e Especificacoes do Repositorio

A tabela a seguir consolida todas as especificacoes tecnicas, manuais de integracao e relatorios de incidentes mantidos no repositorio para consulta aprofundada:
| Caminho do Arquivo | Titulo e Objetivo do Documento |
| :--- | :--- |
| [README.md](README.md) | Documentacao principal da API: guia de execucao local, variaveis de ambiente, catalogo de endpoints e instrucoes de Docker. |
| [HELP.md](HELP.md) | Guia padrao de inicializacao do ecossistema Spring Boot e referencias oficiais do Maven. |
| [DOCUMENTACAO.md](DOCUMENTACAO.md) | Este documento: sintese consolidada formal de todo o projeto, decisoes arquiteturais, sprints e acoes de engenharia. |
| [docs/API_MAP.md](docs/API_MAP.md) | Mapeamento completo de contratos REST, rotas publicas e protegidas, DTOs de requisicao e exemplos de integracao para Mobile e Web. |
| [docs/DATABASE_DIAGRAM.md](docs/DATABASE_DIAGRAM.md) | Diagrama de Entidade-Relacionamento (Mermaid e dbdiagram.io), especificacao de schemas, chaves estrangeiras e dicionario de colunas. |
| [docs/JIRA_ESTRUTURA_DS2_SPRINTS.md](docs/JIRA_ESTRUTURA_DS2_SPRINTS.md) | Estrutura oficial do backlog do Jira (`EFFICIENTI-94`), tarefas, subtarefas e tabela formatada para importacao via CSV. |
| [docs/INTEGRACAO_DIARIO_MOBILE_2026-10-13.md](docs/INTEGRACAO_DIARIO_MOBILE_2026-10-13.md) | Guia de integracao das rotas de diario de rota, consulta `/me`, catalogo de locais e persistencia progressiva para o app mobile. |
| [docs/FIX-ADEQUACAO-SEGURANCA-INTEGRACAO-MOBILE.md](docs/FIX-ADEQUACAO-SEGURANCA-INTEGRACAO-MOBILE.md) | Relatorio tecnico de blindagem da seguranca, ativacao em producao, correcao de JWT HS256 e prevencao de enumeracao de usuarios. |
| [docs/FIX-DIARIO-ROTA-HTTP-400.md](docs/FIX-DIARIO-ROTA-HTTP-400.md) | Diagnostico e solucao do erro HTTP 400 nas etapas do diario, copia isolada de estado e validacao estrita na submissao final. |
| [docs/FIX-TESTES-COBERTURA-JACOCO-RENDER.md](docs/FIX-TESTES-COBERTURA-JACOCO-RENDER.md) | Resolucao dos bloqueios de inicializacao no Render, adequacao do Actuator e massiva ampliacao de testes para JaCoCo (93,1% / 80,9%). |
| [docs/SPEC_INITIAL-STRUCTURE.md](docs/SPEC_INITIAL-STRUCTURE.md) | Especificacao tecnica da Sprint 01: fundacao da aplicacao Spring Boot e esteira GitHub Actions. |
| [docs/SPEC-SPRINT-02-POSTGRESQL-FLYWAY.md](docs/SPEC-SPRINT-02-POSTGRESQL-FLYWAY.md) | Especificacao tecnica da Sprint 02: modelagem relacional 3FN e migracao Flyway V1. |
| [docs/SPEC-SPRINT-03-DOMINIO-DOCUMENTOS.md](docs/SPEC-SPRINT-03-DOMINIO-DOCUMENTOS.md) | Especificacao tecnica da Sprint 03: modelo de dominio e persistencia de documentos com UUID publico (Flyway V2). |
| [docs/SPEC-SPRINT-04-STORAGE-PRIVADO.md](docs/SPEC-SPRINT-04-STORAGE-PRIVADO.md) | Especificacao tecnica da Sprint 04: storage privado de arquivos em disco, hash SHA-256 e protecao contra Path Traversal. |
| [docs/SPEC-SPRINT-05-UPLOAD-MULTIPART.md](docs/SPEC-SPRINT-05-UPLOAD-MULTIPART.md) | Especificacao tecnica da Sprint 05: endpoint multipart para envio de PDFs/PNGs com garantia de idempotencia. |
| [docs/SPEC-SPRINT-06-ASSINATURAS.md](docs/SPEC-SPRINT-06-ASSINATURAS.md) | Especificacao tecnica da Sprint 06: assinaturas multimodais (foto, desenho e texto) e validacao de exclusividade. |
| [docs/SPEC-SPRINT-07-LISTAGEM-CURSOR.md](docs/SPEC-SPRINT-07-LISTAGEM-CURSOR.md) | Especificacao tecnica da Sprint 07: paginacao estavel por cursor opaco Base64 e filtros dinamicos de consulta. |
| [docs/SPEC-SPRINT-08-DOWNLOAD-STREAMING.md](docs/SPEC-SPRINT-08-DOWNLOAD-STREAMING.md) | Especificacao tecnica da Sprint 08: streaming binario assincrono e suporte ao modo inline para navegadores. |
| [docs/SPEC-SPRINT-09-CRUD-AUDITORIA.md](docs/SPEC-SPRINT-09-CRUD-AUDITORIA.md) | Especificacao tecnica da Sprint 09: atualizacao otimista, delecao segura e tabela de auditoria (Flyway V3). |
| [docs/SPEC-SPRINT-10-VALIDACOES-ERROS-OPENAPI.md](docs/SPEC-SPRINT-10-VALIDACOES-ERROS-OPENAPI.md) | Especificacao tecnica da Sprint 10: padronizacao RFC 7807, validacao de magic bytes e publicacao do Swagger UI. |
| [docs/SPEC-SPRINT-11-SEGURANCA-JWT-CORS.md](docs/SPEC-SPRINT-11-SEGURANCA-JWT-CORS.md) | Especificacao tecnica da Sprint 11: filtros Spring Security 6, controle RBAC stateless e politicas de CORS. |
| [docs/SPEC-SPRINT-12-EXPORTACAO-ASSINCRONA.md](docs/SPEC-SPRINT-12-EXPORTACAO-ASSINCRONA.md) | Especificacao tecnica da Sprint 12: solicitacao assincrona de exportacao de lotes documentais (Flyway V4). |
| [docs/SPEC-SPRINT-13-WORKER-ZIP-DOWNLOAD.md](docs/SPEC-SPRINT-13-WORKER-ZIP-DOWNLOAD.md) | Especificacao tecnica da Sprint 13: compactacao em background com ZipOutputStream e limpeza periodica de downloads. |
| [docs/SPEC-SPRINT-14-OBSERVABILIDADE-METRICAS-IA.md](docs/SPEC-SPRINT-14-OBSERVABILIDADE-METRICAS-IA.md) | Especificacao tecnica da Sprint 14: telemetria com Micrometer/Prometheus e correlacao de logs com MDC. |
| [docs/SPEC-SPRINT-15-HOMOLOGACAO-PRODUCTION-RELEASE.md](docs/SPEC-SPRINT-15-HOMOLOGACAO-PRODUCTION-RELEASE.md) | Especificacao tecnica da Sprint 15: homologacao com Testcontainers PostgreSQL, Dockerfile e deploy em nuvem. |
| [docs/SPEC-SPRINT-16-FLUXO-AUTENTICACAO-MOBILE.md](docs/SPEC-SPRINT-16-FLUXO-AUTENTICACAO-MOBILE.md) | Especificacao tecnica da Sprint 16: endpoints de login e signup operacional para o aplicativo mobile. |
| [docs/SPEC-SPRINT-17-CADASTRO-EMPRESAS-CODIGO-ACESSO.md](docs/SPEC-SPRINT-17-CADASTRO-EMPRESAS-CODIGO-ACESSO.md) | Especificacao tecnica da Sprint 17: estrutura multi-tenant e gerador autonomo de codigo empresarial (Flyway V5). |
| [docs/SPEC-SPRINT-18-LOGIN-ADESAO-ADMINISTRADORES-EMPRESA.md](docs/SPEC-SPRINT-18-LOGIN-ADESAO-ADMINISTRADORES-EMPRESA.md) | Especificacao tecnica da Sprint 18: esteira de primeiro acesso administrativo e pre-login de funcionarios corporativos. |
| [docs/SPEC-SPRINT-19-ONBOARDING-EMPRESA-DADOS-COMPLEMENTARES.md](docs/SPEC-SPRINT-19-ONBOARDING-EMPRESA-DADOS-COMPLEMENTARES.md) | Especificacao tecnica da Sprint 19: persistencia de dados complementares corporativos, endereco e logotipo (Flyway V6). |
| [docs/SPEC-SPRINT-20-GESTAO-FROTA-CAMINHOES-RELATORIO-MOBILE.md](docs/SPEC-SPRINT-20-GESTAO-FROTA-CAMINHOES-RELATORIO-MOBILE.md) | Especificacao tecnica da Sprint 20: gestao desacoplada de frota veicular e bloqueio por inspecao sanitaria vencida (Flyway V7). |
| [docs/SPEC-SPRINT-21-ASSINATURA-FIXA-MOTORISTA-IMAGEM.md](docs/SPEC-SPRINT-21-ASSINATURA-FIXA-MOTORISTA-IMAGEM.md) | Especificacao tecnica da Sprint 21: assinatura fixa do motorista como imagem PNG e versionamento append-only (Flyway V8). |
| [docs/SPEC-SPRINT-22-DIARIO-ROTA-API.md](docs/SPEC-SPRINT-22-DIARIO-ROTA-API.md) | Especificacao tecnica da Sprint 22: integracao do diario de rota mobile em 6 etapas e contratos de salvamento progressivo. |
| [docs/SPEC-SPRINT-23-ASSINATURAS-PARTICIPANTES-RELATORIO.md](docs/SPEC-SPRINT-23-ASSINATURAS-PARTICIPANTES-RELATORIO.md) | Especificacao tecnica da Sprint 23: upload binario multipart e streaming das assinaturas de pecuarista, manobrista e curraleiro. |
