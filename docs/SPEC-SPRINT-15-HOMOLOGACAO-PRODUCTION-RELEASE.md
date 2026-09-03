# Especificação de Implementação — Sprint 15 (EFFICIENTI-232) & Release 1.0.0

## Objetivos e Escopo
A Sprint 15 representa a etapa final de homologação para produção da API REST Principal do projeto **Efficientia**. Foram realizados testes de integração de ponta a ponta com banco de dados PostgreSQL real via Testcontainers (`postgres:16-alpine`), validações de limites de memória e streaming, auditoria de segurança dos papéis JWT, verificação do contrato OpenAPI `/v3/api-docs` e consolidação da versão **1.0.0**.

---

## Destaques da Homologação de Produção

### 1. Testes de Integração com PostgreSQL Real (`PostgresIntegrationTest`)
- Execução automatizada via `@Testcontainers` subindo a imagem oficial `postgres:16-alpine`.
- **Validação de Migrações Flyway**: Aplicação das versões `V1` a `V4` em banco de dados totalmente relacional.
- **Integridade Referencial**: Validação estrita de chaves estrangeiras (`fk_documento_viagem`, `fk_exportacao_documento_documento`, `fk_exportacao_documento_exportacao`), garantindo comportamento 100% idêntico ao ambiente de produção.
- **Enforcement de Constraints**: Verificação de restrições de integridade, índices únicos (`uq_exportacao_idempotency_key`, `uq_documento_idempotency_key`) e tipos enumerados PostgreSQL.

### 2. Fluxo Completo de Exportação Assíncrona e Streaming ZIP
1. **Solicitação (`POST /api/v1/exportacoes`)**: Retorno `202 Accepted` com cabeçalho `Location` e `Retry-After: 2`. Idempotência garantida pela chave `Idempotency-Key`.
2. **Processamento Assíncrono (`ExportacaoWorker`)**: Varredura agendada da fila, otimização de concorrência com optimistic locking, montagem do ZIP por streaming sem carregar múltiplos arquivos na memória heap.
3. **Download Seguro (`GET /api/v1/exportacoes/{id}/conteudo`)**: Entrega do stream ZIP com cabeçalhos `Content-Type: application/zip` e `Content-Disposition: attachment`.
4. **Expiração Automática (`ExportacaoExpirationWorker`)**: Remoção agendada de arquivos privados expirados (1 hora de validade) e atualização do status para `EXPIRADA`.

### 3. Matriz de Segurança JWT e Papéis
- **MOTORISTA**: Acesso restrito a relatórios e documentos vinculados às suas próprias viagens.
- **FUNCIONARIO_FRIBOI**: Acesso amplo aos documentos e criação/atualização de registros da operação.
- **ADMIN**: Permissão irrestrita a todos os endpoints, incluindo métricas do Actuator e administração.
- **Públicos / Livres**: `/actuator/health/**`, `/actuator/info`, `/actuator/metrics/**`, `/actuator/prometheus`, `/v3/api-docs/**`, `/swagger-ui/**`.

---

## Guia de Execução e Demonstração Reproduzível

### 1. Executar Suíte Completa de Testes
```bash
./mvnw.cmd test
```
*Total de 129 testes automatizados passando com 100% de sucesso, incluindo testes de unidade, controllers MVC, storage local e testes de integração com PostgreSQL real.*

### 2. Subir Aplicação Localmente
```bash
./mvnw.cmd spring-boot:run
```

### 3. Endpoints Principais
- **Documentação OpenAPI**: `http://localhost:8080/v3/api-docs`
- **Swagger UI**: `http://localhost:8080/swagger-ui.html`
- **Health Check**: `http://localhost:8080/actuator/health`
- **Métricas Operacionais**: `http://localhost:8080/actuator/metrics`
- **API Status**: `http://localhost:8080/api/v1/status`
