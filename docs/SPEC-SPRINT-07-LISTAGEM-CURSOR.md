# EFFICIENTI-224 — Sprint 07: listagem, filtros, metadados e cursor

## Objetivo

Disponibilizar os metadados públicos dos documentos para o site React e para a
sincronização incremental da Efficientia AI API, sem expor storage, banco ou
conteúdo binário.

## Endpoints

Foram implementados:

```http
GET /api/v1/documentos
GET /api/v1/documentos/{id}
```

A consulta individual retorna `DocumentoResponse` sem abrir o arquivo. UUID
inexistente gera `404 Not Found` em formato `ProblemDetail`.

## Modo página para o React

Quando `cursor` não é informado, a listagem usa paginação com contagem:

```http
GET /api/v1/documentos?page=0&size=20&sort=criadoEm,desc
```

Regras:

- `page` começa em zero;
- `size` deve ficar entre 1 e 100;
- a resposta inclui `totalElementos` e `totalPaginas`;
- campos de ordenação permitidos: `id`, `criadoEm`, `atualizadoEm`,
  `tipoDocumento` e `origem`;
- direções permitidas: `asc` e `desc`.

Campos desconhecidos não são repassados ao JPA.

## Modo cursor para a AI API

O cliente inicia com:

```http
GET /api/v1/documentos?cursor=INICIO&size=100
```

As próximas chamadas usam `proximoCursor`. O valor é Base64 URL-safe e codifica
internamente o par `criadoEm + id`; o cliente não depende desse formato.

O repository busca `size + 1` itens para determinar `temMais`, sem executar
consulta de contagem. A ordenação é sempre crescente por:

1. `criadoEm`;
2. `id`, como desempate determinístico.

O próximo cursor é derivado do último item entregue. Repetir o mesmo cursor é
uma consulta sem estado e devolve o mesmo lote, sem pular ou duplicar itens.

`page` e `cursor` são mutuamente exclusivos. Cursor vazio, malformado ou acima
do limite é rejeitado com `400 Bad Request`.

## Filtros

Os dois modos aplicam filtros no banco:

| Filtro | Tipo |
| --- | --- |
| `viagemId` | integer positivo |
| `assinanteId` | integer positivo |
| `tipoDocumento` | enum |
| `origem` | enum |
| `modalidadeAssinatura` | enum |
| `criadoDe` | ISO 8601 inclusivo |
| `criadoAte` | ISO 8601 inclusivo |

`criadoDe` não pode ser posterior a `criadoAte`.

## Repository

`DocumentoQueryRepository` implementa consultas com Criteria API:

- página com conteúdo e `COUNT` filtrado;
- cursor com os mesmos filtros, ordem fixa e sem `COUNT`;
- ordenação mapeada por lista permitida, sem nomes arbitrários de propriedades.

O cursor usa a comparação:

```text
criadoEm > cursor.criadoEm
OU (criadoEm = cursor.criadoEm E id > cursor.id)
```

## Contrato público

`PaginaDocumentosResponse` contém:

- `itens`;
- `page` e totais no modo página;
- `proximoCursor` no modo cursor quando existe outro lote;
- `size` e `temMais` nos dois modos.

Cada item é produzido por `DocumentoMapper`. `storageKey`, caminho físico,
`idempotencyKey`, Base64 e bytes nunca aparecem.

Assinatura textual mantém `textoAssinatura` preenchido e `conteudoUrl` nulo.

## Critérios de aceite atendidos

| Critério | Evidência |
| --- | --- |
| Página retorna somente campos públicos | mapper e teste de controller |
| page e cursor não podem ser combinados | validação do service |
| Cursor não pula nem duplica | ordenação `criadoEm + id` e teste de desempate |
| Filtros funcionam nos dois modos | testes do repository para página e cursor |
| GET por ID retorna 404 | service e controller advice testados |
| Texto retorna URL nula | contrato do mapper preservado desde a Sprint 06 |

## Testes

Foram adicionados 12 casos desta sprint:

- 3 do codec para round-trip, `INICIO` e cursor inválido;
- 4 do service para página, cursor, exclusividade e 404;
- 3 do repository para filtros e desempate determinístico;
- 2 do controller para listagem e consulta inexistente.

Em 31/08/2026, a suíte acumulada executou 61 testes, sem falhas ou erros.

## Fora do escopo desta sprint

- streaming do conteúdo em `/conteudo`;
- alteração e exclusão de documentos;
- autenticação e autorização;
- scheduler e persistência interna da AI API.

Esses itens permanecem nas próximas tarefas do épico.
