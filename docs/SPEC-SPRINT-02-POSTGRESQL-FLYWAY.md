# SPEC Sprint 02 — PostgreSQL e migrações Flyway

## 1. Identificação

- Projeto: EFFICIENTI — Espaço Efficientia.
- Item principal: [EFFICIENTI-94 — API REST Principal](https://efficientia-team-inter-2026.atlassian.net/browse/EFFICIENTI-94).
- Sprint: Sprint 02 — 10–14/08/2026.
- Tarefa: [EFFICIENTI-219 — PostgreSQL e migrações Flyway](https://efficientia-team-inter-2026.atlassian.net/browse/EFFICIENTI-219).
- Dependência funcional: [EFFICIENTI-95 — Persistência PostgreSQL](https://efficientia-team-inter-2026.atlassian.net/browse/EFFICIENTI-95).
- Branch observada: `build/dev-back/efficient-219/postgresql-migracao-flyway`.
- Status no Jira em 18/08/2026: Em desenvolvimento.

## 2. Objetivo e valor entregue

Criar a fundação relacional da Efficientia API usando Spring Data JPA, PostgreSQL e Flyway. Ao final, um banco vazio deve receber o schema versionado da API por migration reproduzível, sem credenciais gravadas no repositório.

O valor demonstrável é a base persistente da API REST Principal para documentos,
assinaturas e futuras operações CRUD, mantendo o binário de PDF/PNG fora do
PostgreSQL. Mobile e React consumirão esse contrato por HTTP; MongoDB e Redis
não fazem parte desta API e permanecem exclusivos da AI API.

## 3. Estado atual do código

- O projeto compila com Java 17 configurado no `pom.xml`, mas a verificação local foi executada com Java 26.0.1; a CI usa Java 17.
- O código produtivo contém a entidade/repository de `relatorio_viagem` e uma
  leitura paginada de prova pela API REST; upload, criação e edição continuam
  fora desta fatia.
- O teste existente é somente `contextLoads()`.
- O perfil `test` desabilita DataSource, JPA e Flyway; portanto, o teste atual não valida conexão, schema ou migration.
- O `pom.xml` já contém Spring Data JPA, driver PostgreSQL, Flyway e o módulo PostgreSQL do Flyway.
- A documentação existente é `docs/SPEC_INITIAL-STRUCTURE.md` e define a tabela `documento`, constraints, índices e a decisão de acesso direto via JPA.
- `src/main/resources/application-local.properties` está modificado localmente e rastreado pelo Git. A inspeção encontrou referências a variáveis de ambiente, sem credencial literal; a alteração deve ser preservada.

## 4. Escopo incluído

- Configuração de conexão PostgreSQL por variáveis de ambiente.
- Configuração do Flyway para migrations versionadas.
- Primeira migration da tabela `documento` conforme o modelo relacional documentado.
- UUID, viagem, assinante, modalidade, texto de assinatura, metadados do arquivo, idempotência, datas e versão.
- Constraints de nulidade, exclusividade entre assinatura textual e conteúdo binário e valores válidos do domínio.
- Índices necessários para filtros, paginação e cursor determinístico.
- Entidade JPA, repository e serviço de leitura/criação de `relatorio_viagem`,
  para comprovar a persistência desta sprint por HTTP.
- Teste de integração contra PostgreSQL de teste ou Testcontainers, sem depender da configuração local real.

## 5. Fora do escopo

- Upload multipart, LocalStorageService e streaming de arquivos — [EFFICIENTI-221](https://efficientia-team-inter-2026.atlassian.net/browse/EFFICIENTI-221) e [EFFICIENTI-222](https://efficientia-team-inter-2026.atlassian.net/browse/EFFICIENTI-222).
- CRUD público, controllers e contratos HTTP completos — [EFFICIENTI-96](https://efficientia-team-inter-2026.atlassian.net/browse/EFFICIENTI-96).
- Autenticação, autorização e CORS — [EFFICIENTI-98](https://efficientia-team-inter-2026.atlassian.net/browse/EFFICIENTI-98) e [EFFICIENTI-228](https://efficientia-team-inter-2026.atlassian.net/browse/EFFICIENTI-228).
- Exportação ZIP, AI API, MongoDB, Redis e RAG.
- Implementação da API de conexão com banco, que fica para uma etapa posterior.
- Alteração automática de `application-local.properties`.

## 6. Requisitos funcionais

1. Ao iniciar com um banco vazio, o Flyway deve aplicar a migration sem erro.
2. A estrutura deve suportar documentos dos tipos `RELATORIO_VIAGEM`, `BOLETIM_EMBARQUE`, `BOLETIM_DESEMBARQUE` e `ASSINATURA`.
3. A estrutura deve suportar modalidades `FOTO`, `DESENHO` e `TEXTO`.
4. Assinatura textual deve armazenar texto de até 150 caracteres e não exigir arquivo.
5. Documento binário deve armazenar metadados e referência privada, sem Base64 ou `bytea` na primeira entrega.
6. `idempotency_key` deve ser único.
7. A ordenação futura por cursor deve ser determinística por `criado_em` e `id`.
8. Nesta sprint, a API REST Principal acessa o PostgreSQL diretamente via Spring Data JPA.
9. A API REST Principal não deve criar conexão com MongoDB ou Redis; esses recursos pertencem à AI API.
10. Mobile e React não acessam o banco diretamente; usam a API REST por HTTP.

## 7. Requisitos não funcionais

- Migration determinística, versionada e reaplicável em ambiente limpo.
- Credenciais fornecidas por ambiente ou mecanismo equivalente, nunca por valor literal versionado.
- Constraints e índices definidos no banco, não apenas em validações Java.
- Compatibilidade com Java 17, conforme o `pom.xml` e a CI.
- Testes independentes da senha, URL e disponibilidade do banco local do desenvolvedor.

### 7.1 Supabase como PostgreSQL da API REST

O PostgreSQL desta sprint será hospedado no Supabase e acessado pela API REST
Principal por JDBC. A aplicação deve usar a string JDBC do **Session pooler**
na porta `5432`, com SSL obrigatório (`sslmode=require`). A porta `6543` do
Transaction pooler não será usada pelo JPA/Hibernate nesta etapa.

As credenciais ficam somente no ambiente local ou no ambiente de execução:

```text
SUPABASE_DB_URL=jdbc:postgresql://<host-do-session-pooler>:5432/postgres?sslmode=require
SUPABASE_DB_USERNAME=postgres.<project-ref>
SUPABASE_DB_PASSWORD=<senha-do-banco>
DB_SCHEMA=public
```

O projeto também mantém fallback para `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD`, preservando a
configuração anterior. A senha não deve aparecer em arquivos `.properties`,
commits, migrations ou logs.

## 8. Decisões de arquitetura

- PostgreSQL guarda metadados, texto de assinatura e auditoria; PDF/PNG ficam em storage privado.
- A Efficientia API é dona dos documentos, atende Mobile/React por HTTP e acessa PostgreSQL diretamente com Spring Data JPA nesta fase.
- MongoDB e Redis não são dependências da API REST Principal; pertencem exclusivamente à AI API.
- A API de conexão com banco é uma etapa posterior e não altera o fluxo desta sprint.
- Flyway é a fonte de verdade das alterações de schema; não usar `ddl-auto=create` para criar estrutura de produção.
- A entidade pode usar UUID como identificador e versionamento otimista para suportar as próximas sprints.
- O nome físico original não deve ser usado como chave de storage nem como identificador primário.

## 9. Classes, pacotes e arquivos previstos

```text
src/main/java/com/example/efficientia/
├── persistence/
│   ├── DocumentoEntity.java
│   └── DocumentoRepository.java
└── domain/
    ├── TipoDocumento.java
    ├── ModalidadeAssinatura.java
    ├── OrigemDocumento.java
    └── PapelAssinante.java

src/main/resources/
├── application.properties
└── db/migration/V1__create_documento.sql

src/test/java/com/example/efficientia/
└── persistence/DocumentoRepositoryTest.java
```

Os nomes acima seguem a estrutura prevista em `docs/SPEC_INITIAL-STRUCTURE.md`; só devem ser criados quando necessários para cumprir os critérios desta sprint.

## 10. Modelo de dados e migration

A primeira migration deve refletir o modelo documentado:

| Campo | Regra principal |
| --- | --- |
| `id` | UUID, chave primária |
| `viagem_id` | UUID, obrigatório |
| `tipo_documento` | valor de domínio, obrigatório |
| `origem` | valor de domínio, obrigatório |
| `assinante_id` | UUID, opcional |
| `papel_assinante` | valor de domínio, opcional |
| `modalidade_assinatura` | valor de domínio, opcional |
| `texto_assinatura` | até 150 caracteres, opcional |
| `descricao` | até 300 caracteres, opcional |
| `nome_original` | até 255 caracteres, opcional |
| `mime_type` | PDF/PNG quando houver arquivo |
| `tamanho_bytes` | obrigatório quando houver arquivo |
| `sha256` | hash do binário, quando houver arquivo |
| `storage_key` | único e opcional; nulo para texto |
| `idempotency_key` | UUID único, obrigatório |
| `criado_por` | UUID, opcional |
| `criado_em` / `atualizado_em` | timestamp obrigatório |
| `versao` | obrigatório, para controle de versão |

Índices mínimos: `(viagem_id, criado_em desc)`, `(assinante_id, criado_em desc)`, `(criado_em, id)`, `sha256` e chave única de idempotência.

## 11. Endpoints e contratos HTTP

O CRUD público completo continua fora desta sprint. Como prova vertical de
persistência, foram implementados os endpoints de leitura e criação:

- `POST /api/v1/usuarios`;
- `POST /api/v1/enderecos`;
- `POST /api/v1/fazendas`;
- `POST /api/v1/veiculos/cavalos`;
- `POST /api/v1/veiculos/carretas`;
- `POST /api/v1/relatorios-viagem`;
- `GET /api/v1/relatorios-viagem?pagina=0&tamanho=20`;
- `GET /api/v1/relatorios-viagem/{id}`.

Os cadastros base retornam os identificadores usados pelas chaves estrangeiras
do relatório de viagem. Senhas recebidas em `POST /api/v1/usuarios` são
persistidas exclusivamente como hash BCrypt e nunca aparecem na resposta HTTP.

A migration também prepara os dados consumidos posteriormente por:

- `POST /api/v1/documentos`;
- `GET /api/v1/documentos`;
- `GET /api/v1/documentos/{documentoId}`;
- `GET /api/v1/documentos/{documentoId}/conteudo`.

## 12. Validações e regras de negócio

- `TEXTO`: `texto_assinatura` preenchido; campos de arquivo nulos.
- `FOTO` ou `DESENHO`: texto de assinatura nulo; metadados do arquivo presentes.
- `storage_key` nulo para assinatura textual.
- Valores de domínio inválidos devem ser rejeitados por constraint ou enum coerente.
- `idempotency_key` duplicado deve ser rejeitado pelo banco.

## 13. Tratamento de erros

Esta sprint não define respostas HTTP. Falhas de migration, schema ou conexão devem produzir falha clara no build/teste, sem expor credenciais nos logs ou nos artefatos versionados.

## 14. Segurança e credenciais

- Não ler, copiar ou registrar valores de `application-local.properties`.
- Preservar a alteração local existente.
- Manter URL, usuário e senha como referências de ambiente.
- Garantir que arquivos reais, senhas e tokens não entrem na migration, em fixtures ou no Git.
- Manter `target/` ignorado e nenhum dado de teste persistente no repositório.

## 15. Estratégia de testes

1. Teste de migration em banco PostgreSQL vazio.
2. Teste de schema/constraints para assinatura textual e conteúdo binário.
3. Teste de unicidade de `idempotency_key`.
4. Teste dos índices e da ordenação determinística usada pelo cursor.
5. Teste de persistência e leitura via `DocumentoRepository`.
6. Execução de `mvn verify` com Java 17 na CI.

O `contextLoads()` existente continua sendo smoke test, mas não substitui o teste real de PostgreSQL/Flyway porque o perfil `test` atual desabilita DataSource, JPA e Flyway.

## 16. Critérios de aceite verificáveis

- [ ] Flyway cria o schema em banco vazio.
- [ ] A migration passa em um teste de integração.
- [ ] O modelo suporta FOTO, DESENHO e TEXTO.
- [ ] A constraint impede texto e conteúdo binário simultaneamente.
- [ ] `idempotency_key` é único.
- [ ] Índices e ordenação por `criado_em, id` estão presentes.
- [ ] Nenhuma senha aparece no repositório, migration ou fixture.
- [ ] A API conecta ao Supabase usando Session pooler, SSL e porta 5432.
- [ ] A conexão é fornecida por variáveis de ambiente e não pelo frontend.
- [ ] `mvn verify` passa com o ambiente de CI Java 17.
- [ ] `application-local.properties` permanece sem alteração feita por esta sprint.

## 17. Dependências e riscos

### Dependências

- [EFFICIENTI-218](https://efficientia-team-inter-2026.atlassian.net/browse/EFFICIENTI-218) — fundação do projeto concluída.
- [EFFICIENTI-6](https://efficientia-team-inter-2026.atlassian.net/browse/EFFICIENTI-6) — modelo relacional BD1, atualmente A fazer.
- EFFICIENTI-220 depende da base criada aqui.
- EFFICIENTI-222, EFFICIENTI-224 e EFFICIENTI-226 consumirão esta persistência.

### Riscos

- A sprint está atrasada em relação ao timebox 10–14/08/2026.
- O runtime local é Java 26, enquanto o contrato do projeto é Java 17.
- O teste atual não executa contra PostgreSQL/Flyway.
- A configuração local está rastreada pelo Git; apesar de usar referências de ambiente na inspeção, o arquivo deve deixar de ser tratado como configuração pessoal versionável.
- O prazo de 31/08/2026 não comporta o escopo completo das 15 sprints planejadas até 13/11/2026; até essa data, o objetivo realista é uma fatia vertical demonstrável, não a API completa.

## 18. Sequência de implementação

1. Confirmar a versão Java usada localmente e manter o código compatível com Java 17.
2. Criar a migration vazia e validar o mecanismo Flyway em banco de teste.
3. Adicionar a tabela `documento`, constraints e índices.
4. Criar enums/entidade/repository mínimos.
5. Escrever testes de schema, unicidade e persistência.
6. Executar `mvn verify` e revisar o diff sem incluir `application-local.properties`.
7. Só depois iniciar EFFICIENTI-220 — domínio e persistência de documentos.

## 19. Checklist de conclusão

- [ ] Migration nomeada e versionada.
- [ ] Schema validado em banco limpo.
- [ ] Constraints e índices testados.
- [ ] Entidade/repository coerentes com a migration.
- [ ] Teste de integração reproduzível.
- [ ] Nenhuma credencial ou documento real no Git.
- [ ] CI executa `verify` com Java 17.
- [ ] Jira atualizado somente após revisão e evidência dos testes.

## 20. Rastreabilidade

- [EFFICIENTI-17 — DS2](https://efficientia-team-inter-2026.atlassian.net/browse/EFFICIENTI-17)
- [EFFICIENTI-94 — API REST Principal](https://efficientia-team-inter-2026.atlassian.net/browse/EFFICIENTI-94)
- [EFFICIENTI-95 — Persistência PostgreSQL](https://efficientia-team-inter-2026.atlassian.net/browse/EFFICIENTI-95)
- [EFFICIENTI-219 — PostgreSQL e migrações Flyway](https://efficientia-team-inter-2026.atlassian.net/browse/EFFICIENTI-219)
- [EFFICIENTI-220 — Domínio e persistência de documentos](https://efficientia-team-inter-2026.atlassian.net/browse/EFFICIENTI-220)
- [EFFICIENTI-6 — BD1](https://efficientia-team-inter-2026.atlassian.net/browse/EFFICIENTI-6)
- Documento local de referência: `docs/SPEC_INITIAL-STRUCTURE.md`.
