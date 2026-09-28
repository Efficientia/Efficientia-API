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
- **Testes Unitários e de Integração:** 220 testes executados e aprovados (BUILD SUCCESS).
- **Métricas Finais (JaCoCo):** Todas as violações resolvidas. Cobertura superou 80% em Linhas e 60% em Ramificações (`[INFO] All coverage checks have been met`).
- **Estado de Commit Local:** Salvo com sucesso na ramificação local (`fix/health-check-test-update`). Preparado para a esteira de Pull Request (devido a regra `GH013`).
