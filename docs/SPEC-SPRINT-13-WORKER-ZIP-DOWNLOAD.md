# Especificação de Implementação — Sprint 13 (EFFICIENTI-230)

## Objetivos e Escopo
A Sprint 13 complementa o ecossistema de exportação assíncrona introduzido na Sprint 12, fornecendo a geração em segundo plano de arquivos ZIP via worker agendado (`ExportacaoWorker`), a limpeza automática de arquivos expirados (`ExportacaoExpirationWorker`), a extensão do storage privado (`StorageService`) para suporte a ZIPs e a inclusão do endpoint REST seguro de download (`GET /api/v1/exportacoes/{id}/conteudo`).

---

## Componentes Desenvolvidos

### 1. Extensão do Storage Privado (`StorageService` / `LocalStorageService`)
- Suporte nativo ao tipo MIME `application/zip` com extensão `.zip`.
- Padrão de chave de storage atualizado para suportar diretórios e arquivos em `exportacoes/{referenciaId}/{uuid}.zip` além de `documentos/{referenciaId}/{uuid}.(pdf|png)`.
- Generalização do parâmetro do método `salvar(UUID referenciaId, ...)` mantendo compatibilidade com os tipos existentes.
- Limite configurável ampliado para ZIPs (até 550 MiB) prevenindo estouro de quota em exportações com múltiplos anexos.

### 2. Worker de Processamento Assíncrono (`ExportacaoWorker`)
- `@Scheduled(fixedDelay = 5000)` para busca ordenada da exportação mais antiga em estado `NA_FILA`.
- Transição atômica para o estado `PROCESSANDO` utilizando *optimistic locking* (`ObjectOptimisticLockingFailureException`) para prevenir concorrência em múltiplas instâncias da API.
- Streaming direto via `ZipOutputStream` sem manter todos os binários simultaneamente em memória heap.
- Persistência atômica do ZIP resultante no storage privado.
- Atualização da entidade para `CONCLUIDA` definindo `tamanho_zip_bytes`, `storage_key`, `nome_arquivo` e validade (`expira_em = criado_em + 1h`).
- Tratamento de falhas: transição para `FALHA` com limpeza de qualquer arquivo temporário gerado em caso de erro de I/O ou ausência de documentos.

### 3. Worker de Expiração de Conteúdo (`ExportacaoExpirationWorker`)
- `@Scheduled(fixedDelay = 60000)` para identificação de exportações concluídas ou falhas cuja data `expira_em` seja anterior ao momento atual (`Instant.now()`).
- Exclusão idempotente do arquivo ZIP privado via `StorageService.remover(storageKey)`.
- Transição de estado para `EXPIRADA`.

### 4. Endpoint de Download REST (`ExportacaoController`)
- `GET /api/v1/exportacoes/{id}/conteudo`
- **Validação de Autorização**: O usuário autenticado deve possuir acesso aos documentos associados à exportação (conforme política `DocumentoAccessPolicy`).
- **Respostas**:
  - `200 OK`: Stream do arquivo ZIP com cabeçalhos HTTP `Content-Type: application/zip`, `Content-Disposition: attachment; filename="..."` e `Content-Length`.
  - `409 Conflict`: Retorna ProblemDetail `EXPORTACAO_NAO_CONCLUIDA` se a exportação ainda estiver `NA_FILA` ou `PROCESSANDO`.
  - `410 Gone`: Retorna ProblemDetail `EXPORTACAO_EXPIRADA` se a exportação tiver sido expirada/removida.
  - `404 Not Found`: Retorna ProblemDetail `EXPORTACAO_NAO_ENCONTRADA`.

---

## Testes Automatizados e Evidências
- `ExportacaoWorkerTest`: Validação de transições de estado (`PROCESSANDO` -> `CONCLUIDA` / `FALHA`), otimização de bloqueio e geração correta da estrutura do ZIP.
- `ExportacaoControllerTest`: Testes de integração MVC para o endpoint de download (`GET /conteudo`), verificando códigos 200, 409 e 410, além dos cabeçalhos HTTP exigidos.
- `LocalStorageServiceTest`: Cobertura completa de armazenamento, validação de chaves e exclusão de arquivos ZIP privados.
