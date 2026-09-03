# Especificação de Implementação — Sprint 14 (EFFICIENTI-231)

## Objetivos e Escopo
A Sprint 14 foca no fortalecimento da observabilidade operacional, estabilização do cursor de busca para inteligência artificial, estruturação de logs distribuídos com MDC, publicação de métricas via Micrometer e verificações de integridade (health checks) das dependências críticas da API REST.

---

## Componentes Desenvolvidos

### 1. Estabilização do Cursor IA (`DocumentoQueryRepositoryImpl` / `DocumentoService`)
- Por padrão, a navegação via cursor (`cursor != null` ou `cursor=INICIO`) exclui documentos do tipo `ASSINATURA` da listagem.
- Permite requisições explícitas por assinaturas quando o filtro `tipoDocumento=ASSINATURA` for informado na consulta.
- Previne poluição de contexto de IA com assinaturas textuais puras durante varreduras paginadas por cursor.

### 2. Verification Indicator de Saúde do Storage (`StorageHealthIndicator`)
- Implementação de `HealthIndicator` (`org.springframework.boot.health.contributor.HealthIndicator`) registrado sob o identificador `privateStorage`.
- Verifica se o diretório do storage privado configurado existe, se é um diretório válido e se a aplicação possui permissões de escrita.
- Retorna estado `UP` ou `DOWN` com detalhes estruturados no endpoint `/actuator/health`.

### 3. Log Estruturado e Rastreabilidade Distribuída (`CorrelationIdFilter`)
- Integração do `X-Correlation-Id` ao Logback MDC (`org.slf4j.MDC`).
- População automática em cada requisição HTTP e limpeza garantida no bloco `finally`.
- Propagação do ID de correlação para todo o ciclo de vida do log na thread de atendimento.

### 4. Coleta de Métricas Operacionais (`EfficientiaMetricsService`)
- Serviço Micrometer que publica métricas chave:
  - `exportacoes.solicitadas.total`: Contador de requisições de exportação.
  - `exportacoes.processadas.total` (tags `status=CONCLUIDA|FALHA`): Contador de conclusões e erros do worker.
  - `exportacoes.processamento.tempo`: Temporizador de duração do processamento do ZIP em segundo plano.
  - `documentos.criados.total`: Contador de novos documentos armazenados.
  - `documentos.tamanho.bytes`: Resumo de distribuição de tamanho de arquivos em bytes.

### 5. Configuração e Exposição do Actuator (`application.properties` e `SecurityConfig`)
- `management.endpoints.web.exposure.include=health,info,metrics,prometheus`
- `management.endpoint.health.show-details=always`
- Regras de segurança em `SecurityConfig` liberando `/actuator/health/**`, `/actuator/info`, `/actuator/metrics/**` e `/actuator/prometheus`.

---

## Testes Automatizados e Evidências
- `StorageHealthIndicatorTest`: Cobertura unitária de status `UP` e `DOWN` para cenários com diretório válido ou inexistente.
- `EfficientiaApplicationTests`: Teste de inicialização completa do contexto Spring Boot `@SpringBootTest` validando a ausência de conflitos de injeção de dependência e integração do Actuator.
- Suíte completa de 128 testes passando integralmente sem falhas.
