# SPEC-SPRINT-21: Assinatura Fixa do Motorista como Imagem (PNG), Versionamento Imutável e Vínculo com Documentos

## 1. Visão Geral e Contexto de Negócio

Esta especificação formaliza a arquitetura, modelo de dados relacional e contratos REST para o recurso de **Assinatura Fixa do Motorista como Imagem**, atendendo aos requisitos integrados do aplicativo **Mobile** e do **Portal Web** de gestão corporativa.

### 1.1 Diferenciação Fundamental de Conceitos
No ecossistema Efficientia, coexistem dois tipos de assinatura que não devem ser confundidos:
1. **Assinatura Fixa do Motorista (`assinatura_motorista`):**
   - Assinatura atual do perfil do motorista, gerada no primeiro acesso ou cadastrada previamente pela administração.
   - Pode ser criada desenhando manualmente na tela do app ou digitando o nome completo (com renderização de prévia gráfica convertida em PNG transparente).
   - Suporta substituição controlada (nova versão desativa a versão anterior sem apagá-la fisicamente do histórico).
2. **Assinatura do Formulário / Relatório de Viagem (`documento`):**
   - Evidência imutável coletada no contexto de uma viagem específica (`viagemId`), boletim de embarque/desembarque ou conferência de carga viva.
   - Possui vínculo imutável com a versão exata da assinatura do motorista ativa no instante da assinatura (`documento.assinatura_motorista_id`).
   - Uma eventual atualização futura da assinatura fixa no perfil do motorista **nunca modifica retroativamente formulários ou relatórios já finalizados e auditados**.

---

## 2. Fluxo Operacional: Mobile, Web e Login

```
┌──────────────────────────────────────────────────────────────────────────────────┐
│                            FLUXO DE PRIMEIRO ACESSO / LOGIN                      │
└──────────────────────────────────────────────────────────────────────────────────┘
                                          │
                        POST /api/v1/auth/login
                                          │
                                          ▼
                ┌──────────────────────────────────────────────────┐
                │ Resposta de Autenticação com Metadados:          │
                │ - token: "eyJhbGciOi..."                         │
                │ - usuario.assinaturaFixaCadastrada: true | false │
                │ - usuario.assinaturaFixaId: UUID | null          │
                └─────────────────────────┬────────────────────────┘
                                          │
                 ┌────────────────────────┴────────────────────────┐
                 ▼ (assinaturaFixaCadastrada == false)             ▼ (assinaturaFixaCadastrada == true)
┌────────────────────────────────────────────────┐  ┌────────────────────────────────────────────────┐
│   TELA DE PRIMEIRO ACESSO NO APLICATIVO        │  │       CONSULTA / EXIBIÇÃO NO APLICATIVO        │
│ 1. Motorista escolhe:                          │  │ 1. O app já sabe que possui assinatura fixa    │
│    - Desenho na tela touchscreen, ou           │  │ 2. Consulta prévia via:                        │
│    - Digitação de nome completo estilizado     │  │    GET /api/v1/usuarios/me/assinatura/conteudo │
│ 2. App gera imagem PNG com fundo transparente  │  │ 3. Exibe a assinatura na tela de perfil        │
│ 3. Envia com Idempotency-Key e JWT:            │  │ 4. Segue fluxo normal para viagens e checklists│
│    PUT /api/v1/usuarios/me/assinatura          │  └────────────────────────────────────────────────┘
│ 4. API valida Magic Bytes, SHA-256 e persiste  │
│ 5. App libera acesso às rotas operacionais     │
└────────────────────────────────────────────────┘
```

---

## 3. Modelagem de Dados e Migração (Flyway V8)

Arquivo: `src/main/resources/db/migration/V8__create_assinatura_motorista.sql`

```sql
CREATE TABLE IF NOT EXISTS public.assinatura_motorista (
    id UUID PRIMARY KEY,
    motorista_id INTEGER NOT NULL,
    modalidade VARCHAR(30) NOT NULL,
    texto_origem VARCHAR(150),
    mime_type VARCHAR(50) NOT NULL,
    conteudo BYTEA NOT NULL,
    tamanho_bytes BIGINT NOT NULL,
    sha256 CHAR(64) NOT NULL,
    idempotency_key UUID NOT NULL UNIQUE,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    criado_por INTEGER NOT NULL,
    criado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    versao BIGINT NOT NULL DEFAULT 0,

    CONSTRAINT fk_assinatura_motorista
        FOREIGN KEY (motorista_id) REFERENCES public.usuario(id) ON DELETE RESTRICT,
    CONSTRAINT fk_assinatura_motorista_criado_por
        FOREIGN KEY (criado_por) REFERENCES public.usuario(id) ON DELETE RESTRICT,
    CONSTRAINT chk_assinatura_modalidade
        CHECK (modalidade IN ('DESENHO', 'NOME_DIGITADO')),
    CONSTRAINT chk_assinatura_mime_type
        CHECK (mime_type = 'image/png'),
    CONSTRAINT chk_assinatura_tamanho
        CHECK (tamanho_bytes > 0),
    CONSTRAINT chk_assinatura_sha256
        CHECK (sha256 ~ '^[0-9A-Fa-f]{64}$')
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_assinatura_motorista_ativa
    ON public.assinatura_motorista(motorista_id)
    WHERE ativa = TRUE;

CREATE INDEX IF NOT EXISTS idx_assinatura_motorista_idempotency
    ON public.assinatura_motorista(idempotency_key);

ALTER TABLE public.documento
    ADD COLUMN IF NOT EXISTS assinatura_motorista_id UUID NULL;

ALTER TABLE public.documento
    ADD CONSTRAINT fk_documento_assinatura_motorista
    FOREIGN KEY (assinatura_motorista_id)
    REFERENCES public.assinatura_motorista(id)
    ON DELETE RESTRICT;
```

---

## 4. Contratos de API REST

### 4.1 Rotas do Próprio Motorista Autenticado (`/me`)

#### `PUT /api/v1/usuarios/me/assinatura`
- **Descrição:** Cadastro ou substituição da assinatura fixa do motorista autenticado.
- **Autenticação:** `Bearer <JWT>` (Perfil: `ROLE_MOTORISTA`).
- **Headers Obrigatórios:** `Idempotency-Key: <UUID>`
- **Content-Type:** `multipart/form-data`
  - Part `arquivo`: binário do PNG (até 1 MB).
  - Part `metadados`: JSON:
    ```json
    {
      "modalidade": "DESENHO",
      "textoOrigem": "João da Silva"
    }
    ```
- **Respostas:**
  - `200 OK`: Assinatura criada ou atualizada.
  - `400 Bad Request`: Falta de Idempotency-Key, partes multipart ou metadados inválidos.
  - `401 Unauthorized`: Token ausente ou inválido.
  - `403 Forbidden`: Usuário não é motorista.
  - `413 Payload Too Large`: Arquivo excede 1 MB.
  - `415 Unsupported Media Type`: Arquivo não possui o cabeçalho binário oficial do formato PNG.

#### `GET /api/v1/usuarios/me/assinatura`
- **Descrição:** Consulta metadados da assinatura ativa do motorista autenticado.
- **Autenticação:** `Bearer <JWT>` (`ROLE_MOTORISTA`).
- **Respostas:**
  - `200 OK`: Metadados da assinatura ativa.
  - `404 Not Found`: Motorista ainda não possui assinatura cadastrada.

#### `GET /api/v1/usuarios/me/assinatura/conteudo`
- **Descrição:** Download/transmissão direta dos bytes PNG da assinatura ativa.
- **Headers de Resposta:**
  - `Content-Type: image/png`
  - `Content-Disposition: inline; filename="assinatura.png"`
  - `Cache-Control: private, no-store`

---

### 4.2 Rotas Administrativas e de Auditoria Web

#### `PUT /api/v1/usuarios/{motoristaId}/assinatura`
- **Descrição:** Cadastro administrativo da assinatura fixa antes do primeiro login do motorista.
- **Autenticação:** `Bearer <JWT>` (`ROLE_ADMIN` ou `ROLE_ADMINISTRADOR`).
- **Respostas:**
  - `200 OK`: Sucesso.
  - `403 Forbidden`: Não administrador.
  - `422 Unprocessable Entity`: O usuário alvo (`motoristaId`) existe mas não possui perfil de motorista.

#### `GET /api/v1/usuarios/{usuarioId}/assinatura`
- **Descrição:** Consulta metadados da assinatura fixa de terceiros para exibição em painel web.
- **Autenticação:** Restrito a `ROLE_ADMIN`, `ROLE_ADMINISTRADOR`, `ROLE_ANALISTA` ou `ROLE_FUNCIONARIO_FRIBOI`.

#### `GET /api/v1/usuarios/{usuarioId}/assinatura/conteudo`
- **Descrição:** Transmite binário PNG da assinatura de terceiros para relatórios e formulários web.

---

## 5. Regras de Validação e Segurança

1. **Magic Bytes do PNG:** A validação inspeciona os primeiros 8 bytes (`89 50 4E 47 0D 0A 1A 0A`). Extensões de arquivo ou `Content-Type` adulterados são rejeitados com `415 Unsupported Media Type`.
2. **Tamanho Máximo:** Limite estrito de 1 MB (1.048.576 bytes). Ultrapassagens retornam `413 Payload Too Large`.
3. **Integridade Criptográfica (SHA-256):** O hash SHA-256 é calculado pelo servidor e armazenado na coluna `sha256 CHAR(64)` para auditoria forense.
4. **Idempotência Móvel:** Utilização de `Idempotency-Key` único com índice no banco de dados para evitar inserções duplicadas por repetições de requisição em conexões 4G/5G oscilantes.
5. **Versionamento e Imutabilidade:** Ao registrar uma nova assinatura, a versão ativa anterior recebe `ativa = false` e a nova versão é criada com `versao = versaoAnterior + 1` em transação única (`@Transactional`).
6. **Desacoplamento em Formulários:** O vínculo de auditoria em relatórios aponta para o UUID imutável da versão da assinatura (`documento.assinatura_motorista_id`), garantindo valor probatório legal.
