# Estrutura Oficial do Jira: DS2 — API REST Principal (Backend Java / Spring Boot)

**Projeto Jira:** `EFFICIENTI`  
**Disciplina Acadêmica:** Desenvolvimento de Sistemas 2 (DS2) — 2º Ano Técnico  
**Épico / História Guarda-Chuva:** `EFFICIENTI-94` — **DS2: Desenvolvimento da API REST Principal (Java / Spring Boot)**  
**Repositório Base:** `efficientia-API/efficientia`  
**Tecnologias:** Java 26, Spring Boot 4.1.0, Spring Data JPA, Spring Security (JWT HS256), PostgreSQL, Flyway, Docker, Swagger / OpenAPI 3.0, Micrometer/Prometheus.

---

## 1. Visão Geral da Arquitetura do Backlog

A estrutura de **DS2** no Jira reflete rigorosamente o padrão adotado para **DAD (Desenvolvimento de Aplicações Dinâmicas - `EFFICIENTI-16`)**, organizando as entregas do backend em uma hierarquia corporativa de alto nível:

```
[ÉPICO / HISTÓRIA PAI] EFFICIENTI-94: DS2 - API REST Principal
 ├── [SPRINT 01] EFFICIENTI-218: Fundação da API, Health Check e Pipeline CI
 ├── [SPRINT 02] EFFICIENTI-219: Banco de Dados PostgreSQL e Migrações Flyway (V1)
 ├── [SPRINT 03] EFFICIENTI-220: Domínio e Persistência de Documentos (V2)
 ├── [SPRINT 04] EFFICIENTI-221: Storage Privado e Integridade Criptográfica (SHA-256)
 ├── [SPRINT 05] EFFICIENTI-222: Upload Multipart de PDF/PNG e Idempotência
 ├── [SPRINT 06] EFFICIENTI-223: Assinaturas Digitais Multimodais (Foto, Desenho e Texto)
 ├── [SPRINT 07] EFFICIENTI-224: Listagem Paginada, Filtros e Cursor Opaco para Mobile/IA
 ├── [SPRINT 08] EFFICIENTI-225: Download Streaming Seguro e Pré-visualização Inline
 ├── [SPRINT 09] EFFICIENTI-226: Ciclo de Vida de Documentos: CRUD, Exclusão e Auditoria (V3)
 ├── [SPRINT 10] EFFICIENTI-227: Validações Estritas, RFC 7807 e OpenAPI/Swagger UI
 ├── [SPRINT 11] EFFICIENTI-228: Segurança Spring Security, Autenticação JWT e CORS
 ├── [SPRINT 12] EFFICIENTI-229: Exportação Assíncrona de Lote e Enfileiramento (V4)
 ├── [SPRINT 13] EFFICIENTI-230: Worker ZIP em Background e Download Consolidado
 ├── [SPRINT 14] EFFICIENTI-231: Observabilidade, Logs Distribuídos e Métricas Prometheus
 ├── [SPRINT 15] EFFICIENTI-232: Homologação E2E Testcontainers, Release 1.0.0 e Deploy Render
 ├── [SPRINT 16] EFFICIENTI-233: Fluxo de Autenticação Operacional e Login Mobile
 ├── [SPRINT 17] EFFICIENTI-234: Cadastro de Empresas e Gerador de Código de 8 Dígitos (V5)
 ├── [SPRINT 18] EFFICIENTI-235: Gestão Multi-tenant SaaS, 1º Acesso ADM e Adesão de Gestores
 └── [SPRINT 19] EFFICIENTI-236: Onboarding Corporativo: Etapa 1 de 3 (Dados, Endereço e Logo) (V6)
```

---

## 2. Detalhamento das Sprints de DS2 (Histórias, Tarefas e Critérios)

### SPRINT 01 — Fundação da API REST Principal, Spring Boot e CI
- **Chave Jira:** `EFFICIENTI-218`
- **Tipo:** História / Task
- **Épico Pai:** `EFFICIENTI-94` (DS2)
- **Estimativa:** 5 Story Points
- **Status:** Concluída (Mergeada na `main`)
- **Descrição:** Estabelecer a fundação do projeto Spring Boot em Java moderno, configurando o gerenciador de dependências Maven, plugins de compilação, endpoint de verificação de integridade operacional (Health Check) e esteira automatizada de Integração Contínua (CI) via GitHub Actions.
- **Subtarefas (Subtasks):**
  - `EFFICIENTI-218.1`: Inicialização do Spring Boot com dependências Web, Actuator e Testes.
  - `EFFICIENTI-218.2`: Criação do endpoint `GET /api/v1/status` e `/actuator/health`.
  - `EFFICIENTI-218.3`: Configuração da GitHub Actions (`.github/workflows/ci.yml`).
- **Critérios de Aceite:**
  - `GET /api/v1/status` retorna HTTP 200 com JSON contendo nome, versão e status "UP".
  - Pipeline de CI executa `mvn verify` em ambiente Ubuntu e Java 26 com sucesso.

---

### SPRINT 02 — Banco de Dados PostgreSQL e Migrações Flyway (V1)
- **Chave Jira:** `EFFICIENTI-219`
- **Tipo:** História / Task
- **Épico Pai:** `EFFICIENTI-94` (DS2)
- **Estimativa:** 8 Story Points
- **Status:** Concluída (Mergeada na `main`)
- **Descrição:** Implementar a camada relacional base com PostgreSQL (Supabase), configurando versionamento de schema com Flyway (`V1__create_relational_schema.sql`), pool de conexões HikariCP e entidades Spring Data JPA para usuários operacionais, endereços, fazendas, unidades frigoríficas e veículos (cavalos e carretas).
- **Subtarefas (Subtasks):**
  - `EFFICIENTI-219.1`: Escrita do script DDL `V1__create_relational_schema.sql` normalizado em 3FN.
  - `EFFICIENTI-219.2`: Mapeamento de entidades JPA (`UsuarioEntity`, `EnderecoEntity`, `FazendaEntity`, etc.).
  - `EFFICIENTI-219.3`: Configuração de properties para session pooler Supabase (porta 5432).
  - `EFFICIENTI-219.4`: Criação dos endpoints base de cadastro em `/api/v1/usuarios`, `/enderecos`, etc.
- **Critérios de Aceite:**
  - Aplicação executa as migrations Flyway na inicialização sem erros de schema.
  - Todas as chaves estrangeiras (`ON DELETE RESTRICT`) garantem integridade referencial.

---

### SPRINT 03 — Domínio e Persistência de Documentos Operacionais (V2)
- **Chave Jira:** `EFFICIENTI-220`
- **Tipo:** História / Task
- **Épico Pai:** `EFFICIENTI-94` (DS2)
- **Estimativa:** 5 Story Points
- **Status:** Concluída (Mergeada na `main`)
- **Descrição:** Modelar a entidade de domínio `Documento` e criar a migration Flyway `V2__create_documento.sql` para suportar metadados de GTAs, tickets de pesagem e comprovantes de despesa vinculados aos relatórios de viagem dos motoristas.
- **Subtarefas (Subtasks):**
  - `EFFICIENTI-220.1`: Criação da migration `V2__create_documento.sql` com índices em `viagem_id` e `idempotency_key`.
  - `EFFICIENTI-220.2`: Criação da entidade `DocumentoEntity` com mapeamento de UUID e Enums.
  - `EFFICIENTI-220.3`: Criação do repositório `DocumentoRepository`.
- **Critérios de Aceite:**
  - `documento.id` é gerado como UUID público independente de auto-incremento.
  - Restrição única em `idempotency_key` impede duplicidades a nível de banco.

---

### SPRINT 04 — Storage Privado e Integridade Criptográfica (SHA-256)
- **Chave Jira:** `EFFICIENTI-221`
- **Tipo:** História / Task
- **Épico Pai:** `EFFICIENTI-94` (DS2)
- **Estimativa:** 8 Story Points
- **Status:** Concluída (Mergeada na `main`)
- **Descrição:** Desenvolver o subsistema de armazenamento seguro de arquivos binários (PDF e PNG) desacoplado do provedor físico (`StorageService`), implementando streaming em disco, cálculo obrigatório de SHA-256 durante a cópia e proteção contra Path Traversal (`../`).
- **Subtarefas (Subtasks):**
  - `EFFICIENTI-221.1`: Interface `StorageService` e implementação `LocalStorageService`.
  - `EFFICIENTI-221.2`: Algoritmo de stream que computa hash SHA-256 sem alocação em memória.
  - `EFFICIENTI-221.3`: Validação de segurança de chaves de storage e caminhos canônicos.
- **Critérios de Aceite:**
  - Arquivo é gravado exclusivamente na pasta privada fora do web root.
  - Tentativas de acesso com `..` ou caracteres inválidos disparam exceções de segurança imediatas.

---

### SPRINT 05 — Upload Multipart de PDF/PNG e Idempotência
- **Chave Jira:** `EFFICIENTI-222`
- **Tipo:** História / Task
- **Épico Pai:** `EFFICIENTI-94` (DS2)
- **Estimativa:** 8 Story Points
- **Status:** Concluída (Mergeada na `main`)
- **Descrição:** Criar o fluxo ponta a ponta de upload de documentos operacionais via HTTP `POST /api/v1/documentos` utilizando `multipart/form-data`, validação rigorosa de metadados JSON e garantia de idempotência via cabeçalho `Idempotency-Key`.
- **Subtarefas (Subtasks):**
  - `EFFICIENTI-222.1`: DTO `DocumentoMetadataRequest` com validações Jakarta Validation.
  - `EFFICIENTI-222.2`: Controller `POST /api/v1/documentos` suportando partes `metadados` e `arquivo`.
  - `EFFICIENTI-222.3`: Tratamento de requisições idempotentes repetidas (retorna registro original sem duplicar).
- **Critérios de Aceite:**
  - Envio bem-sucedido retorna HTTP 201 Created com UUID do documento criado.
  - Reenvio com a mesma `Idempotency-Key` retorna o documento já salvo sem gravar novo arquivo físico.

---

### SPRINT 06 — Assinaturas Digitais Multimodais (Foto, Desenho e Texto)
- **Chave Jira:** `EFFICIENTI-223`
- **Tipo:** História / Task
- **Épico Pai:** `EFFICIENTI-94` (DS2)
- **Estimativa:** 5 Story Points
- **Status:** Concluída (Mergeada na `main`)
- **Descrição:** Habilitar a captura de assinaturas digitais nas modalidades Foto, Desenho (Canvas) e Texto digitado para motoristas, manobristas e curraleiros, com exclusividade mútua entre arquivo binário e conteúdo textual.
- **Subtarefas (Subtasks):**
  - `EFFICIENTI-223.1`: Validador `AssinaturaValidator` com regras de modalidade.
  - `EFFICIENTI-223.2`: Suporte a `application/json` em `POST /api/v1/documentos` para assinaturas textuais puras.
  - `EFFICIENTI-223.3`: Associação do papel do assinante (`PapelAssinante`: MOTORISTA, MANOBRISTA, CURRALEIRO).
- **Critérios de Aceite:**
  - Assinaturas de foto/desenho exigem PNG binário; assinaturas de texto proíbem envio de arquivo.
  - Papel do assinante é registrado e validado de acordo com as regras de negócio.

---

### SPRINT 07 — Listagem Paginada, Filtros e Cursor Opaco para Mobile/IA
- **Chave Jira:** `EFFICIENTI-224`
- **Tipo:** História / Task
- **Épico Pai:** `EFFICIENTI-94` (DS2)
- **Estimativa:** 8 Story Points
- **Status:** Concluída (Mergeada na `main`)
- **Descrição:** Implementar a API de consulta de documentos (`GET /api/v1/documentos`) oferecendo tanto paginação tradicional por offset/limit para o portal Web quanto paginação contínua e estável por cursor opaco (`DocumentoCursorCodec`) para o app Mobile e ingestão de IA.
- **Subtarefas (Subtasks):**
  - `EFFICIENTI-224.1`: Implementação do codec de cursor opaco codificado em Base64.
  - `EFFICIENTI-224.2`: Filtros por `viagemId`, `tipoDocumento`, intervalo de datas (`criadoDe`, `criadoAte`).
  - `EFFICIENTI-224.3`: Repositório de consulta com Criteria API / JPQL otimizada por índices.
- **Critérios de Aceite:**
  - Consulta por cursor navega por grandes volumes sem degradação de performance (`OFFSET`).
  - Metadados públicos são retornados sem expor caminhos internos de storage.

---

### SPRINT 08 — Download Streaming Seguro e Pré-visualização Inline
- **Chave Jira:** `EFFICIENTI-225`
- **Tipo:** História / Task
- **Épico Pai:** `EFFICIENTI-94` (DS2)
- **Estimativa:** 5 Story Points
- **Status:** Concluída (Mergeada na `main`)
- **Descrição:** Disponibilizar o endpoint `GET /api/v1/documentos/{id}/conteudo` permitindo download forçado (`inline=false`) ou exibição direta em navegadores e visualizadores mobile (`inline=true`), utilizando streaming assíncrono via `StreamingResponseBody`.
- **Subtarefas (Subtasks):**
  - `EFFICIENTI-225.1`: Serviço de streaming de bytes diretamente do storage para o canal HTTP.
  - `EFFICIENTI-225.2`: Cabeçalhos HTTP dinâmicos: `Content-Disposition`, `Content-Type` e `Content-Length`.
  - `EFFICIENTI-225.3`: Tratamento de erro 404 para arquivos inexistentes ou registros removidos.
- **Critérios de Aceite:**
  - Download de arquivos grandes não consome memória heap do servidor (streaming eficiente).
  - Parâmetro `inline=true` exibe o PDF/PNG na janela do browser sem forçar caixa de diálogo de download.

---

### SPRINT 09 — Ciclo de Vida de Documentos: CRUD, Exclusão e Auditoria (V3)
- **Chave Jira:** `EFFICIENTI-226`
- **Tipo:** História / Task
- **Épico Pai:** `EFFICIENTI-94` (DS2)
- **Estimativa:** 8 Story Points
- **Status:** Concluída (Mergeada na `main`)
- **Descrição:** Finalizar as operações de atualização (`PATCH /api/v1/documentos/{id}`) e remoção (`DELETE /api/v1/documentos/{id}`), acompanhada de bloqueio otimista via `@Version` e trilha de auditoria universal no PostgreSQL (`V3__create_documento_auditoria.sql`).
- **Subtarefas (Subtasks):**
  - `EFFICIENTI-226.1`: Migration `V3__create_documento_auditoria.sql` para rastreamento de operações.
  - `EFFICIENTI-226.2`: Endpoint `PATCH /api/v1/documentos/{id}` com controle de concorrência (`versao`).
  - `EFFICIENTI-226.3`: Endpoint `DELETE /api/v1/documentos/{id}` com deleção transacional do arquivo físico.
- **Critérios de Aceite:**
  - Conflito de versão em `PATCH` retorna HTTP 409 Conflict prevenindo sobrescritas acidentais.
  - Remoção de documento grava log de auditoria e limpa o binário do disco.

---

### SPRINT 10 — Validações Estritas, RFC 7807 e OpenAPI/Swagger UI
- **Chave Jira:** `EFFICIENTI-227`
- **Tipo:** História / Task
- **Épico Pai:** `EFFICIENTI-94` (DS2)
- **Estimativa:** 5 Story Points
- **Status:** Concluída (Mergeada na `main`)
- **Descrição:** Padronizar todas as respostas de erro da API de acordo com o padrão RFC 7807 (`ProblemDetail`), implementar validação estrita de magic bytes para PDFs e PNGs, e expor a documentação interativa OpenAPI 3.0 via Swagger UI.
- **Subtarefas (Subtasks):**
  - `EFFICIENTI-227.1`: Global Exception Handler (`@RestControllerAdvice`) formatando RFC 7807.
  - `EFFICIENTI-227.2`: Validador de cabeçalho binário (magic numbers `%PDF` e `\x89PNG`).
  - `EFFICIENTI-227.3`: Configuração do SpringDoc OpenAPI (`/swagger-ui.html` e `/v3/api-docs`).
- **Critérios de Aceite:**
  - Qualquer erro de validação (400, 404, 409, 415, 500) retorna JSON padronizado com `title`, `status` e `detail`.
  - Documentação Swagger UI reflete todos os endpoints, DTOs e códigos de resposta.

---

### SPRINT 11 — Segurança Spring Security, Autenticação JWT e CORS
- **Chave Jira:** `EFFICIENTI-228`
- **Tipo:** História / Task
- **Épico Pai:** `EFFICIENTI-94` (DS2)
- **Estimativa:** 8 Story Points
- **Status:** Concluída (Mergeada na `main`)
- **Descrição:** Proteger as rotas da API com Spring Security 6 e tokens JWT (Bearer Token HS256), aplicando controle de acesso baseado em papéis (RBAC) para motoristas, analistas e administradores, além de filtros CORS rigorosos para o frontend web.
- **Subtarefas (Subtasks):**
  - `EFFICIENTI-228.1`: Implementação de `JwtTokenService` (emissão, parsing e validação criptográfica).
  - `EFFICIENTI-228.2`: Configuração do `SecurityFilterChain` e conversor de authorities `JwtRoleConverter`.
  - `EFFICIENTI-228.3`: Configuração centralizada de CORS para as origens do React e ambientes locais.
- **Critérios de Aceite:**
  - Rotas protegidas sem token ou com token inválido retornam HTTP 401 Unauthorized.
  - Usuários com papéis operacionais (motoristas) não acessam rotas administrativas restritas (403 Forbidden).

---

### SPRINT 12 — Exportação Assíncrona de Lote e Enfileiramento (V4)
- **Chave Jira:** `EFFICIENTI-229`
- **Tipo:** História / Task
- **Épico Pai:** `EFFICIENTI-94` (DS2)
- **Estimativa:** 5 Story Points
- **Status:** Concluída (Mergeada na `main`)
- **Descrição:** Criar a infraestrutura de banco de dados (`V4__create_exportacao.sql`) e o endpoint `POST /api/v1/exportacoes` para solicitar o empacotamento assíncrono de múltiplos documentos, retornando imediatamente HTTP 202 Accepted.
- **Subtarefas (Subtasks):**
  - `EFFICIENTI-229.1`: Migration `V4__create_exportacao.sql` com tabelas `exportacao` e `exportacao_documento`.
  - `EFFICIENTI-229.2`: Endpoint `POST /api/v1/exportacoes` com idempotência e validação de documentos existentes.
  - `EFFICIENTI-229.3`: Endpoint de consulta de status `GET /api/v1/exportacoes/{id}` (`PENDENTE`, `EM_PROCESSAMENTO`).
- **Critérios de Aceite:**
  - Requisição de exportação aceita retorna HTTP 202 com cabeçalhos `Location` e `Retry-After: 2`.
  - Estado inicial `PENDENTE` é registrado no banco de forma transacional.

---

### SPRINT 13 — Worker ZIP em Background e Download Consolidado
- **Chave Jira:** `EFFICIENTI-230`
- **Tipo:** História / Task
- **Épico Pai:** `EFFICIENTI-94` (DS2)
- **Estimativa:** 8 Story Points
- **Status:** Concluída (Mergeada na `main`)
- **Descrição:** Implementar o worker agendado (`ExportacaoWorker`) para compactação em arquivo `.zip` dos documentos solicitados, serviço de expiração automática de downloads antigos (`ExportacaoExpirationWorker`) e endpoint de download seguro `GET /api/v1/exportacoes/{id}/conteudo`.
- **Subtarefas (Subtasks):**
  - `EFFICIENTI-230.1`: Worker de compactação assíncrona usando `ZipOutputStream`.
  - `EFFICIENTI-230.2`: Endpoint de download de arquivo ZIP com verificação de status `CONCLUIDO`.
  - `EFFICIENTI-230.3`: Job de limpeza automática de arquivos expirados após janela de retenção.
- **Critérios de Aceite:**
  - Download entrega o arquivo `.zip` íntegro contendo todos os PDFs e PNGs solicitados.
  - Tentativa de download antes da conclusão retorna HTTP 409 Conflict; após expiração retorna HTTP 410 Gone.

---

### SPRINT 14 — Observabilidade, Logs Distribuídos e Métricas Prometheus
- **Chave Jira:** `EFFICIENTI-231`
- **Tipo:** História / Task
- **Épico Pai:** `EFFICIENTI-94` (DS2)
- **Estimativa:** 5 Story Points
- **Status:** Concluída (Mergeada na `main`)
- **Descrição:** Instrumentar a API REST com observabilidade ponta a ponta, incluindo correlação de requisições (`X-Correlation-Id`) via MDC, métricas customizadas com Micrometer (`/actuator/prometheus`), health check customizado do storage privado e estabilização de cursor para IA.
- **Subtarefas (Subtasks):**
  - `EFFICIENTI-231.1`: Filtro de correlação distribuída `CorrelationIdFilter`.
  - `EFFICIENTI-231.2`: Serviço de métricas `EfficientiaMetricsService` registrando counters e timers de upload/exportação.
  - `EFFICIENTI-231.3`: Health Indicator customizado `StorageHealthIndicator` exposto no Actuator.
- **Critérios de Aceite:**
  - Logs registram o correlation ID em todas as linhas de processamento.
  - `/actuator/metrics` e `/actuator/prometheus` expõem latências e contadores da aplicação.

---

### SPRINT 15 — Homologação E2E Testcontainers, Release 1.0.0 e Deploy Render
- **Chave Jira:** `EFFICIENTI-232`
- **Tipo:** História / Task
- **Épico Pai:** `EFFICIENTI-94` (DS2)
- **Estimativa:** 8 Story Points
- **Status:** Concluída (Mergeada na `main`)
- **Descrição:** Homologação final para release 1.0.0 com suíte de testes de integração ponta a ponta usando Testcontainers PostgreSQL real, criação do Dockerfile multi-stage enxuto e implantação no ambiente de nuvem do Render.
- **Subtarefas (Subtasks):**
  - `EFFICIENTI-232.1`: Testes de integração automatizados com `@Testcontainers` (`PostgresIntegrationTest`).
  - `EFFICIENTI-232.2`: Construção do `Dockerfile` multi-stage com OpenJDK headless e usuário não-root.
  - `EFFICIENTI-232.3`: Configuração de deploy contínuo na nuvem (Render) com SSL e pooling de banco.
- **Critérios de Aceite:**
  - 100% dos testes passam sem mocks de banco, validando todas as migrations Flyway de V1 a V4.
  - API implantada e acessível em `https://efficientia-api.onrender.com`.

---

### SPRINT 16 — Fluxo de Autenticação Operacional e Login Mobile
- **Chave Jira:** `EFFICIENTI-233`
- **Tipo:** História / Task
- **Épico Pai:** `EFFICIENTI-94` (DS2)
- **Estimativa:** 5 Story Points
- **Status:** Concluída (Mergeada na `main`)
- **Descrição:** Implementar os endpoints de autenticação operacional para consumo pelo aplicativo mobile (`POST /api/v1/auth/login` e `POST /api/v1/auth/signup`), validando credenciais (CPF/e-mail, senha criptografada em BCrypt e código de empresa) diretamente no banco PostgreSQL.
- **Subtarefas (Subtasks):**
  - `EFFICIENTI-233.1`: Endpoints públicos de login e cadastro em `AuthController`.
  - `EFFICIENTI-233.2`: Emissão de JWT contendo `usuario_id`, `tipo_usuario` e `codigo_interno`.
  - `EFFICIENTI-233.3`: Testes unitários e de integração de autenticação com dados inválidos e ativos.
- **Critérios de Aceite:**
  - Login bem-sucedido retorna token JWT Bearer e dados cadastrais do motorista sem a senha.
  - Falhas de credencial retornam HTTP 401 Unauthorized com RFC 7807.

---

### SPRINT 17 — Cadastro de Empresas e Gerador de Código de 8 Dígitos (V5)
- **Chave Jira:** `EFFICIENTI-234`
- **Tipo:** História / Task
- **Épico Pai:** `EFFICIENTI-94` (DS2)
- **Estimativa:** 8 Story Points
- **Status:** Concluída (Mergeada na `main`)
- **Descrição:** Criar a estrutura corporativa multi-tenant (SaaS) com a migration `V5__create_empresa_and_configuracao.sql`, endpoint `POST /api/v1/empresas` e algoritmo de geração autônoma de código empresarial único de 8 dígitos (`CodigoEmpresaGenerator`: 3 letras + 5 números aleatórios, ex: `FRI48291`).
- **Subtarefas (Subtasks):**
  - `EFFICIENTI-234.1`: Migration `V5__create_empresa_and_configuracao.sql` criando tabela `empresa` e constraints.
  - `EFFICIENTI-234.2`: Algoritmo `CodigoEmpresaGenerator` com sanitização e garantia de unicidade.
  - `EFFICIENTI-234.3`: Endpoints de busca pública por código (`GET /api/v1/empresas/codigo/{codigo}`) e CNPJ.
- **Critérios de Aceite:**
  - Código gerado atende rigorosamente à regex `^[A-Z]{3}[0-9]{5}$`.
  - Empresa recém-criada sinaliza obrigatoriedade do primeiro acesso de administrador (`requerPrimeiroAdmin: true`).

---

### SPRINT 18 — Gestão Multi-tenant SaaS, 1º Acesso ADM e Adesão de Gestores
- **Chave Jira:** `EFFICIENTI-235`
- **Tipo:** História / Task
- **Épico Pai:** `EFFICIENTI-94` (DS2)
- **Estimativa:** 8 Story Points
- **Status:** Concluída (Mergeada na `main`)
- **Descrição:** Implementar a esteira obrigatória de onboarding do primeiro administrador (`POST /api/v1/auth/adm/primeiro-acesso`) para liberação do acesso corporativo, login de gestores com perfil `ROLE_ADMIN`, adesão de novos administradores (`POST /api/v1/empresas/{id}/adms`) e pré-login de funcionários da empresa (motoristas e analistas).
- **Subtarefas (Subtasks):**
  - `EFFICIENTI-235.1`: Endpoint de primeiro acesso com emissão imediata de token `ROLE_ADMIN`.
  - `EFFICIENTI-235.2`: Endpoint de login corporativo `POST /api/v1/auth/adm/login`.
  - `EFFICIENTI-235.3`: Endpoints de adesão de gestores e pré-cadastro de motoristas/analistas vinculados à empresa.
- **Critérios de Aceite:**
  - Primeiro acesso é bloqueado com HTTP 403 se a empresa já possuir gestor registrado.
  - Administradores só podem aderir novos gestores ou funcionários para a sua própria empresa.

---

### SPRINT 19 — Onboarding Corporativo: Etapa 1 de 3 (Dados, Endereço e Logo) (V6)
- **Chave Jira:** `EFFICIENTI-236`
- **Tipo:** História / Task
- **Épico Pai:** `EFFICIENTI-94` (DS2)
- **Estimativa:** 8 Story Points
- **Status:** Concluída (Mergeada na branch `feat/onboarding-empresa-dados-complementares`)
- **Descrição:** Implementar a **Etapa 1 de 3** do Onboarding Corporativo Web: persistência de dados cadastrais complementares (telefone corporativo, nome fantasia, razão social, CNPJ, e-mail), endereço completo (CEP, logradouro, número, cidade, UF) com vínculo a `endereco_id`, upload de logotipo corporativo PNG/SVG até 5 MB (`POST /api/v1/empresas/{id}/logo`), streaming público de visualização (`GET /api/v1/empresas/{id}/logo/conteudo`) e migration Flyway `V6__empresa_dados_complementares.sql`.
- **Subtarefas (Subtasks):**
  - `EFFICIENTI-236.1`: Migration `V6__empresa_dados_complementares.sql` adicionando colunas `telefone`, `nome_fantasia`, `logo_url`, `etapa_cadastro` e `cadastro_completo`.
  - `EFFICIENTI-236.2`: Endpoints `PUT /api/v1/empresas/{id}/dados-complementares` e `PUT /api/v1/empresas/codigo/{codigo}` avançando para a etapa 2.
  - `EFFICIENTI-236.3`: Upload de logotipo via `multipart/form-data` com validação de tamanho (até 5 MB) e tipo (PNG ou SVG).
  - `EFFICIENTI-236.4`: Endpoint público de entrega de imagem com cabeçalhos de cache.
  - `EFFICIENTI-236.5`: Testes unitários e de integração cobrindo 100% dos cenários positivos e negativos.
- **Critérios de Aceite:**
  - Salvar a Etapa 1 avança o indicador `etapaCadastro` para 2 e vincula o endereço no banco.
  - Upload aceita apenas PNG ou SVG de até 5 MB; formatos ou tamanhos divergentes retornam HTTP 400.
  - Logotipo é acessível publicamente via URL sem necessidade de autenticação para renderização em tags `<img>`.

---

## 3. Matriz Consolidada de Rastreabilidade (Jira x Entregáveis de Código)

| Sprint | Chave Jira | Título do Item de Backlog | Migration Flyway | Endpoints REST Principais | Status |
| :--- | :--- | :--- | :--- | :--- | :--- |
| **01** | `EFFICIENTI-218` | Fundação API REST, Actuator e CI | — | `GET /api/v1/status`, `/actuator/health` | **Concluída** |
| **02** | `EFFICIENTI-219` | PostgreSQL e Modelagem Relacional | `V1` | `POST /api/v1/usuarios`, `/enderecos`, `/fazendas` | **Concluída** |
| **03** | `EFFICIENTI-220` | Domínio e Persistência de Documentos | `V2` | Domínio JPA `DocumentoEntity` | **Concluída** |
| **04** | `EFFICIENTI-221` | Storage Privado e Hash SHA-256 | — | `StorageService`, `LocalStorageService` | **Concluída** |
| **05** | `EFFICIENTI-222` | Upload Multipart e Idempotência | — | `POST /api/v1/documentos` (multipart) | **Concluída** |
| **06** | `EFFICIENTI-223` | Assinaturas Digitais Multimodais | — | `POST /api/v1/documentos` (json/multipart) | **Concluída** |
| **07** | `EFFICIENTI-224` | Listagem Paginada e Cursor Opaco | — | `GET /api/v1/documentos`, `GET /documentos/{id}` | **Concluída** |
| **08** | `EFFICIENTI-225` | Download Streaming e Visualização Inline | — | `GET /api/v1/documentos/{id}/conteudo` | **Concluída** |
| **09** | `EFFICIENTI-226` | Ciclo de Vida: CRUD e Auditoria | `V3` | `PATCH /documentos/{id}`, `DELETE /documentos/{id}` | **Concluída** |
| **10** | `EFFICIENTI-227` | Validações, RFC 7807 e OpenAPI | — | `/swagger-ui.html`, `/v3/api-docs` | **Concluída** |
| **11** | `EFFICIENTI-228` | Segurança JWT Stateless e RBAC | — | Filtros de Segurança, `JwtTokenService` | **Concluída** |
| **12** | `EFFICIENTI-229` | Exportação Assíncrona de Lote | `V4` | `POST /api/v1/exportacoes` (HTTP 202) | **Concluída** |
| **13** | `EFFICIENTI-230` | Worker ZIP e Download de Exportação | — | `GET /api/v1/exportacoes/{id}/conteudo` | **Concluída** |
| **14** | `EFFICIENTI-231` | Observabilidade e Métricas Prometheus | — | `CorrelationIdFilter`, `/actuator/prometheus` | **Concluída** |
| **15** | `EFFICIENTI-232` | Homologação E2E, Release e Deploy | — | Dockerfile, Deploy Render Cloud | **Concluída** |
| **16** | `EFFICIENTI-233` | Autenticação e Login Mobile | — | `POST /api/v1/auth/login`, `POST /auth/signup` | **Concluída** |
| **17** | `EFFICIENTI-234` | Cadastro de Empresas e Código 8 Dígitos | `V5` | `POST /api/v1/empresas`, `GET /empresas/codigo/{c}` | **Concluída** |
| **18** | `EFFICIENTI-235` | Multi-tenant SaaS e Gestão de Gestores | — | `POST /api/v1/auth/adm/primeiro-acesso`, `/adms` | **Concluída** |
| **19** | `EFFICIENTI-236` | Onboarding: Dados, Endereço e Logo | `V6` | `PUT /empresas/{id}/dados-complementares`, `/logo` | **Concluída** |

---

## 4. Tabela de Importação Direta para o Jira (CSV Format)

Caso seja necessário importar em lote para o Jira Software (via *Jira Settings > System > External System Import > CSV*):

```csv
Issue Type,Issue key,Summary,Parent,Story Points,Status,Description
Epic,EFFICIENTI-94,DS2: Desenvolvimento da API REST Principal (Java / Spring Boot),,130,Em Andamento,"Épico guarda-chuva responsável por todos os requisitos da disciplina de Desenvolvimento de Sistemas 2 (DS2), provendo a API RESTful central para Mobile, Web e IA."
Story,EFFICIENTI-218,Sprint 01: Fundação da API REST Principal e CI,EFFICIENTI-94,5,Done,"Fundação Spring Boot, Actuator e esteira GitHub Actions."
Story,EFFICIENTI-219,Sprint 02: PostgreSQL e Migrações Flyway (V1),EFFICIENTI-94,8,Done,"Setup do PostgreSQL com Supabase, migração V1 e entidades base JPA."
Story,EFFICIENTI-220,Sprint 03: Domínio e Persistência de Documentos (V2),EFFICIENTI-94,5,Done,"Criação da tabela de documentos no PostgreSQL com UUID público e Flyway V2."
Story,EFFICIENTI-221,Sprint 04: Storage Privado e Integridade Criptográfica (SHA-256),EFFICIENTI-94,8,Done,"Camada de StorageService desacoplada, hash SHA-256 e blindagem contra Path Traversal."
Story,EFFICIENTI-222,Sprint 05: Upload Multipart de PDF/PNG e Idempotência,EFFICIENTI-94,8,Done,"Endpoint POST /api/v1/documentos com suporte a multipart e Idempotency-Key."
Story,EFFICIENTI-223,Sprint 06: Assinaturas Digitais Multimodais,EFFICIENTI-94,5,Done,"Assinaturas por foto, desenho e texto com validação estrita de exclusividade."
Story,EFFICIENTI-224,Sprint 07: Listagem Paginada e Cursor Opaco para Mobile/IA,EFFICIENTI-94,8,Done,"Paginação por cursor opaco Base64 e filtros dinâmicos de documentos."
Story,EFFICIENTI-225,Sprint 08: Download Streaming Seguro e Visualização Inline,EFFICIENTI-94,5,Done,"Endpoint de streaming de arquivos binários com flag inline para browser."
Story,EFFICIENTI-226,Sprint 09: Ciclo de Vida de Documentos: CRUD e Auditoria (V3),EFFICIENTI-94,8,Done,"Atualização otimista com PATCH, deleção com DELETE e tabela de auditoria V3."
Story,EFFICIENTI-227,Sprint 10: Validações Estritas, RFC 7807 e OpenAPI/Swagger UI,EFFICIENTI-94,5,Done,"Padronização RFC 7807 ProblemDetail, magic bytes e Swagger interativo."
Story,EFFICIENTI-228,Sprint 11: Segurança Spring Security, JWT e CORS,EFFICIENTI-94,8,Done,"Controle RBAC stateless com tokens JWT Bearer e filtro de CORS."
Story,EFFICIENTI-229,Sprint 12: Exportação Assíncrona de Lote e Enfileiramento (V4),EFFICIENTI-94,5,Done,"Fila de empacotamento em lote, retorno HTTP 202 Accepted e Flyway V4."
Story,EFFICIENTI-230,Sprint 13: Worker ZIP em Background e Download Consolidado,EFFICIENTI-94,8,Done,"Processamento assíncrono em lote, geração de ZIPs e limpeza automática."
Story,EFFICIENTI-231,Sprint 14: Observabilidade, MDC e Métricas Prometheus,EFFICIENTI-94,5,Done,"Correlation ID distribuído, métricas Micrometer e health check de storage."
Story,EFFICIENTI-232,Sprint 15: Homologação E2E Testcontainers e Deploy Render,EFFICIENTI-94,8,Done,"Testes ponta a ponta com Docker Postgres real, Dockerfile e deploy Render."
Story,EFFICIENTI-233,Sprint 16: Fluxo de Autenticação Operacional e Login Mobile,EFFICIENTI-94,5,Done,"Autenticação mobile por CPF/e-mail, senha e código de empresa."
Story,EFFICIENTI-234,Sprint 17: Cadastro de Empresas e Código de 8 Dígitos (V5),EFFICIENTI-94,8,Done,"Estrutura corporativa multi-tenant e gerador autônomo de código empresarial."
Story,EFFICIENTI-235,Sprint 18: Gestão Multi-tenant SaaS e Adesão de Administradores,EFFICIENTI-94,8,Done,"Primeiro acesso obrigatório de gestores e pré-login de funcionários da empresa."
Story,EFFICIENTI-236,Sprint 19: Onboarding Corporativo: Etapa 1 de 3 (Dados, Endereço e Logo) (V6),EFFICIENTI-94,8,Done,"Etapa 1 de onboarding: telefone, endereço completo, upload e streaming de logo PNG/SVG."
```
