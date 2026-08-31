# EFFICIENTI-226 — Sprint 09: alteração, exclusão e auditoria

## Objetivo

Completar o ciclo de vida do documento mantendo consistência entre PostgreSQL e
storage privado e preservando rastreabilidade das operações destrutivas.

## Endpoints

```http
PATCH /api/v1/documentos/{id}
DELETE /api/v1/documentos/{id}
```

O PATCH aceita somente `versao` e `descricao`. Hash, MIME, tamanho, nome,
conteúdo e `storageKey` não fazem parte do DTO público.

## Concorrência otimista

A coluna `versao`, mapeada com `@Version`, é comparada com a versão enviada.
Versão divergente ou conflito detectado pelo Hibernate retorna `409 Conflict`,
orientando o cliente a consultar o documento novamente.

## Exclusão consistente

Metadados e auditoria participam da mesma transação. Para documentos binários,
a remoção do storage ocorre em `afterCommit`: rollback do banco não apaga
antecipadamente o arquivo. Assinatura textual não chama storage.

O endpoint responde `204 No Content`.

## Auditoria

A migração `V3` cria `documento_auditoria`, sem chave estrangeira para o
documento, preservando o histórico após o DELETE. São registrados UUID,
operação, usuário disponível, detalhes operacionais e instante. Conteúdo, hash
e chave privada não são copiados.

## Critérios de aceite atendidos

| Critério | Evidência |
| --- | --- |
| PATCH restringe campos | DTO dedicado |
| Concorrência retorna 409 | versão esperada e lock otimista |
| DELETE retorna 204 | controller sem corpo |
| Conteúdo correto é removido | chave da entidade e ação pós-commit |
| Texto não acessa storage | fluxo condicionado à presença da chave |
| Operações auditáveis | tabela e repository próprios |

## Testes

Os testes cobrem atualização válida, versão obsoleta, exclusão binária,
exclusão textual, auditoria e contratos HTTP de PATCH/DELETE.
