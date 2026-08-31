# EFFICIENTI-222 — Sprint 05: upload multipart de PDF e PNG

## Objetivo

Entregar o primeiro fluxo ponta a ponta de documentos da API REST principal:
receber PDF ou PNG por multipart, validar o conteúdo real, salvar o binário no
storage privado e persistir seus metadados no PostgreSQL.

## Contrato HTTP

`POST /api/v1/documentos` recebe `multipart/form-data` com:

| Parte | Tipo | Obrigatória |
| --- | --- | --- |
| `metadados` | `application/json` com `DocumentoMetadataRequest` | Sim |
| `arquivo` | `application/pdf` ou `image/png` | Sim |
| header `Idempotency-Key` | UUID | Sim |

O controller valida o contrato, delega ao `DocumentoService` e responde
`201 Created` com `Location: /api/v1/documentos/{id}`.

## Validação do arquivo

`ArquivoValidator` abre o upload como stream e inspeciona somente o cabeçalho:

- PDF precisa começar com `%PDF-`;
- PNG precisa conter a assinatura binária oficial de oito bytes;
- o `Content-Type` da parte deve corresponder aos bytes;
- a extensão precisa corresponder ao MIME detectado;
- o nome persistido é somente o basename, sem diretórios enviados pelo cliente;
- nomes vazios, muito longos ou com caracteres de controle são rejeitados.

O arquivo não é convertido para Base64 nem carregado integralmente em memória.
Depois da inspeção, o mesmo stream reposicionado é entregue ao
`StorageService`, que calcula tamanho e SHA-256 durante a gravação.

## Fluxo transacional

O serviço executa as operações nesta ordem:

1. valida metadados e `Idempotency-Key`;
2. procura um documento já associado à chave;
3. valida os magic bytes do upload;
4. grava o conteúdo no storage privado;
5. persiste os metadados com `saveAndFlush`;
6. mapeia somente os campos públicos da resposta.

Storage e PostgreSQL não compartilham uma transação ACID. Se a operação JPA
falhar após a gravação, o serviço remove a `storageKey` recém-criada. Uma falha
da compensação é anexada à exceção original sem ocultar a causa principal.

## Idempotência

Antes de ler ou salvar o arquivo, o serviço consulta `Idempotency-Key`. Uma
repetição normal retorna o documento existente e não chama o storage novamente.

A constraint única do PostgreSQL continua protegendo concorrência e integridade
da chave.

## Contrato público

`DocumentoResponse` pode expor nome, MIME, tamanho, SHA-256 e `conteudoUrl`.
Ele nunca contém:

- `storageKey`;
- caminho físico;
- `idempotencyKey`;
- Base64 ou bytes do documento.

## Limites

O limite global do multipart é configurável:

```properties
spring.servlet.multipart.max-file-size=${MAX_PDF_SIZE:25MB}
spring.servlet.multipart.max-request-size=${MAX_MULTIPART_REQUEST_SIZE:26MB}
```

O storage mantém os limites específicos de 25 MiB para PDF e 10 MiB para PNG
durante o streaming.

## Erros HTTP

- `400 Bad Request`: metadados, arquivo vazio ou nome inválido;
- `413 Payload Too Large`: limite global ou específico excedido;
- `415 Unsupported Media Type`: bytes, header ou extensão incompatíveis;
- `500 Internal Server Error`: falha técnica no storage.

As respostas usam `ProblemDetail` e não expõem caminhos ou exceções internas.

## Critérios de aceite atendidos

| Critério | Evidência |
| --- | --- |
| PDF e PNG válidos retornam 201 | controller e validator cobrem os dois tipos |
| MIME real é validado | magic bytes comparados com header e extensão |
| Resposta não contém Base64 ou storageKey | DTO público e teste de controller |
| Repetição idempotente não duplica conteúdo | service retorna entidade existente antes de validar o arquivo |
| Falha JPA remove arquivo órfão | teste força `DataIntegrityViolationException` e verifica `remover` |
| Controller apenas delega | repository e storage são dependências exclusivas do service |

## Testes

Foram adicionados 10 casos desta sprint:

- 5 invocações para magic bytes, header, extensão e sanitização do nome;
- 3 casos de service para sucesso, idempotência e compensação;
- 2 casos de controller para `201`, `Location`, contrato público e validação.

Em 31/08/2026, a suíte acumulada executou 38 testes, sem falhas ou erros.

## Fora do escopo desta sprint

- assinatura textual em JSON;
- regras completas de FOTO, DESENHO e TEXTO;
- listagem, filtros, cursor e consulta por UUID;
- download do conteúdo;
- autenticação e autorização.

Esses itens pertencem às sprints seguintes do mesmo lote.
