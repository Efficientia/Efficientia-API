# FIX-TESTES-COBERTURA-JACOCO-RENDER

## Visão Geral
Este documento registra a resolução dos impedimentos de *deploy* na infraestrutura do Render, os acertos de configuração no Spring Boot/Actuator, e as massivas ações de testes desenvolvidas para atingir os rigorosos critérios de Qualidade e Integração Contínua (CI) no JaCoCo (80% Linhas, 60% Ramificações).

---

## Detalhamento das Ações Realizadas

### 1. Bloqueios de Deploy no Render (Health Check Actuator)
- **Problema:** Durante o *deploy* efêmero do Render, a aplicação não encontrava o diretório de arquivos (`./data/documentos`), fazendo com que a rota do Actuator retornasse falha de disco. O Render assumia que a inicialização (boot) havia falhado e encerrava o contêiner imediatamente.
- **Correção:** 
  - Alterado o componente `StorageHealthIndicator` para forçar a criação recursiva do diretório `./data/documentos` através do `Files.createDirectories()` logo na primeira execução do ping de *health check*.

### 2. Rejeição do Audience em Autenticações (401 Unauthorized)
- **Problema:** Ao gerar os tokens de acesso via `JwtTokenService`, a falta do *claim* `aud` (Audience) fazia com que o `AudienceValidator` rejeitasse os próprios tokens emitidos pelo sistema, cortando o acesso corporativo.
- **Correção:** 
  - Inserido o claim `.audience(List.of("efficientia-api"))` no builder de geração do token.

### 3. Falha de Migração do Flyway (Supabase)
- **Problema:** Migrações conflitantes e um estado corrompido na tabela `flyway_schema_history` impediam a conexão do banco de dados na inicialização do serviço.
- **Correção:** 
  - A tabela histórica do Flyway e as instâncias de domínios problemáticos (ex. `relatorio_viagem`) foram limpas no Supabase. O script unificado de *schema* foi restaurado para o seu formato normal `V1__create_relational_schema.sql` para ser reconstruído do zero.

#### Falha de migração reportada no Render em 06/10/2026
- **Problema:** A migração `V9__adapta_banco_e_processo_assinaturas.sql` inicialmente encontrou o erro PostgreSQL `42809: "tb_usuario" is not a view`. O banco já possuía uma tabela física `sc_corporativo.tb_usuario`, e `CREATE OR REPLACE VIEW` não pode substituir uma tabela.
- **Correção:** A criação das views de compatibilidade verifica o tipo da relação de destino. Cria a view quando o nome está livre, atualiza quando já existe uma view e preserva tabelas ou outros tipos de relação existentes.
- **Resultado no log seguinte:** A V9 foi validada e aplicada com sucesso, levando o schema à versão 9. A aplicação falhou depois, ao validar JPA.

#### Falha de validação JPA e robustez do build
- **Problema:** `assinatura_motorista.sha256` é `CHAR(64)` no PostgreSQL, enquanto a entidade esperava `VARCHAR(64)`. A validação seguinte também encontraria incompatibilidade entre os enums PostgreSQL das ocorrências e os campos `String` das entidades.
- **Correção:** O campo SHA-256 agora usa o mapeamento Hibernate `CHAR`. A migration V10 converte os três campos de motivo/anomalia para `VARCHAR(50)` e mantém os códigos aceitos por constraints `CHECK`, sem editar migrations já aplicadas.
- **Flyway:** Removido o `repair()` automático em toda inicialização; divergências de checksum agora são rejeitadas pela validação normal do Flyway. O baseline configurado como versão 1 faz bancos não vazios sem histórico ignorarem a V1, de acordo com o comentário da própria migration.
- **Build de imagem:** O Dockerfile usa Java 17 no builder e no runtime (`eclipse-temurin:17-jre-jammy`) para corresponder ao `pom.xml`; a tag Chainguard `openjdk-17` não está publicada. A etapa de empacotamento usa `-DskipTests`, pois os testes são executados pelo workflow Maven; o `.dockerignore` exclui credenciais e artefatos locais do contexto.
- **CI:** Os três workflows foram separados por responsabilidade: `maven-verify.yml` executa `clean verify` e os limites JaCoCo; `ci.yml` executa a integração com PostgreSQL/Testcontainers e falha se o teste for ignorado; `container-build.yml` valida a construção da imagem.
- **Validação automatizada:** `PostgresIntegrationTest` ativa explicitamente `ddl-auto=validate`, de modo que a inicialização contra PostgreSQL verifique os tipos do schema. Os workflows com Testcontainers verificam o daemon Docker antes de iniciar e a migração V11 é exercitada com os nomes finais dos schemas.

### 4. Bateria de Testes: Adequação das Restrições do JaCoCo (80%/60%)
- **Problema:** O código estava engessado no CI/CD com métricas de 73% de linhas (mínimo 80%) e 53% de ramificações (mínimo 60%). Era preciso blindar componentes essenciais.
- **Correção (Testes Escritos/Atualizados):**
  - **JwtTokenServiceTest:** Cobertura de 100% dos fluxos de emissão (Sucesso e Catch Parsing de exceptions).
  - **EmpresaAdminServiceTest:** Desenvolvidos testes robustos de caminhos de erro/falhas e divergências de credenciais. Um *mock* completo do `UsuarioRepository` foi inserido para alcançar dezenas de `if/else` (branches) isolados dentro das rotinas de cadastro, validação e sincronização de administradores e funcionários.
  - **EmpresaServiceTest:** Mapeamento e teste para ausências de informações críticas em `cadastrarEmpresa` (*NullPointers*, senhas corporativas incorretas, campos duplicados ou identificadores inválidos).
  - **CadastroBaseServiceTest:** Implementados os 10 testes faltantes da camada Base (Cavalos, Carretas e Fazendas) atingindo fluxos de duplicidade de chaves únicas e negações por idade (menores de 18 anos).
  - **InMemoryEmpresaAdminRepositoryTest:** Blindagem de operações de persistência em memória.
  - **POJOs (Entidades):** Validada construção base, *getters* e *setters* das entidades `Empresa` e `EmpresaAdmin`, recuperando ~60 linhas opacas para a contagem do JaCoCo.

### 5. Correção de Defeito Crítico Oculto
- **Problema:** A API retornava uma "Tela Branca" (NullPointerException) interna caso a solicitação de Login Corporativo não enviasse nenhum valor (nulo) na chave de autenticação (ex: CNPJ).
- **Correção:** Adicionada cláusula *null-check* no tratador do campo `identificador` (`request.cnpj() != null ? request.cnpj().trim() : ""`) dentro de `EmpresaService.autenticarEmpresa()`.

---

## Status da Validação
- **Testes Unitários e de Integração:** A validação local executou 499 testes, com zero falhas e erros; o teste PostgreSQL foi ignorado porque Docker não está disponível localmente. O CI dedicado exige que esse teste rode e falha se ele for ignorado.
- **Métricas Finais (JaCoCo):** A validação local alcançou 93,1% de linhas e 80,9% de ramificações, acima dos limites de 80% e 60% (`[INFO] All coverage checks have been met`).
- **Estado de Commit Local:** Salvo com sucesso na ramificação local (`fix/health-check-test-update`). Preparado para a esteira de Pull Request (devido a regra `GH013`).
