# EFFICIENTI-220 — Sprint 03: domínio e persistência de documentos

## Objetivo

Implementar a base de domínio e persistência dos documentos pertencentes à API
REST principal, usando PostgreSQL e Spring Data JPA. O MongoDB permanece
exclusivo da Efficientia AI API.

## Decisão de compatibilidade

O schema relacional efetivamente implantado usa chaves `SERIAL`/`INTEGER` para
`usuario` e `relatorio_viagem`. Por isso:

- `documento.id` é UUID público;
- `documento.idempotency_key` é UUID único;
- `viagem_id` referencia `relatorio_viagem(id)` como INTEGER;
- `assinante_id` e `criado_por` referenciam `usuario(id)` como INTEGER.

Essa decisão mantém integridade referencial com os endpoints e dados já
implantados no Supabase.

## Migration V2

`V2__create_documento.sql` adiciona a tabela `documento` sem modificar a V1 já
registrada pelo Flyway.

Principais garantias no banco:

- tipos de documento, origem, papel e modalidade limitados por `CHECK`;
- `idempotency_key` e `storage_key` únicos;
- arquivo limitado a PDF ou PNG e tamanho positivo;
- SHA-256 com 64 caracteres hexadecimais;
- assinatura textual sem campos de arquivo;
- documento binário sem `texto_assinatura`;
- assinatura por foto ou desenho somente em PNG;
- chaves estrangeiras para viagem e usuários;
- índices por viagem, assinante, SHA-256 e cursor `(criado_em, id)`.

O conteúdo binário não é salvo no PostgreSQL. A tabela mantém apenas metadados,
referência privada de storage e texto curto quando a modalidade for `TEXTO`.

## Domínio Java

Foram definidos:

- `TipoDocumento`;
- `OrigemDocumento`;
- `ModalidadeAssinatura`;
- `PapelAssinante`;
- `DocumentoCursor`.

`DocumentoEntity` usa UUID, timestamps em UTC e controle otimista com
`@Version`. A entidade não possui Base64 nem `byte[]`.

## Repository

`DocumentoRepository` oferece:

- consulta padrão por UUID;
- consulta e verificação por `Idempotency-Key`;
- paginação por viagem;
- primeira página ordenada por `(criadoEm, id)`;
- continuação por cursor, com tamanho entre 1 e 100.

## Contrato público e mapper

`DocumentoMapper` gera `DocumentoResponse` com metadados públicos. O contrato
não contém `storageKey`, `idempotencyKey` ou caminho físico. `conteudoUrl` é
gerada somente quando existe conteúdo binário e fica nula para assinatura
textual.

## Testes

Os testes de repository usam banco H2 isolado e cobrem:

- persistência e consulta por UUID;
- consulta e unicidade da chave de idempotência;
- paginação por viagem;
- continuação por cursor.

Os testes do mapper verificam o contrato público e a ausência de campos
internos.

Foram adicionados 6 testes específicos desta sprint. A suíte completa da API
executou 20 testes, sem falhas.

## Validação integrada no Supabase

Em 25/08/2026, a aplicação foi iniciada com o `.env` local contra PostgreSQL
17.6 no Supabase. O Flyway:

- validou o histórico existente;
- aplicou `V2__create_documento.sql`;
- atualizou o schema `public` para a versão 2;
- confirmou, em uma segunda inicialização, que não havia migration pendente.

O Hibernate validou `DocumentoEntity` contra a tabela real e a aplicação
respondeu `UP` em `/actuator/health` e `/api/v1/status`.

## Fora do escopo desta sprint

- armazenamento físico ou MinIO;
- upload multipart;
- criação de assinatura textual por endpoint;
- controller e service de documentos;
- streaming, alteração e exclusão.

Esses itens pertencem às sprints EFFICIENTI-221 em diante.
