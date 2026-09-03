# EFFICIENTI-229 — Sprint 12: exportação assíncrona

## Objetivo

Implementar a submissão e o rastreamento da exportação assíncrona de múltiplos documentos, estabelecendo o modelo de persistência no banco de dados, o controle rigoroso de recursos da API, segurança e estabilidade do comportamento de idempotência.

## Persistência e Banco de Dados (V4)

O registro do pedido de exportação segue o modelo V4 de banco de dados, fazendo uso de constrições (constraints) estritas de tabela e chaves ordenadas (ordered IDs). 

- Os IDs ordenados evitam a fragmentação de índices e otimizam a localização.
- A persistência inicial atende exclusivamente o estado `NA_FILA`, delegando etapas posteriores de geração e erro aos estágios de processamento em background futuros.

## Regras de Negócio e Limites

Para manter a estabilidade da aplicação, o fluxo de enfileiramento aloca de modo seguro a requisição e atende aos seguintes critérios preventivos:

- O tamanho agregado de origem aceito (soma dos tamanhos dos documentos solicitados) respeita o limite inclusivo de 500 MiB.
- Uma submissão pode requerer de 1 a 500 arquivos binários únicos simultaneamente.
- É mantida a integração dos papéis (roles) de segurança. As rotas validam autorização, isolamento de dados e verificação integral da propriedade (ownership) e acesso aos registros de origem listados para evitar extração indevida de dados.

## Contrato HTTP e Erros

A API opera exclusivamente em contrato assíncrono não-bloqueante para a criação.
- **POST (Criação):** Retorna `202 Accepted` de forma imediata quando aprovado, fornecendo os cabeçalhos de resposta `Location` (indicando onde obter status) e `Retry-After` (estimativa até a primeira checagem). O corpo da resposta expõe estritamente campos seguros.
- **GET (Status):** Rota separada para acompanhamento de situação via URL distribuída no `Location`.

O ecossistema adota tratamento de erros padronizado e limpo, emitindo:
- `404 Not Found` se os documentos requisitados não existirem ou não forem passíveis de acesso;
- `409 Conflict` sob quebra de consistência de estado;
- `422 Unprocessable Entity` quando limites nominais (extensão de 500 arquivos, cota de 500 MiB) não forem atendidos.

## Estabilidade de Idempotência

O ciclo de vida da API assegura repetição segura (idempotência) para recuperação temporal e para concorrência de rede (Sprint 12 fix):

- **Fluxo Sequencial:** A retransmissão tardia para a mesma chave devolve os mesmos cabeçalhos do pedido original sem gerar duplicidade.
- **Fluxo Concorrente:** A API abandona o fluxo read-modify-write isolado anterior (find-then-save não atômico). Em colisão paralela de inserção da mesma chave, um pedido assume a propriedade (winner) e a verificação de propriedade (ownership) é plenamente garantida. O pedido alternativo em concorrência reflete pacificamente a resposta idempotente, eliminando o erro fatal de unicidade não capturado na camada de banco (uncaught unique-key error).

## Critérios de aceite atendidos

| Critério | Evidência |
| --- | --- |
| Retorno HTTP 202 com cabeçalhos corretos | teste validando emissão de status 202, `Location` e `Retry-After` |
| Restrição entre 1..500 itens únicos | testes validando violações `422 Unprocessable Entity` nas fronteiras |
| Soma limite em 500 MiB restrita | recusa formal com `422 Unprocessable Entity` sob tamanho excedente |
| Integridade de idempotência simultânea | correção da concorrência find-then-save validada em `ExportacaoServiceTest` |
| Retenção da segurança de propriedade | testes validando escopo de owner estritamente aplicado ao replay da idempotência |
| Tabela V4 com Ordered ID | `ExportacaoPersistenceService` criado e estruturado sobre chave ordenada |

## Fronteira para o Sprint 13

Todo o processo computacional de arquivamento (worker) e entrega por stream (ZIP) se encontra deliberadamente fora do escopo desta entrega. A implementação restringe-se ao enfileiramento (`NA_FILA`), contrato web, restrições e idempotência seguras. O processamento em background real e a respectiva baixa ou notificação encerram a fronteira de início exclusiva do **Sprint 13**.
