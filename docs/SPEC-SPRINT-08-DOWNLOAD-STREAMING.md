# EFFICIENTI-225 — Sprint 08: download e visualização por streaming

## Objetivo

Disponibilizar o conteúdo binário autorizado ao React, mobile e Efficientia AI
API sem expor a chave ou o caminho do storage privado.

## Endpoint

```http
GET /api/v1/documentos/{id}/conteudo?inline=false
```

O modo padrão usa `Content-Disposition: attachment`. Com `inline=true`, o
cliente pode pré-visualizar PDF ou PNG suportado pelo navegador.

## Streaming e headers

O service devolve um `Resource` aberto pelo `StorageService`; controller e
Spring MVC transmitem o conteúdo sem criar um `byte[]` integral na camada da
aplicação.

A resposta contém:

- `Content-Type` detectado e persistido pelo fluxo de upload;
- `Content-Length` do arquivo armazenado;
- `Content-Disposition` com nome original sanitizado e codificação UTF-8;
- `Cache-Control: private, no-store`.

O `storageKey` e o caminho físico permanecem internos.

## Erros observáveis

| Cenário | Status |
| --- | --- |
| UUID inexistente | `404 Not Found` |
| Assinatura textual sem arquivo | `409 Conflict` |
| Falha ao abrir o storage | erro padronizado pelo advice |

Assinaturas textuais são identificadas antes da chamada ao storage.

## Critérios de aceite atendidos

| Critério | Evidência |
| --- | --- |
| PDF e PNG sem materialização integral | contrato baseado em `Resource` |
| Pré-visualização | parâmetro `inline=true` |
| Nome original seguro | nome sanitizado no upload e header UTF-8 |
| Ausente retorna 404 | exceção de domínio específica |
| Texto sem binário retorna conflito | `DocumentoSemConteudoException` |

## Testes

Os testes cobrem streaming e headers no controller, preservação do `Resource`
no service, documento inexistente e assinatura textual sem acesso ao storage.

## Fora do escopo desta sprint

- alteração e exclusão;
- auditoria;
- autorização JWT;
- documentação OpenAPI completa.
