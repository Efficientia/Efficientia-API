# FIX-ADEQUACAO-SEGURANCA-INTEGRACAO-MOBILE

## Visão Geral
Este documento registra a resolução dos itens de segurança, autenticação e alinhamento de contratos reportados na auditoria de integração do aplicativo Mobile com a API `Efficientia-API` em ambiente de produção (Render).

---

## Detalhamento das Correções Realizadas

### 1. P0 — Ativação de Segurança e Proteção Padrão das Rotas
- **Problema:** O ambiente de produção estava executando com segurança desabilitada por padrão (`SECURITY_ENABLED=false`), deixando rotas de documentos, relatórios e cadastros abertas sem autenticação.
- **Correção:**
  - Configurado `app.security.enabled=${SECURITY_ENABLED:true}` em `application.properties`.
  - Incluída a rota pública `/api/v1/status` em `permitAll()`.
  - Todas as rotas de negócio (`/api/v1/documentos/**`, `/api/v1/relatorios-viagem/**`, `/api/v1/exportacoes/**`, `/api/v1/usuarios/**`, `/api/v1/enderecos/**`, `/api/v1/fazendas/**`, `/api/v1/veiculos/**`) foram protegidas e exigem cabeçalho `Authorization: Bearer <token_jwt>`.

### 2. P0 — Unificação da Emissão e Validação do JWT (HS256)
- **Problema:** A emissão de tokens usava `JwtTokenService` com algoritmo `HS256` e segredo HMAC, porém o `SecurityConfig` tentava validar via `NimbusJwtDecoder.withJwkSetUri`, causando falha de autenticação quando a segurança era ativada.
- **Correção:**
  - Atualizado `SecurityConfig.java` para decodificar JWTs usando a chave secreta HMAC (`NimbusJwtDecoder.withSecretKey`), lida de `SecurityProperties.secret()`.
  - Alinhados o emissor (`iss=efficientia-api`) e a audiência (`aud=efficientia-api`) entre o serviço de emissão e o Resource Server.

### 3. P0 — Suporte Expandido a Roles do Domínio
- **Problema:** `JwtRoleConverter` aceitava somente `MOTORISTA`, `FUNCIONARIO_FRIBOI` e `ADMIN`, descartando os demais papéis do enum `TipoUsuario`.
- **Correção:**
  - Atualizado `JwtRoleConverter.java` para aceitar todas as roles do sistema: `MOTORISTA`, `MANOBRISTA`, `ANALISTA`, `PECUARISTA`, `CURRALEIRO`, `FUNCIONARIO_FRIBOI` e `ADMIN`.
  - Atualizado o `SecurityConfig.java` para dar permissão de acesso a essas roles nos endpoints correspondentes.

### 4. P1 — Prevenção contra Enumeração de Usuários no Login
- **Problema:** O endpoint `POST /api/v1/auth/login` retornava mensagens de erro detalhadas (ex: "CPF diverge", "e-mail diverge", "senha incorreta"), permitindo a enumeração de usuários por atacantes.
- **Correção:**
  - Alterado `AuthService.java` para logar o motivo interno via SLF4J (`log.warn(...)`), mas lançar apenas a resposta genérica `"Credenciais inválidas."` (`HTTP 401 Unauthorized`).

### 5. P2 — Testes de Integração de Segurança
- **Correção:** Criado `SecurityIntegrationTest.java` cobrindo:
  - Rotas públicas (`/api/v1/status`) liberadas sem token.
  - Rotas protegidas bloqueadas com `HTTP 401` na ausência de token.
  - Validação do token `HS256` emitido no login.

---

## Status da Validação
- **Testes Unitários e de Integração:** 141 testes executados e aprovados (`BUILD SUCCESS`).
- **Deploy:** Publicado na branch `main` (`commit aff9080`).
