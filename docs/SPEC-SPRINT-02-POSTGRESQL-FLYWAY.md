# SPEC Sprint 02 — PostgreSQL e migrações Flyway

## 1. Identificação

- Projeto: EFFICIENTI.
- Épico: [EFFICIENTI-94 — API REST Principal](https://efficientia-team-inter-2026.atlassian.net/browse/EFFICIENTI-94).
- Tarefa: [EFFICIENTI-219 — PostgreSQL e migrações Flyway](https://efficientia-team-inter-2026.atlassian.net/browse/EFFICIENTI-219).
- Dependência funcional: [EFFICIENTI-95 — Persistência PostgreSQL](https://efficientia-team-inter-2026.atlassian.net/browse/EFFICIENTI-95).
- Sprint planejada: 10–14/08/2026.
- Status consolidado em 25/08/2026: concluída e incorporada à `main`.

## 2. Objetivo entregue

Criar a fundação relacional da API REST Principal com Spring Data JPA,
PostgreSQL e Flyway, conectá-la ao Supabase e entregar uma primeira fatia
vertical de cadastros e relatórios de viagem.

Esta sprint não cria a tabela de documentos. O domínio e a persistência de
documentos pertencem à
[EFFICIENTI-220](https://efficientia-team-inter-2026.atlassian.net/browse/EFFICIENTI-220)
e à migration `V2__create_documento.sql`.

## 3. Limite arquitetural

- A API REST Principal acessa o PostgreSQL diretamente via Spring Data JPA.
- Mobile e React acessam dados relacionais somente por HTTP.
- MongoDB e Redis são exclusivos da Efficientia AI API.
- Credenciais do Supabase ficam no ambiente local ou de execução.
- O frontend nunca recebe URL JDBC, usuário ou senha do banco.

## 4. Entregas

### 4.1 Conexão e configuração

O datasource aceita as seguintes variáveis:

```text
SUPABASE_DB_URL=postgresql://<host-do-session-pooler>:5432/postgres?sslmode=require
SUPABASE_DB_USERNAME=postgres.<project-ref>
SUPABASE_DB_PASSWORD=<senha-do-banco>
DB_SCHEMA=public
```

O arquivo `.env` local é ignorado pelo Git. `.env.example` contém somente
placeholders. O carregador:

- aceita valores com ou sem aspas;
- converte a URL `postgresql://` para JDBC;
- não sobrescreve variáveis já definidas no sistema ou na CI;
- mantém fallback para as variáveis padrão do Spring.

Foi escolhido o Session pooler na porta 5432 com SSL obrigatório. O Transaction
pooler da porta 6543 não faz parte desta configuração.

### 4.2 Flyway

`V1__create_relational_schema.sql` versiona o schema relacional inicial:

- usuários;
- veículos cavalo e carreta;
- endereços;
- fazendas;
- unidades frigoríficas;
- relatórios de viagem;
- paradas;
- anomalias de embarque e desembarque.

Em banco vazio, a V1 reproduz o modelo. No Supabase que já possuía essas
tabelas, o Flyway foi configurado com baseline para assumir a estrutura
existente sem recriá-la.

Configurações principais:

- migrations em `classpath:db/migration`;
- schema padrão configurável, com `public` como fallback;
- validação antes de migrar;
- baseline em versão 1 para banco preexistente;
- Hibernate em `ddl-auto=validate`.

Flyway é a fonte de verdade do schema. Migrations já aplicadas não devem ser
editadas.

### 4.3 Persistência relacional

Foram implementadas entidades e repositories para:

- `UsuarioEntity`;
- `EnderecoEntity`;
- `FazendaEntity`;
- `VeiculoCavaloEntity`;
- `VeiculoCarretaEntity`;
- `RelatorioViagemEntity`.

As chaves relacionais existentes usam `SERIAL/INTEGER`. Esse padrão também
deve ser respeitado por futuras FKs para `usuario` e `relatorio_viagem`.

### 4.4 Endpoints entregues

- `GET /api/v1/status`;
- `POST /api/v1/usuarios`;
- `POST /api/v1/enderecos`;
- `POST /api/v1/fazendas`;
- `POST /api/v1/veiculos/cavalos`;
- `POST /api/v1/veiculos/carretas`;
- `POST /api/v1/relatorios-viagem`;
- `GET /api/v1/relatorios-viagem?pagina=0&tamanho=20`;
- `GET /api/v1/relatorios-viagem/{id}`.

Senhas recebidas pelo endpoint de usuários são persistidas somente como hash
BCrypt e nunca retornam no contrato HTTP.

## 5. Requisitos não funcionais

- Compatibilidade de compilação com Java 17.
- Credenciais nunca versionadas.
- Falha explícita quando conexão, migration ou validação de schema falhar.
- Timestamps tratados em UTC pela camada JDBC.
- `open-in-view` desabilitado.
- Pool de conexões configurável por ambiente.
- Testes locais independentes da senha e da disponibilidade do Supabase.

## 6. Testes

A entrega inclui testes para:

- carregamento e normalização do `.env`;
- status da API;
- contratos de cadastro base;
- regras do serviço de cadastro;
- endpoints de relatório de viagem;
- regras do serviço de relatório;
- inicialização do contexto no perfil de teste.

O perfil `test` não conecta ao Supabase. A validação integrada foi realizada
separadamente com a aplicação local e o schema real.

## 7. Evidências de aceite

- [x] A aplicação conecta ao Session pooler do Supabase com SSL.
- [x] O Flyway possui histórico no schema `public`.
- [x] A V1 relacional está versionada e validada.
- [x] O Hibernate valida o schema sem criar tabelas automaticamente.
- [x] Cadastros base podem ser inseridos pela API.
- [x] Relatórios de viagem podem ser criados e consultados.
- [x] A listagem paginada responde HTTP 200.
- [x] Nenhuma credencial está presente nos arquivos versionados.
- [x] MongoDB e Redis não são dependências da API REST Principal.

## 8. Fora do escopo

- tabela, entidade e repository de documentos;
- storage privado;
- upload multipart e streaming;
- autenticação e autorização;
- integração com a Efficientia AI API;
- MongoDB, Redis e RAG;
- aplicação Mobile e site React.

## 9. Continuidade

A próxima entrega é a
[SPEC Sprint 03 — domínio e persistência de documentos](./SPEC-SPRINT-03-DOMINIO-DOCUMENTOS.md),
que adiciona a migration V2 sem modificar a V1 já aplicada.

## 10. Rastreabilidade

- [EFFICIENTI-17 — DS2](https://efficientia-team-inter-2026.atlassian.net/browse/EFFICIENTI-17)
- [EFFICIENTI-94 — API REST Principal](https://efficientia-team-inter-2026.atlassian.net/browse/EFFICIENTI-94)
- [EFFICIENTI-95 — Persistência PostgreSQL](https://efficientia-team-inter-2026.atlassian.net/browse/EFFICIENTI-95)
- [EFFICIENTI-219 — PostgreSQL e migrações Flyway](https://efficientia-team-inter-2026.atlassian.net/browse/EFFICIENTI-219)
- [EFFICIENTI-220 — Domínio e persistência de documentos](https://efficientia-team-inter-2026.atlassian.net/browse/EFFICIENTI-220)
- [SPEC de arquitetura principal](./SPEC_INITIAL-STRUCTURE.md)
