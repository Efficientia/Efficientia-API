# Mapa da API Efficientia (API Reference & Integration Guide)

Este guia serve como **referência unificada** para desenvolvedores **Mobile (iOS, Android, Flutter, React Native)** e **Web (React, Vue, Next.js)** que precisam integrar suas aplicações com a **Efficientia-API**.

---

## 🌍 1. Ambientes e URLs Base

- **Produção (Render):** `https://efficientia-api.onrender.com`
- **Desenvolvimento Local:** `http://localhost:8080`
- **Documentação Interativa (Swagger UI):** `https://efficientia-api.onrender.com/swagger-ui.html`
- **Especificação OpenAPI (JSON):** `https://efficientia-api.onrender.com/v3/api-docs`

---

## 🔑 2. Padrões de Autenticação e Cabeçalhos (Headers)

### 2.1 Autenticação JWT (Bearer Token)
Todas as rotas protegidas exigem o cabeçalho `Authorization` com o token recebido no endpoint de login:
```http
Authorization: Bearer <seu_token_jwt>
```

### 2.2 Cabeçalhos Padrão de Requisição
- `Content-Type`: `application/json` (para requisições com corpo JSON).
- `Content-Type`: `multipart/form-data` (para upload de arquivos).
- `Idempotency-Key`: `<UUID>` (obrigatório em `POST /api/v1/documentos` e `POST /api/v1/exportacoes`).
- `X-Correlation-Id`: `<UUID>` (opcional, para rastreabilidade de logs).

---

## 👥 3. Papéis de Usuário (Roles & Permissões)

Os usuários do sistema possuem papéis mapeados a partir de `TipoUsuario`:
- `MOTORISTA`: Responsável por relatórios de viagem, upload de documentos e consultas do seu escopo.
- `MANOBRISTA`: Apoio operacional e navegação de pátio/documentos.
- `ANALISTA`: Gestão e validação de relatórios e atualização de documentos.
- `PECUARISTA`: Produtor rural que consulta viagens e documentos associados às suas fazendas.
- `CURRALEIRO`: Operador de curral/recebimento de animais.
- `FUNCIONARIO_FRIBOI`: Usuário corporativo com acesso a edições e gestão.
- `ADMIN`: Administrador geral do sistema.

---

## 🗺️ 4. Mapa Completo dos Endpoints

### 🟢 4.1 Status e Saúde da Aplicação (`PÚBLICO`)

#### `GET /api/v1/status`
- **O que faz:** Retorna o nome da aplicação, estado ("UP") e versão da release.
- **Autenticação:** Não exige token.
- **Exemplo de Resposta (200 OK):**
```json
{
  "nome": "efficientia",
  "status": "UP",
  "versao": "1.0.0"
}
```

#### `GET /actuator/health`
- **O que faz:** Endpoint de verificação de saúde do Spring Actuator (status da conexão com banco Supabase, storage privado, disk space, etc.).
- **Autenticação:** Não exige token.
- **Exemplo de Resposta (200 OK):**
```json
{
  "status": "UP",
  "components": {
    "db": { "status": "UP" },
    "privateStorage": { "status": "UP" }
  }
}
```

---

### 🔐 4.2 Autenticação (`/api/v1/auth`) (`PÚBLICO`)

#### `POST /api/v1/auth/signup`
- **O que faz:** Cadastra um novo usuário no sistema.
- **Autenticação:** Não exige token.
- **Corpo da Requisição (JSON):**
```json
{
  "tipo": "motorista",
  "cpf": "12345678901",
  "codigoInterno": "EMP-100",
  "nome": "João da Silva",
  "dataNascimento": "1990-05-15",
  "email": "joao.silva@exemplo.com",
  "telefone": "11999998888",
  "senha": "senhaSegura123"
}
```
- **Resposta (201 Created):** Retorna os dados do usuário cadastrado (sem a senha).

#### `POST /api/v1/auth/login`
- **O que faz:** Autentica o usuário e retorna o Token JWT para uso no aplicativo mobile ou web.
- **Autenticação:** Não exige token.
- **Corpo da Requisição (JSON):**
```json
{
  "cpf": "12345678901",
  "email": "joao.silva@exemplo.com",
  "senha": "senhaSegura123",
  "codigoEmpresa": "EMP-100"
}
```
- **Exemplo de Resposta (200 OK):**
```json
{
  "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "usuario": {
    "id": 1,
    "tipo": "motorista",
    "cpf": "12345678901",
    "codigoInterno": "EMP-100",
    "nome": "João da Silva",
    "email": "joao.silva@exemplo.com",
    "ativo": true
  }
}
```
- **Erros Possíveis:** `401 Unauthorized` (`{"title": "Falha na autenticação", "detail": "Credenciais inválidas."}`).

#### `POST /api/v1/empresas` (ou `POST /api/v1/auth/empresas`) — Cadastro de Empresas
- **O que faz:** Cadastra uma nova empresa parceira e gera automaticamente o **código corporativo de 8 dígitos** (3 letras iniciais + 5 números aleatórios, ex: `FRI48291`). **Obrigatoriedade:** Logo após este cadastro, o frontend deve direcionar obrigatoriamente para o cadastro do primeiro administrador.
- **Autenticação:** Pública (`permitAll()`).
- **Corpo da Requisição (JSON):**
```json
{
  "nomeEmpresa": "Friboi Alimentos",
  "razaoSocial": "JBS S.A.",
  "cnpj": "12.345.678/0001-95",
  "emailCorporativo": "contato@friboi.com.br",
  "senha": "senhaSeguraOpcional123",
  "enderecoId": 1
}
```
- **Exemplo de Resposta (201 Created):**
```json
{
  "id": 1,
  "codigoEmpresa": "FRI48291",
  "codigo": "FRI48291",
  "codigoInterno": "FRI48291",
  "nomeEmpresa": "Friboi Alimentos",
  "razaoSocial": "JBS S.A.",
  "cnpj": "12345678000195",
  "emailCorporativo": "contato@friboi.com.br",
  "status": "PENDENTE_PRIMEIRO_ADMIN",
  "requerPrimeiroAdmin": true,
  "proximoPasso": "CADASTRO_PRIMEIRO_ADMIN",
  "mensagem": "Empresa registrada com sucesso. O cadastro do primeiro administrador é obrigatório para liberar o acesso ao sistema.",
  "criadoEm": "2026-09-24T14:30:00Z"
}
```

#### `POST /api/v1/auth/empresa/login` — Login / Verificação de Status da Empresa
- **O que faz:** Permite identificar a empresa por CNPJ, e-mail empresarial ou código de acesso. Se a empresa não possuir administrador cadastrado, sinaliza `requerPrimeiroAdmin: true` para direcionar à esteira obrigatória. Se credenciais corporativas forem fornecidas, autentica o acesso.
- **Autenticação:** Pública (`permitAll()`).

#### `POST /api/v1/auth/adm/primeiro-acesso` — Cadastro Obrigatório do Primeiro Administrador
- **O que faz:** Registra o primeiro gestor da empresa imediatamente após a criação corporativa e entrega o token JWT com acesso liberado.
- **Autenticação:** Pública (`permitAll()`).
- **Regra:** Bloqueado (`403 Forbidden`) se a empresa já possuir qualquer administrador cadastrado.

#### `POST /api/v1/auth/adm/login` — Login de Administrador por Empresa
- **O que faz:** Autentica o gestor por e-mail ou CPF e senha, retornando token JWT com autoridades `ROLE_ADMIN` e `ROLE_ADMINISTRADOR`.
- **Autenticação:** Pública (`permitAll()`).

#### `POST /api/v1/empresas/{empresaId}/adms` — Adesão de Novos Administradores
- **O que faz:** Cadastra novos administradores vinculados à mesma empresa.
- **Autenticação:** Protegida (`hasRole("ADMIN")`).

#### `POST /api/v1/empresas/{empresaId}/funcionarios` — Cadastro de Funcionários da Empresa
- **O que faz:** Cadastra motoristas, manobristas, analistas, curraleiros ou pecuaristas associados ao código da empresa.
- **Autenticação:** Protegida (`hasRole("ADMIN")`).

#### `GET /api/v1/empresas/{empresaId}/funcionarios` — Listagem de Funcionários da Empresa
- **O que faz:** Lista os funcionários da empresa parceira.
- **Autenticação:** Protegida (`hasRole("ADMIN")`).

### 📋 4.3 Cadastros Base (`/api/v1`) (`PROTEGIDO`)

Todos estes endpoints exigem `Authorization: Bearer <token>`:

- `POST /api/v1/usuarios`: Cadastra usuários operacionais ou administrativos.
- `POST /api/v1/enderecos`: Cadastra endereço (logradouro, número, cidade, UF, CEP).
- `POST /api/v1/fazendas`: Cadastra fazenda de origem/destino.
- `POST /api/v1/veiculos/cavalos`: Cadastra trator/cavalo mecânico (placa, modelo, frota).
- `POST /api/v1/veiculos/carretas`: Cadastra carreta/semirreboque (placa, tipo de piso, número de eixos).


#### 🖋️ 4.3.1 Assinatura Fixa do Motorista como Imagem PNG (`/api/v1/usuarios`) (`PROTEGIDO`)

Endpoints dedicados à assinatura fixa de perfil do motorista, armazenada como imagem PNG transparente em banco (`BYTEA`) com validação de magic bytes, SHA-256 e versionamento imutável.

##### `PUT /api/v1/usuarios/me/assinatura` — Cadastro / Atualização pelo próprio Motorista
- **O que faz:** Cadastra ou substitui a assinatura fixa do motorista logado (obtido via JWT).
- **Autenticação:** Protegida (`hasRole("ROLE_MOTORISTA")`).
- **Headers:** `Idempotency-Key: <UUID>` (obrigatório).
- **Formato:** `multipart/form-data` com parte `arquivo` (PNG até 1 MB) e `metadados` (JSON com `modalidade`: `DESENHO` ou `NOME_DIGITADO`, `textoOrigem`).
- **Status:** `200 OK`, `400 Bad Request`, `401 Unauthorized`, `403 Forbidden`, `413 Payload Too Large`, `415 Unsupported Media Type`.

##### `GET /api/v1/usuarios/me/assinatura` — Metadados da Assinatura do Motorista Logado
- **O que faz:** Retorna os metadados da assinatura ativa (`id`, `usuarioId`, `modalidade`, `sha256`, `conteudoUrl`, `atualizadoEm`, `versao`).
- **Autenticação:** Protegida (`hasRole("ROLE_MOTORISTA")`).
- **Status:** `200 OK`, `404 Not Found` (se ainda não cadastrou).

##### `GET /api/v1/usuarios/me/assinatura/conteudo` — Transmissão do PNG do Motorista Logado
- **O que faz:** Transmite os bytes binários do PNG para exibição no app mobile.
- **Headers de Resposta:** `Content-Type: image/png`, `Content-Disposition: inline; filename="assinatura.png"`, `Cache-Control: private, no-store`.
- **Autenticação:** Protegida (`hasRole("ROLE_MOTORISTA")`).

##### `PUT /api/v1/usuarios/{motoristaId}/assinatura` — Cadastro Administrativo Prévio
- **O que faz:** Permite que administradores cadastrem a assinatura fixa no perfil de um motorista antes mesmo do primeiro login dele no app.
- **Autenticação:** Protegida (`hasAnyRole("ROLE_ADMIN", "ROLE_ADMINISTRADOR")`).
- **Status:** `200 OK`, `422 Unprocessable Entity` (se usuário alvo não for motorista).

##### `GET /api/v1/usuarios/{usuarioId}/assinatura` — Consulta de Assinatura por Gestores Web
- **O que faz:** Retorna os metadados da assinatura ativa de qualquer motorista para visualização em relatórios/formulários web.
- **Autenticação:** Protegida (`hasAnyRole("ROLE_ADMIN", "ROLE_ADMINISTRADOR", "ROLE_ANALISTA", "ROLE_FUNCIONARIO_FRIBOI")`).

##### `GET /api/v1/usuarios/{usuarioId}/assinatura/conteudo` — Transmissão de Imagem para a Web
- **O que faz:** Transmite os bytes PNG da assinatura ativa de um motorista com cabeçalho `inline` para renderização em formulários do portal web.
- **Autenticação:** Protegida (`hasAnyRole("ROLE_ADMIN", "ROLE_ADMINISTRADOR", "ROLE_ANALISTA", "ROLE_FUNCIONARIO_FRIBOI")`).
---

### 🚛 4.4 Relatórios de Viagem (`/api/v1/relatorios-viagem`) (`PROTEGIDO`)

#### `POST /api/v1/relatorios-viagem`
- **O que faz:** Registra um novo relatório de transporte animal (GTA, quantidade de animais, km inicial/final, avarias, etc.).
- **Resposta (201 Created):** Retorna o relatório criado com ID numérico.

#### `GET /api/v1/relatorios-viagem`
- **O que faz:** Lista os relatórios de viagem cadastrados.
- **Parâmetros de Consulta:**
  - `pagina`: Número da página (padrão: `0`).
  - `tamanho`: Quantidade de itens por página (padrão: `20`).
- **Exemplo:** `GET /api/v1/relatorios-viagem?pagina=0&tamanho=10`

#### `GET /api/v1/relatorios-viagem/{id}`
- **O que faz:** Retorna os detalhes completos de um relatório específico pelo seu ID numérico.

#### `GET /api/v1/relatorios-viagem/{id}/caminhao`
- **O que faz:** Retorna os detalhes do caminhão (cavalo mecânico e carreta/boiadeira) alocado no relatório de viagem, incluindo identificação do motorista responsável e status de uso.
- **Resposta (200 OK):** Objeto `CaminhaoRelatorioResponse`.

#### `POST /api/v1/relatorios-viagem/{id}/vincular-caminhao`
- **O que faz:** Vincula um caminhão ao relatório informando `placaCavalo` e `placaCarreta` (ou IDs). Valida automaticamente se as inspeções veiculares estão em dia (`fn_validar_alocacao_viagem`).
- **Corpo (JSON):** `{"placaCavalo": "ABC1D23", "placaCarreta": "XYZ9W87"}`

#### `PATCH /api/v1/relatorios-viagem/{id}/finalizar`
- **O que faz:** Finaliza o relatório de viagem (status `aprovado`), desocupando automaticamente o caminhão e a carreta para que fiquem imediatamente disponíveis para outros motoristas da frota.
- **Resposta (200 OK):** Relatório atualizado com timestamp `finalizadoEm`.

#### `PATCH /api/v1/relatorios-viagem/{id}/status?status={novoStatus}`
- **O que faz:** Atualiza o estado da viagem no ciclo de vida (`rascunho`, `pendente`, `aprovado`, `concluido`, `reprovado`).

---

### 📄 4.5 Gerenciamento de Documentos e Anexos (`/api/v1/documentos`) (`PROTEGIDO`)

#### `POST /api/v1/documentos` — (Upload de PDF/PNG)
- **O que faz:** Envia um arquivo físico (PDF ou PNG) associado a uma viagem.
- **Formato:** `multipart/form-data`
- **Headers Exigidos:** `Authorization: Bearer <token>` + `Idempotency-Key: <UUID>`
- **Partes do Form:**
  - `metadados`: JSON do tipo `DocumentoMetadataRequest` (ex: `{"viagemId": 10, "tipoDocumento": "COMPROVANTE_DESPESA", ...}`)
  - `arquivo`: Arquivo binário (`.pdf` ou `.png`)
- **Resposta (201 Created):** Retorna o registro do documento criado contendo o UUID do documento.

#### `POST /api/v1/documentos` — (Assinatura Textual)
- **O que faz:** Registra uma assinatura digital acessível em texto.
- **Formato:** `application/json`
- **Headers Exigidos:** `Idempotency-Key: <UUID>`

#### `GET /api/v1/documentos`
- **O que faz:** Consulta lista de documentos com paginação ou cursor.
- **Filtros Disponíveis:**
  - `viagemId`: ID do relatório de viagem.
  - `tipoDocumento`: `GTA`, `COMPROVANTE_DESPESA`, `TICKET_PESAGEM`, etc.
  - `criadoDe` / `criadoAte`: Intervalo de datas ISO-8601 (`2026-09-01T00:00:00Z`).
  - `page` & `size` OU `cursor` (para paginação infinita opaca no mobile).

#### `GET /api/v1/documentos/{id}`
- **O que faz:** Retorna os metadados públicos do documento pelo seu UUID.

#### `GET /api/v1/documentos/{id}/conteudo`
- **O que faz:** Baixa ou pré-visualiza o arquivo armazenado no storage privado.
- **Parâmetro:** `?inline=true` (visualizar diretamente no app/browser) ou `?inline=false` (forçar download).

#### `PATCH /api/v1/documentos/{id}`
- **O que faz:** Atualiza a descrição de um documento de forma otimista.
- **Corpo:** `{"versao": 1, "descricao": "Nova descrição do documento"}`

#### `DELETE /api/v1/documentos/{id}`
- **O que faz:** Exclui os metadados do documento no banco e apaga o arquivo físico do storage.

---

### 📦 4.6 Exportações Assíncronas de Lote (`/api/v1/exportacoes`) (`PROTEGIDO`)

#### `POST /api/v1/exportacoes`
- **O que faz:** Solicita o empacotamento assíncrono de múltiplos documentos em um arquivo `.zip`.
- **Headers Exigidos:** `Authorization: Bearer <token>` + `Idempotency-Key: <UUID>`
- **Corpo (JSON):** `{"documentoIds": ["uuid-1", "uuid-2"]}`
- **Resposta (202 Accepted):** Retorna `HTTP 202` com o header `Location: /api/v1/exportacoes/{id}` e `Retry-After: 2`.

#### `GET /api/v1/exportacoes/{id}`
- **O que faz:** Consulta se o arquivo `.zip` já foi gerado.
- **Status Retornados:** `PENDENTE`, `EM_PROCESSAMENTO`, `CONCLUIDO`, `ERRO`.

#### `GET /api/v1/exportacoes/{id}/conteudo`
- **O que faz:** Faz o download do arquivo `.zip` final quando o status estiver `CONCLUIDO`.


### 🏢 4.7 Empresas Parceiras e Administradores (`/api/v1/empresas` e `/api/v1/auth/adm`)

#### `POST /api/v1/empresas` (e alias `/api/v1/auth/empresas`) (`PÚBLICO`)
- **O que faz:** Cadastra nova empresa parceira e gera automaticamente o código corporativo de 8 dígitos (`codigoEmpresa`, ex: `FRI48291`). Senha é opcional.
- **Campos obrigatórios:** `nomeEmpresa`, `cnpj` (14 dígitos), `emailCorporativo`.

#### `POST /api/v1/auth/adm/primeiro-acesso` (`PÚBLICO`)
- **O que faz:** Cadastra o primeiro administrador da empresa imediatamente após a criação corporativa e entrega o token JWT com perfil `ROLE_ADMIN` liberado.
- **Regra:** Bloqueado (`403 Forbidden`) se a empresa já possuir qualquer administrador cadastrado.

#### `POST /api/v1/auth/adm/login` (`PÚBLICO`)
- **O que faz:** Autentica o administrador por e-mail (ou CPF) e senha, retornando o token JWT Bearer com perfil `ROLE_ADMIN`.

#### `POST /api/v1/empresas/{empresaId}/adms` (`PROTEGIDO - ROLE_ADMIN`)
- **O que faz:** Adesão de novos administradores à empresa, restrita a administradores autenticados da mesma empresa.

#### `GET /api/v1/empresas/{empresaId}/adms` (`PROTEGIDO - ROLE_ADMIN`)
- **O que faz:** Lista todos os administradores vinculados à empresa (organograma corporativo/RH).

#### `PUT /api/v1/empresas/{id}` (e alias `/api/v1/empresas/{id}/dados-complementares`, `/api/v1/empresas/{id}/etapa-1`) — Etapa 1 de 3 (Dados da Empresa)
- **O que faz:** Salva e complementa as informações da empresa após o cadastro inicial e login do 1º administrador (CNPJ, Razão Social, Nome Fantasia, E-mail Corporativo, Telefone, Endereço completo: CEP, Logradouro, Número, Cidade, UF e URL de logo). Avança o cadastro para a etapa 2.
- **Autenticação:** Permite acesso durante onboarding ou com Bearer Token de `ROLE_ADMIN`.
- **Também disponível por código:** `PUT /api/v1/empresas/codigo/{codigo}`
- **Exemplo de Corpo (JSON):**
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
  "estado": "MS"
}
```

#### `POST /api/v1/empresas/{id}/logo` — Upload de Logotipo da Empresa
- **O que faz:** Faz upload do logotipo da empresa no formato PNG ou SVG (tamanho máximo de até 5 MB).
- **Content-Type:** `multipart/form-data` (campo `arquivo` ou `file`)
- **Resposta (200 OK):**
```json
{
  "logoUrl": "/api/v1/empresas/1/logo/conteudo",
  "mensagem": "Logo da empresa enviada com sucesso.",
  "tamanhoBytes": 1048576,
  "mimeType": "image/png"
}
```

#### `GET /api/v1/empresas/{id}/logo/conteudo` — Visualização / Download do Logotipo
- **O que faz:** Retorna o binário da imagem com o cabeçalho `Content-Type` adequado (`image/png` ou `image/svg+xml`).
- **Autenticação:** Pública (`permitAll()`) para exibição direta em tags `<img src="...">` ou no app mobile.

#### `GET /api/v1/empresas/codigo/{codigo}` (`PÚBLICO`)
- **O que faz:** Consulta dados públicos da empresa através do código de 8 dígitos para validação no app mobile.


---

### 🚛 4.8 Gestão de Frota e Caminhões (`/api/v1/caminhoes` e `/api/v1/veiculos`) (`PROTEGIDO`)

Permite o cadastro e gestão independente de caminhões pelo **Portal Web** e a consulta operacional e alocação pelo **Aplicativo Mobile**.

#### `POST /api/v1/caminhoes` — Cadastro de Caminhão (Web)
- **O que faz:** Cadastra um novo veículo ou conjunto completo de caminhão.
- **Tipos Suportados:**
  - `CAVALO`: Cadastra o cavalo mecânico (trator/cabine). Campos: `placa`, `kmAcumulado`, `marca`, `modelo`, `anoFabricacao`, `dataVencimentoInspecao`.
  - `CARRETA`: Cadastra o implemento / gaiola boiadeira. Campos: `placa`, `capacidadeCabecas` (obrigatório e > 0), `marca`, `modelo`, `tipoCarreta`, `dataVencimentoInspecao`.
  - `CONJUNTO`: Cadastra simultaneamente cavalo mecânico (`placa`) e carreta (`placaCarreta`).

#### `GET /api/v1/caminhoes` — Listagem Geral com Status de Alocação
- **O que faz:** Lista todos os caminhões e carretas da empresa, indicando dinamicamente se cada veículo está `DISPONIVEL` ou `EM_USO` em algum relatório de viagem ativo, além do ID do relatório e nome do motorista em trânsito.
- **Parâmetros Opcionais:** `?empresaId=1&ativo=true&tipo=TODOS` (`CAVALO` ou `CARRETA`).

#### `GET /api/v1/caminhoes/app` — Listagem Otimizada para o Aplicativo Mobile
- **O que faz:** Retorna a frota formatada para exibição no smartphone do motorista, com status de inspeção sanitária/mecânica (`inspecaoValida`), dias restantes para o vencimento (`diasParaVencerInspecao`) e indicador de disponibilidade.

#### `GET /api/v1/caminhoes/disponiveis` — Consulta de Caminhões Livres para Viagem
- **O que faz:** Filtra e retorna apenas caminhões ativos, com inspeção válida e que **não estejam alocados** em relatórios de viagem em andamento.

#### `GET /api/v1/caminhoes/relatorio/{relatorioId}` — Caminhão do Relatório
- **O que faz:** Retorna o cavalo mecânico e a carreta associados a um relatório específico.

#### `GET /api/v1/caminhoes/motorista/{motoristaId}` — Caminhão Ativo do Motorista
- **O que faz:** Localiza a viagem em andamento do motorista e retorna o caminhão que ele está conduzindo.

#### `POST /api/v1/caminhoes/relatorio/{relatorioId}/vincular` — Alocação de Caminhão por Placa
- **O que faz:** O motorista digita a placa do cavalo e da carreta no aplicativo mobile para atrelar o veículo ao seu relatório de transporte.
- **Bloqueio:** Retorna `422 Unprocessable Entity` se qualquer um dos veículos estiver com inspeção vencida.

#### `PUT /api/v1/caminhoes/{id}` e `DELETE /api/v1/caminhoes/{id}`
- **O que faz:** Edição e exclusão/desativação de caminhões no portal administrativo.

---

### 🔄 4.9 Fluxo Completo de Onboarding, Frota e Operação Web & Mobile


1. **Criação da Empresa (Portal Web):**
   - Usuário gestor envia razão social, nome, CNPJ e e-mail via `POST /api/v1/empresas`.
   - API gera código corporativo único de 8 dígitos (ex: `FRI48291`) e retorna `requerPrimeiroAdmin: true`.

2. **Primeiro Acesso do Administrador (Portal Web):**
   - Usuário preenche seus dados pessoais e senha via `POST /api/v1/auth/adm/primeiro-acesso` usando o código da empresa.
   - API libera o acesso imediato com token JWT contendo `ROLE_ADMIN`.

3. **Etapa 1 de 3 - Dados Complementares e Logo (Portal Web):**
   - Usuário autenticado preenche telefone, endereço completo e faz upload do logotipo PNG/SVG (até 5 MB) via `PUT /api/v1/empresas/{id}/dados-complementares` e `POST /api/v1/empresas/{id}/logo`.
   - O botão **Salvar e continuar** avança a empresa para `etapaCadastro: 2`.

4. **Pré-Login de Funcionários pelo Administrador (Portal Web):**
   - O administrador cadastra motoristas e analistas via `POST /api/v1/empresas/{empresaId}/funcionarios`.
   - A API associa o usuário diretamente ao tenant da empresa através do `codigoInterno`.

5. **Acesso do Motorista no Aplicativo Mobile (App Mobile):**
   - O motorista faz login no aplicativo mobile (`POST /api/v1/auth/login`) informando CPF/e-mail, senha e código da empresa.
   - Preenche os dados de viagem (`POST /api/v1/relatorios-viagem`), assina digitalmente e envia comprovantes (`POST /api/v1/documentos`).

6. **Auditoria e Download de Documentos pelo Analista (Portal Web):**
   - O analista consulta os relatórios de viagem e documentos enviados pelo mobile.
   - Realiza download direto (`GET /api/v1/documentos/{id}/conteudo`) ou solicita exportação assíncrona em `.zip` (`POST /api/v1/exportacoes`).

## 🛠️ 5. Exemplos de Implementação

### Exemplo no App Mobile (Flutter / Dart)
```dart
import 'package:http/http.dart' as http;

final baseUrl = 'https://efficientia-api.onrender.com';

Future<void> fetchRelatorios(String token) async {
  final response = await http.get(
    Uri.parse('$baseUrl/api/v1/relatorios-viagem?pagina=0&tamanho=20'),
    headers: {
      'Content-Type': 'application/json',
      'Authorization': 'Bearer $token',
    },
  );

  if (response.statusCode == 200) {
    print('Sucesso: ${response.body}');
  } else if (response.statusCode == 401) {
    print('Sessão expirada. Faça login novamente.');
  }
}
```

### Exemplo na Aplicação Web (JavaScript / Axios)
```javascript
import axios from 'axios';

const api = axios.create({
  baseURL: 'https://efficientia-api.onrender.com',
});

// Interceptor para injetar o token JWT
api.interceptors.request.use((config) => {
  const token = localStorage.getItem('jwt_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

// Exemplo de busca de documento
export const getDocumentoMetadata = async (documentoId) => {
  const response = await api.get(`/api/v1/documentos/${documentoId}`);
  return response.data;
};
```
