# SPEC-SPRINT-19: Onboarding Corporativo - Dados Complementares da Empresa (Etapa 1 de 3) e Fluxo Integrado Web/Mobile

## 1. Visão Geral e Contexto de Negócio

Este documento estabelece as especificações arquiteturais e contratos da **Etapa 1 de 3 do Onboarding Corporativo**, além de mapear a divisão clara de responsabilidades entre o **Portal Web** e o **Aplicativo Mobile (desenvolvido pela equipe mobile)**.

### 1.1 Divisão de Responsabilidades Web vs. Mobile

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                             API EFFICIENTIA                                 │
└───────▲──────────────────────▲────────────────────────▲──────────────▲──────┘
        │                      │                        │              │
 ┌──────┴───────────────┐ ┌────┴───────────────────┐ ┌──┴──────────────┴──────┐
 │      PORTAL WEB      │ │    PRÉ-CADASTRO RH     │ │       APP MOBILE       │
 │  (Administrador/RH)  │ │   (Motorista/Analista) │ │      (Motorista)       │
 ├──────────────────────┤ ├────────────────────────┤ ├────────────────────────┤
 │ 1. Criação Empresa   │ │ 4. Pré-login equipe:   │ │ 5. Validação e Login   │
 │ 2. 1º Acesso do ADM  │ │    - Motoristas        │ │    (com código empresa)│
 │ 3. Etapa 1 de 3:     │ │    - Analistas         │ │ 6. Preenchimento de    │
 │    - Dados Legais    │ │    - Manobristas       │ │    Relatórios Viagem   │
 │    - Endereço compl. │ │    - Curraleiros       │ │ 7. Coleta Assinaturas  │
 │    - Upload de Logo  │ │                        │ │ 8. Upload Comprovantes │
 │ 9. Download Doc/Zip  │ │                        │ │    (fotos e PDFs)      │
 └──────────────────────┘ └────────────────────────┘ └────────────────────────┘
```

1. **Portal Web (Gestão e Onboarding Corporativo):**
   - **Cadastro da Empresa:** razão social, nome fantasia, CNPJ, e-mail corporativo e geração automática do código da empresa de 8 dígitos (ex: `FRI48291`).
   - **Primeiro Acesso do ADM:** cadastro obrigatório do primeiro administrador utilizando o código da empresa para liberar o acesso ao sistema.
   - **Etapa 1 de 3 (Dados da Empresa):** inserção e enriquecimento das informações complementares (telefone corporativo, endereço completo com CEP/logradouro/número/cidade/UF e upload de logotipo em PNG ou SVG de até 5 MB).
   - **Gestão de Administradores:** adesão de novos administradores por administradores logados.
   - **Pré-login de Funcionários:** cadastro dos motoristas, manobristas e analistas vinculando-os ao tenant corporativo.
   - **Gestão Documental pelo Analista:** consulta aos relatórios de viagem preenchidos pelos motoristas no app e download/exportação em `.zip` dos documentos e assinaturas.

2. **Aplicativo Mobile (Operação em Campo pelo Motorista):**
   - **Login Rápido:** o motorista pré-cadastrado no Web entra no app fornecendo CPF/e-mail, senha e código da empresa. A API valida a existência no banco.
   - **Operação de Transporte:** preenchimento da GTA, quilometragem, vistorias e coleta de assinaturas digitais na tela do smartphone.
   - **Envio de Documentos:** upload de fotos e PDFs de comprovantes direto para o storage privado via API.

---

## 2. Tela de Onboarding: Etapa 1 de 3 (Dados da Empresa)

Baseado no layout da aplicação web:

- **Progresso:** `ETAPA 1 DE 3`
- **Título:** `Dados da empresa`
- **Subtítulo:** `Informações usadas nos relatórios e documentos da operação.`
- **Bloco de Identificação Legal e Endereço:**
  - `CNPJ` (com máscara `12.345.678/0001-90` ou 14 dígitos numéricos)
  - `Razão Social` (ex: `Efficientia Transportes Ltda.`)
  - `Nome Fantasia` (ex: `Efficientia`)
  - `E-mail corporativo` (ex: `operacao@efficientia.com.br`)
  - `Telefone` (ex: `+55 (67) 99999-2048`)
  - `CEP` (ex: `79002-190`)
  - `Logradouro` (ex: `Av. Afonso Pena`)
  - `Número` (ex: `2450`)
  - `Cidade` (ex: `Campo Grande`)
  - `UF / Estado` (ex: `MS`)
- **Painel Lateral de Logotipo:**
  - `LOGO DA EMPRESA`
  - Requisitos: `PNG ou SVG • até 5 MB`
  - Ação: Botão `Enviar logo` (upload multipart assíncrono ou imediato)
- **Ação Principal:** Botão `Salvar e continuar` (avança para a Etapa 2 de 3).

---

## 3. Endpoints da API

### 3.1 Atualizar Dados Cadastrais Complementares
- **Método / Rotas:**
  - `PUT /api/v1/empresas/{id}`
  - `PUT /api/v1/empresas/{id}/dados-complementares`
  - `PUT /api/v1/empresas/{id}/etapa-1`
  - `PATCH /api/v1/empresas/{id}`
  - `PUT /api/v1/empresas/codigo/{codigo}` (consulta e atualização direta por código corporativo)
- **Autenticação:** Pública durante onboarding corporativo ou protegida por token Bearer com perfil `ROLE_ADMIN` / `ROLE_ADMINISTRADOR`.
- **Corpo da Requisição (JSON):**
```json
{
  "nomeFantasia": "Efficientia",
  "razaoSocial": "Efficientia Transportes Ltda.",
  "cnpj": "12.345.678/0001-90",
  "emailCorporativo": "operacao@efficientia.com.br",
  "telefone": "+55 (67) 99999-2048",
  "cep": "79002-190",
  "logradouro": "Av. Afonso Pena",
  "numero": "2450",
  "cidade": "Campo Grande",
  "estado": "MS",
  "logoUrl": "/api/v1/empresas/1/logo/conteudo"
}
```
- **Resposta (200 OK):**
```json
{
  "id": 1,
  "codigoEmpresa": "EFF12345",
  "codigo": "EFF12345",
  "codigoInterno": "EFF12345",
  "nomeEmpresa": "Efficientia",
  "nome": "Efficientia",
  "nomeFantasia": "Efficientia",
  "razaoSocial": "Efficientia Transportes Ltda.",
  "cnpj": "12345678000190",
  "emailCorporativo": "operacao@efficientia.com.br",
  "email": "operacao@efficientia.com.br",
  "telefone": "+55 (67) 99999-2048",
  "enderecoId": 10,
  "endereco": {
    "id": 10,
    "cep": "79002190",
    "logradouro": "Av. Afonso Pena",
    "numero": "2450",
    "cidade": "Campo Grande",
    "estado": "MS",
    "uf": "MS"
  },
  "cep": "79002190",
  "logradouro": "Av. Afonso Pena",
  "numero": "2450",
  "cidade": "Campo Grande",
  "estado": "MS",
  "uf": "MS",
  "logoUrl": "/api/v1/empresas/1/logo/conteudo",
  "etapaCadastro": 2,
  "cadastroCompleto": false,
  "status": "ATIVO",
  "requerPrimeiroAdmin": false,
  "proximoPasso": "PAINEL_ADMINISTRATIVO",
  "mensagem": "Empresa ativa e operacional.",
  "criadoEm": "2026-09-30T10:00:00Z",
  "atualizadoEm": "2026-09-30T11:45:00Z"
}
```

### 3.2 Upload de Logotipo da Empresa
- **Método / Rotas:**
  - `POST /api/v1/empresas/{id}/logo`
  - `POST /api/v1/empresas/{id}/logo-upload`
- **Content-Type:** `multipart/form-data`
- **Campos do Formulário:**
  - `arquivo` (ou `file`, `logo`): arquivo binário da imagem
- **Regras de Validação:**
  - Formatos aceitos: `PNG` (`image/png`) ou `SVG` (`image/svg+xml`).
  - Tamanho máximo permitido: **5 MB** (`5 * 1024 * 1024` bytes).
- **Resposta (200 OK):**
```json
{
  "logoUrl": "/api/v1/empresas/1/logo/conteudo",
  "mensagem": "Logo da empresa enviada com sucesso.",
  "tamanhoBytes": 204800,
  "mimeType": "image/png"
}
```

### 3.3 Visualização / Download Direto do Logotipo
- **Método / Rota:**
  - `GET /api/v1/empresas/{id}/logo/conteudo`
- **Autenticação:** Pública (`permitAll()`), permitindo renderização imediata em tags `<img src="...">` ou aplicativos sem headers complexos.
- **Headers de Resposta:**
  - `Content-Type`: `image/png` ou `image/svg+xml`
  - `Cache-Control`: `public, max-age=86400`
- **Corpo:** Array de bytes da imagem original.

---

## 4. Evolução do Banco de Dados (Flyway Migration V6)

Arquivo: `src/main/resources/db/migration/V6__empresa_dados_complementares.sql`

```sql
-- V6: Dados cadastrais complementares da empresa (Etapa 1 de 3 - Onboarding Corporativo)
ALTER TABLE public.empresa ADD COLUMN IF NOT EXISTS telefone VARCHAR(20);
ALTER TABLE public.empresa ADD COLUMN IF NOT EXISTS nome_fantasia VARCHAR(150);
ALTER TABLE public.empresa ADD COLUMN IF NOT EXISTS logo_url VARCHAR(500);
ALTER TABLE public.empresa ADD COLUMN IF NOT EXISTS etapa_cadastro INTEGER DEFAULT 1;
ALTER TABLE public.empresa ADD COLUMN IF NOT EXISTS cadastro_completo BOOLEAN DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_empresa_telefone ON public.empresa(telefone);
```

---

## 5. Matriz de Rastreabilidade e Critérios de Aceite

| ID | Cenário / Critério | Comportamento Esperado | Status |
|---|---|---|---|
| **CA-01** | Cadastro inicial com campos complementares | `POST /api/v1/empresas` aceita telefone, nome fantasia e endereço, persistindo e vinculando `enderecoId`. | **Aprovado** |
| **CA-02** | Atualização da Etapa 1 por ID | `PUT /api/v1/empresas/{id}/dados-complementares` salva informações e avança `etapaCadastro` para 2. | **Aprovado** |
| **CA-03** | Atualização da Etapa 1 por Código | `PUT /api/v1/empresas/codigo/{codigo}` atualiza a empresa correspondente sem necessitar do ID numérico. | **Aprovado** |
| **CA-04** | Upload de Logotipo PNG/SVG até 5 MB | `POST /api/v1/empresas/{id}/logo` armazena a imagem e define a URL correspondente. | **Aprovado** |
| **CA-05** | Rejeição de Logotipo > 5 MB ou tipo inválido | Retorna `400 Bad Request` com mensagem de erro clara. | **Aprovado** |
| **CA-06** | Visualização pública do Logotipo | `GET /api/v1/empresas/{id}/logo/conteudo` entrega bytes com `Content-Type` adequado. | **Aprovado** |
| **CA-07** | Validação de unicidade de CNPJ e E-mail | Atualizações com CNPJ/E-mail de outra empresa retornam `409 Conflict`. | **Aprovado** |
| **CA-08** | Integração Mobile: pré-login de funcionários | ADM faz pré-login no web via `/api/v1/empresas/{id}/funcionarios`; motorista entra no mobile com CPF/e-mail + senha + código. | **Aprovado** |
