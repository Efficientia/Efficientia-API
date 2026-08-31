# EFFICIENTI-223 — Sprint 06: assinaturas por foto, desenho e texto

## Objetivo

Atender as três modalidades de assinatura previstas para motorista, manobrista,
curraleiro e funcionário Friboi, mantendo um contrato acessível para Mobile e
React e garantindo exclusividade entre texto e arquivo.

## Contratos HTTP

A URL de criação continua sendo `POST /api/v1/documentos` e diferencia o fluxo
pelo `Content-Type`:

| Content-Type | Modalidade | Conteúdo |
| --- | --- | --- |
| `multipart/form-data` | `FOTO` ou `DESENHO` | metadados JSON e PNG |
| `application/json` | `TEXTO` | `AssinaturaTextoRequest`, sem arquivo |

Os dois formatos exigem o header UUID `Idempotency-Key` e retornam
`201 Created` com o header `Location`.

## Assinatura textual

O contrato JSON exige:

- `tipoDocumento=ASSINATURA`;
- `origem=TEXTO`;
- `modalidadeAssinatura=TEXTO`;
- viagem e assinante com identificadores positivos;
- `papelAssinante` preenchido;
- texto entre 1 e 150 caracteres.

`AssinaturaValidator` remove apenas espaços externos e normaliza o texto em
Unicode NFC. A capitalização, os acentos e o conteúdo digitado são preservados.

São rejeitados:

- tags ou fragmentos com os delimitadores `<` e `>`;
- scripts enviados como HTML;
- quebras de linha e demais caracteres de controle;
- texto vazio ou acima de 150 pontos de código Unicode;
- combinações com tipo, origem ou modalidade diferentes de TEXTO.

## Foto e desenho

Assinaturas binárias continuam usando o fluxo multipart da Sprint 05, com
regras adicionais:

- `FOTO` exige origem `CAMERA` e MIME real `image/png`;
- `DESENHO` exige origem `DESENHO` e MIME real `image/png`;
- PDF nunca é aceito como assinatura;
- multipart nunca aceita modalidade `TEXTO`;
- assinante, papel e modalidade são obrigatórios.

Documentos que não são assinatura rejeitam campos de assinante e modalidade.

## Persistência

Assinatura textual grava somente metadados e `texto_assinatura` no PostgreSQL.
Os campos abaixo permanecem nulos:

- `nome_original`;
- `mime_type`;
- `tamanho_bytes`;
- `sha256`;
- `storage_key`.

O fluxo textual não chama `StorageService`. Foto e desenho usam o storage
privado e a mesma compensação transacional definida na Sprint 05.

## Resposta pública

Para modalidade TEXTO:

- `textoAssinatura` contém o texto normalizado;
- `conteudoUrl` é nulo;
- campos de arquivo são nulos;
- `storageKey` e `idempotencyKey` não fazem parte do DTO.

Foto e desenho retornam metadados do PNG e uma `conteudoUrl` privada da API.

## Idempotência

A consulta pela chave ocorre antes da validação e da persistência. Repetir a
mesma operação retorna o documento original sem chamar storage ou repository
de escrita novamente.

## Critérios de aceite atendidos

| Critério | Evidência |
| --- | --- |
| Texto válido retorna 201 sem storage | testes de controller e service |
| Texto preserva acentos e capitalização | teste com sequência Unicode decomposta normalizada para NFC |
| HTML e controles são rejeitados | testes dedicados do validator |
| Foto e desenho exigem PNG | validação por modalidade, origem e MIME real |
| Texto e arquivo são mutuamente exclusivos | endpoints separados por Content-Type e regras do validator |
| Site recebe texto acessível | response contém texto e `conteudoUrl=null` |

## Testes

Foram adicionados 11 casos desta sprint:

- 7 casos de validação e normalização das três modalidades;
- 2 casos de service garantindo persistência textual sem storage;
- 2 casos de controller para criação JSON e rejeição de texto vazio.

Em 31/08/2026, a suíte acumulada executou 49 testes, sem falhas ou erros.

## Fora do escopo desta sprint

- listagem e filtros;
- paginação por página e cursor;
- consulta individual por UUID;
- streaming de download;
- autenticação e autorização.

As consultas pertencem à Sprint 07 deste lote.
