# SPEC-SPRINT-23: Assinaturas de Participantes do Diário de Rota (Upload Binário Multipart e Streaming PNG)

## 1. Visão Geral e Contexto de Negócio

No transporte de gado bovino da fazenda ao frigorífico, o diário de rota exige a coleta e validação de 4 assinaturas distintas:
1. **Motorista:** Capturada da assinatura fixa ativa cadastrada no perfil (`/api/v1/usuarios/me/assinatura`).
2. **Pecuarista:** Coletada no embarque na fazenda (produtor rural).
3. **Manobrista:** Coletada no pátio da unidade frigorífica.
4. **Curraleiro:** Coletada no curral de desembarque de animais.

Esta especificação implementa o contrato para upload binário das assinaturas de pecuarista, manobrista e curraleiro a partir dos desenhos PNG capturados localmente no aplicativo mobile, persistência no storage gerenciado e streaming para renderização na Web e no App.

---

## 2. Contratos HTTP dos Novos Endpoints

### 2.1 Upload de Assinatura de Participante (`PUT /api/v1/relatorios-viagem/{id}/assinaturas/{papel}`)

- **Método / URL:** `PUT /api/v1/relatorios-viagem/{id}/assinaturas/{papel}`
- **Parâmetros de Path:**
  - `id`: `Integer` (ID do relatório de viagem).
  - `papel`: `String` (`pecuarista`, `motorista`, `manobrista`, `curraleiro` — case insensitive).
- **Headers:**
  - `Authorization: Bearer <TOKEN_JWT>`
  - `Idempotency-Key: <UUID>` (opcional para rascunhos, recomendado)
  - `Content-Type: multipart/form-data`
- **Partes Multipart:**
  - `arquivo`: Arquivo binário PNG (máximo 1 MB, validado via magic bytes `89 50 4E 47 0D 0A 1A 0A`).
  - `metadados` (opcional, JSON): `{"modalidade": "DESENHO", "textoOrigem": "Nome Opcional"}`.
- **Respostas:**
  - `200 OK`: Retorna `RelatorioViagemResponse` atualizado com a URL de streaming vinculada (`urlAssinaturaPecuarista`, `urlAssinaturaManobrista`, etc.).
  - `400 Bad Request`: Papel inválido ou relatório finalizado/aprovado.
  - `401 Unauthorized`: Token ausente ou inválido.
  - `403 Forbidden`: Motorista tentando alterar relatório de outro motorista.
  - `404 Not Found`: Relatório não encontrado.
  - `413 Payload Too Large`: Arquivo maior que 1 MB.
  - `415 Unsupported Media Type`: Arquivo não é PNG autêntico.

---

### 2.2 Download e Streaming da Assinatura PNG (`GET /api/v1/relatorios-viagem/{id}/assinaturas/{papel}/conteudo`)

- **Método / URL:** `GET /api/v1/relatorios-viagem/{id}/assinaturas/{papel}/conteudo`
- **Headers:**
  - `Authorization: Bearer <TOKEN_JWT>`
- **Resposta (200 OK):**
  - `Content-Type: image/png`
  - `Content-Disposition: inline; filename="assinatura-{papel}-{id}.png"`
  - `Cache-Control: private, no-store`
  - **Body:** Bytes binários da imagem PNG.

---

## 3. Validação de Submissão e Finalização do Relatório

Ao submeter o relatório como `status = "pendente"`, enviar via `PATCH /enviar` ou finalizar via `PATCH /finalizar`:
- A API valida se as 4 assinaturas obrigatórias estão presentes.
- Se faltar qualquer assinatura, retorna `422 Unprocessable Entity` com `AssinaturasIncompletasException` listando exatamente quais papéis estão pendentes.

---

## 4. Evolução de Schemas e Migrations (Flyway V13)

- Atualização da constraint `chk_documento_papel_assinante` para aceitar `PECUARISTA`.
- Ajuste de Foreign Keys de `public.assinatura_motorista` para `sc_corporativo.tb_usuario`.
- Garantia de colunas complementares em `sc_corporativo.tb_usuario`.
- Remoção da anotação `@Version` do campo `versao` de `AssinaturaMotoristaEntity` para suporte ao modelo append-only imutável.
