# EFFICIENTI-227 — Sprint 10: validações, erros e OpenAPI

## Objetivo

Fechar o contrato público observável da API principal, rejeitando arquivos
incompatíveis antes da persistência e devolvendo falhas estáveis para React,
mobile e AI API.

## Validação de arquivo

O MIME é detectado pelos magic bytes e precisa coincidir com `Content-Type` e
extensão. Arquivos vazios são rejeitados. Antes do streaming para o storage,
também são aplicados os limites fixos:

- PDF: 25 MiB;
- PNG: 10 MiB.

O storage repete a proteção durante a cópia, evitando depender apenas do
tamanho declarado pelo multipart.

## Problem Details

Falhas dos endpoints de documentos usam `application/problem+json` e incluem:

- `code` estável para tratamento no cliente;
- `timestamp` em UTC;
- `correlationId`, também devolvido em `X-Correlation-Id`;
- `title`, `status` e `detail` seguros;
- `erros` com campo e mensagem quando a validação Bean Validation falha.

Headers de correlação fornecidos pelo cliente só são aceitos com caracteres
seguros e até 64 posições. Stack trace, caminho físico e mensagem interna do
storage não são expostos.

## Status cobertos

| Status | Uso |
| --- | --- |
| 400 | JSON, header, filtro, cursor ou campo inválido |
| 404 | documento inexistente |
| 409 | versão concorrente ou documento textual sem binário |
| 413 | PDF/PNG acima do limite |
| 415 | MIME real, header ou extensão incompatível |
| 422 | regra de negócio de assinatura violada |
| 503 | storage indisponível |

Campos JSON desconhecidos são rejeitados; por isso um PATCH não consegue
silenciosamente enviar `storageKey`, hash ou MIME.

## OpenAPI

O Swagger UI permanece disponível em `/swagger-ui.html` e o contrato JSON em
`/v3/api-docs`. A tag `Documentos` descreve os sete fluxos públicos, incluindo
respostas de sucesso e falha, limites e comportamento idempotente. DTOs e enums
são derivados dos records Java; `ApiProblem` documenta as propriedades extras
do padrão de erro.

## Critérios de aceite atendidos

| Critério | Evidência |
| --- | --- |
| Vazio, grande ou MIME falso rejeitado | validador por bytes, tamanho e storage |
| Erros úteis sem stack trace | advice central e detalhes seguros |
| Endpoints documentados | operações e respostas OpenAPI no controller |
| Swagger UI abre | starter springdoc e bean OpenAPI |
| 400/404/409/413/415/422/503 | testes MockMvc acumulados |

## Testes

Há testes para magic bytes, limite PNG, campos desconhecidos, correlação,
`ProblemDetail` e todos os status exigidos, além da suíte funcional anterior.
