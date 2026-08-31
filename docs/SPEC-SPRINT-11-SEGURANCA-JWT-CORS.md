# EFFICIENTI-228 — Sprint 11: segurança, autorização e CORS

## Objetivo

Proteger o contrato público de documentos com JWT, aplicar escopo por usuário e
papel no banco e aceitar chamadas somente das origens web configuradas.

## Ativação por ambiente

A segurança permanece desligada no ambiente local atual para não bloquear o
desenvolvimento antes da definição definitiva do provedor. Para ativá-la:

```dotenv
SECURITY_ENABLED=true
JWT_ISSUER_URI=https://SEU-PROJETO.supabase.co/auth/v1
JWT_JWK_SET_URI=https://SEU-PROJETO.supabase.co/auth/v1/.well-known/jwks.json
JWT_AUDIENCE=efficientia-api
CORS_ALLOWED_ORIGINS=https://app.efficientia.example
```

Com `SECURITY_ENABLED=true`, issuer, assinatura, validade e audience são
validados pelo Resource Server. A API não usa a publishable key ou secret key
para validar requisições; ela consulta apenas o JWKS público configurado.

## Papéis

O conversor aceita `roles` ou `role` no JWT, inclusive dentro de
`app_metadata`, e reconhece somente:

- `MOTORISTA`: consulta e envia documentos das próprias viagens;
- `FUNCIONARIO_FRIBOI`: consulta documentos criados ou assinados pelo próprio
  usuário e pode alterar/excluir;
- `ADMIN`: escopo integral e operações administrativas.

Papéis desconhecidos não viram autoridades. O identificador relacional é lido
de `usuario_id`, com fallback para um `sub` numérico.

## Isolamento de dados

O escopo é incorporado ao `DocumentoFiltro` e aplicado na consulta SQL:

- motorista: `viagem_id` precisa pertencer a um relatório cujo
  `motorista_id` é o usuário autenticado;
- funcionário: `criado_por` ou `assinante_id` precisa ser o usuário;
- admin: sem predicado adicional.

Consulta por UUID, streaming, idempotência, criação, alteração e exclusão usam
a mesma política. Documento fora do escopo retorna `403 Forbidden`. Novos
documentos recebem `criado_por` a partir do token e auditoria registra o ator
da operação.

## Segurança HTTP e CORS

- sessão stateless e CSRF desabilitado para Bearer Token;
- ausência de token retorna `401 Unauthorized`;
- papel insuficiente ou escopo incorreto retorna `403 Forbidden`;
- preflight e respostas CORS usam somente `CORS_ALLOWED_ORIGINS`;
- métodos aceitos: GET, POST, PATCH, DELETE e OPTIONS;
- conteúdo binário mantém `Cache-Control: private, no-store`;
- tokens, chaves, conteúdo e caminhos privados não são registrados em log.

Swagger e health permanecem públicos; os endpoints de documentos exibem o
esquema `bearerAuth`.

## Critérios de aceite atendidos

| Critério | Evidência |
| --- | --- |
| Sem token retorna 401 | cadeia Resource Server testada |
| Fora do escopo retorna 403 | policy e teste web |
| Motorista isolado por viagem | subconsulta por `motorista_id` |
| Funcionário sob responsabilidade | predicado `criado_por/assinante_id` |
| Sem provedor externo nos testes | `jwt()` e componentes locais |
| CORS restrito | origem configurada em teste de preflight |

## Dependência pendente

Para produção, a equipe ainda precisa informar os valores reais de issuer,
JWKS, audience e o formato definitivo dos claims. A implementação já os mantém
externalizados e não exige alteração de código para essa troca.
