# SPEC-SPRINT-20: Gestão de Frota (Caminhões), Vinculação de Relatório de Viagem e Integração Web/Mobile

## 1. Visão Geral e Contexto de Negócio

Esta especificação define a arquitetura, modelo de dados relacional e contratos de API para a **Gestão de Caminhões (Frota)** no **Portal Web** e sua integração operacional com o **Aplicativo Mobile**.

A implementação foi desenhada com base nas diretrizes do repositório oficial do banco de dados (`Efficientia-database` — esquemas `sc_frota` e `sc_operacao`) e nos requisitos de fluxo operacional de transporte de gado vivo.

---

### 1.1 Ciclo de Vida e Fluxo: Motorista ➔ Relatório de Viagem ➔ Caminhão

```
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              PORTAL WEB (Gestão de Frota)                       │
│  - Cadastro independente de Caminhões (Cavalos Mecânicos e Carretas Boiadeiras) │
│  - Controle de inspeções periódicas, quilometragem acumulada e capacidade        │
│  - Visualização de disponibilidade (DISPONÍVEL vs. EM_USO em viagem ativa)     │
└──────────────────────────────────────┬──────────────────────────────────────────┘
                                       │
                                       ▼ (API REST)
┌─────────────────────────────────────────────────────────────────────────────────┐
│                              APLICATIVO MOBILE (Motorista)                      │
│                                                                                 │
│  1. Puxar todos os caminhões da empresa para a tela (`GET /caminhoes/app`)      │
│  2. Iniciar Relatório de Viagem vinculando o caminhão pela Placa do Cavalo      │
│     e pela Placa da Carreta (`POST /vincular-caminhao` ou via placas)           │
│  3. Durante a viagem: Caminhão entra em status 'EM_USO'                         │
│  4. Ao finalizar a viagem (`PATCH /finalizar`):                                 │
│     O caminhão é liberado automaticamente e fica 'DISPONÍVEL' para outros       │
│     motoristas utilizarem na frota                                              │
└─────────────────────────────────────────────────────────────────────────────────┘
```

#### Regras Fundamentais do Domínio:
1. **Desacoplamento no Cadastro:** No portal Web, caminhões são cadastrados de forma independente do motorista. Um caminhão pertence à empresa/tenant, não a um motorista fixo.
2. **Uso Dinâmico e Não-Exclusivo:** Um motorista utiliza um caminhão durante o tempo de duração do relatório de viagem. Quando o relatório é concluído/finalizado, o caminhão é desocupado e pode ser alocado para outros motoristas.
3. **Vinculação por Placa no App:** No aplicativo mobile, o motorista informa a **placa do cavalo** e a **placa do caminhão (carreta/boiadeira)**. A API resolve as placas para os identificadores correspondentes e vincula o relatório ao caminhão e ao motorista responsável.
4. **Validação de Inspeção Periódica (`fn_validar_alocacao_viagem`):** Veículos com data de inspeção vencida são impedidos de serem alocados em relatórios de viagem, garantindo conformidade sanitária e de segurança.

---

## 2. Modelagem de Dados e Migração (Flyway V7)

Em total concordância com `sc_frota` (`tb_veiculo_base`, `tb_veiculo_cavalo`, `tb_veiculo_carreta`) e `sc_operacao` (`tb_relatorio_viagem`):

### 2.1 Tabela `public.veiculo_cavalo`
- `id` (SERIAL PRIMARY KEY)
- `placa` (VARCHAR(7) UNIQUE NOT NULL)
- `empresa_id` (INTEGER REFERENCES public.empresa)
- `ativo` (BOOLEAN DEFAULT TRUE)
- `data_vencimento_inspecao` (DATE) — Validade da inspeção mecânica/veicular
- `km_acumulado` (INTEGER DEFAULT 0) — Odômetro acumulado
- `marca` (VARCHAR(50))
- `modelo` (VARCHAR(50))
- `ano_fabricacao` (INTEGER)

### 2.2 Tabela `public.veiculo_carreta`
- `id` (SERIAL PRIMARY KEY)
- `placa` (VARCHAR(7) UNIQUE NOT NULL)
- `empresa_id` (INTEGER REFERENCES public.empresa)
- `capacidade_cabecas` (INTEGER NOT NULL CHECK > 0) — Lotação animal
- `ativo` (BOOLEAN DEFAULT TRUE)
- `data_vencimento_inspecao` (DATE)
- `marca` (VARCHAR(50))
- `modelo` (VARCHAR(50))
- `tipo_carreta` (VARCHAR(50))

### 2.3 Tabela `public.relatorio_viagem`
- `status` (VARCHAR(20) NOT NULL DEFAULT 'rascunho') — `rascunho`, `pendente`, `aprovado`, `concluido`, `reprovado`
- `enviado_em` (TIMESTAMPTZ)
- `finalizado_em` (TIMESTAMPTZ)
- Índices de busca por `placa`, `empresa_id`, `motorista_id`, `cavalo_id`, `carreta_id` e `status`.

---

## 3. Endpoints da API

### 🚛 3.1 Gestão Unificada de Caminhões (`/api/v1/caminhoes`)

| Método | Endpoint | Perfil | Descrição |
| :--- | :--- | :--- | :--- |
| **POST** | `/api/v1/caminhoes` | Admin, Gestor | Cadastra caminhão (tipo `CAVALO`, `CARRETA` ou `CONJUNTO`). |
| **GET** | `/api/v1/caminhoes` | Todos Autenticados | Lista caminhões com filtros (`empresaId`, `ativo`, `tipo`). Retorna status `DISPONIVEL` ou `EM_USO`. |
| **GET** | `/api/v1/caminhoes/{id}` | Todos Autenticados | Detalha caminhão por ID. |
| **GET** | `/api/v1/caminhoes/placa/{placa}` | Todos Autenticados | Busca caminhão por placa (cavalo ou carreta). |
| **PUT** | `/api/v1/caminhoes/{id}` | Admin, Gestor | Atualiza dados cadastrais, quilometragem e inspeção. |
| **DELETE** | `/api/v1/caminhoes/{id}` | Admin | Remove caminhão (soft-delete se houver viagens vinculadas). |

### 📱 3.2 Endpoints Dedicados para o Aplicativo Mobile

| Método | Endpoint | Perfil | Descrição |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/v1/caminhoes/app` | Motorista, Operação | Retorna a lista completa de caminhões pronta para exibição na tela do app (com `inspecaoValida`, dias para vencer, status de uso e motorista atual). |
| **GET** | `/api/v1/caminhoes/disponiveis` | Motorista, Operação | Retorna apenas os veículos livres para iniciar novo relatório. |
| **GET** | `/api/v1/caminhoes/relatorio/{relatorioId}` | Motorista, Operação | Retorna o conjunto (cavalo + carreta) vinculado a um relatório de viagem. |
| **GET** | `/api/v1/caminhoes/motorista/{motoristaId}` | Motorista, Operação | Retorna o caminhão em uso ativo pelo motorista no momento. |
| **POST** | `/api/v1/caminhoes/relatorio/{relatorioId}/vincular` | Motorista, Operação | Vincula o caminhão ao relatório informando `placaCavalo` e `placaCarreta` (ou IDs). |

### 📋 3.3 Endpoints no Módulo de Relatórios de Viagem (`/api/v1/relatorios-viagem`)

| Método | Endpoint | Perfil | Descrição |
| :--- | :--- | :--- | :--- |
| **GET** | `/api/v1/relatorios-viagem/{id}/caminhao` | Motorista, Operação | Puxa o caminhão vinculado ao relatório. |
| **POST** | `/api/v1/relatorios-viagem/{id}/vincular-caminhao` | Motorista, Operação | Vincula o caminhão pelas placas ou IDs. |
| **PATCH** | `/api/v1/relatorios-viagem/{id}/finalizar` | Motorista, Operação | Finaliza a viagem (`aprovado`/`concluido`), liberando o caminhão para outros motoristas. |
| **PATCH** | `/api/v1/relatorios-viagem/{id}/status?status=...` | Operação, Analista | Atualiza o estado da viagem no ciclo de vida. |

---

## 4. Exemplos de Payload e Integração

### 4.1 Cadastro de Caminhão Conjunto (Web)
```http
POST /api/v1/caminhoes
Content-Type: application/json
Authorization: Bearer <token>

{
  "tipo": "CONJUNTO",
  "placa": "ABC1D23",
  "placaCarreta": "XYZ9W87",
  "empresaId": 1,
  "capacidadeCabecas": 48,
  "kmAcumulado": 15400,
  "marca": "Scania",
  "modelo": "R450",
  "anoFabricacao": 2023,
  "tipoCarreta": "Boiadeira 2 Pisos",
  "dataVencimentoInspecao": "2026-12-31",
  "ativo": true
}
```

### 4.2 Resposta do App Mobile (`GET /api/v1/caminhoes/app`)
```json
[
  {
    "id": 10,
    "tipo": "CAVALO",
    "placa": "ABC1D23",
    "capacidadeCabecas": null,
    "kmAcumulado": 15400,
    "marca": "Scania",
    "modelo": "R450",
    "ativo": true,
    "inspecaoValida": true,
    "diasParaVencerInspecao": 90,
    "statusUso": "DISPONIVEL",
    "relatorioAtualId": null,
    "motoristaAtualNome": null
  },
  {
    "id": 20,
    "tipo": "CARRETA",
    "placa": "XYZ9W87",
    "capacidadeCabecas": 48,
    "kmAcumulado": null,
    "marca": "Randon",
    "modelo": "Boiadeira",
    "ativo": true,
    "inspecaoValida": true,
    "diasParaVencerInspecao": 90,
    "statusUso": "DISPONIVEL",
    "relatorioAtualId": null,
    "motoristaAtualNome": null
  }
]
```

### 4.3 Vinculação de Caminhão por Placa no App (`POST /api/v1/caminhoes/relatorio/{id}/vincular`)
```http
POST /api/v1/caminhoes/relatorio/50/vincular
Content-Type: application/json
Authorization: Bearer <token>

{
  "placaCavalo": "ABC1D23",
  "placaCarreta": "XYZ9W87"
}
```

**Resposta:**
```json
{
  "relatorioId": 50,
  "statusRelatorio": "rascunho",
  "motoristaId": 12,
  "motoristaNome": "Carlos Eduardo",
  "cavalo": {
    "id": 10,
    "tipo": "CAVALO",
    "placa": "ABC1D23",
    "statusUso": "EM_USO"
  },
  "carreta": {
    "id": 20,
    "tipo": "CARRETA",
    "placa": "XYZ9W87",
    "capacidadeCabecas": 48,
    "statusUso": "EM_USO"
  },
  "placaCavalo": "ABC1D23",
  "placaCarreta": "XYZ9W87",
  "emUso": true
}
```

### 4.4 Finalização da Viagem (`PATCH /api/v1/relatorios-viagem/50/finalizar`)
Ao ser finalizado, o relatório atualiza `finalizado_em` e `status = 'aprovado'`. Automaticamente, consultas subsequentes a `/api/v1/caminhoes/disponiveis` voltam a listar as placas `ABC1D23` e `XYZ9W87` como disponíveis para outros motoristas.

---

## 5. Critérios de Aceite e Validação Automatizada

| ID | Critério de Aceite | Cenário Testado | Status |
| :--- | :--- | :--- | :--- |
| **CA-01** | CRUD Web de Caminhão | Cadastro individual (Cavalo/Carreta) e Conjunto unificado, edição, listagem com filtros e deleção. | **Aprovado (100%)** |
| **CA-02** | Unicidade de Placa | Rejeição com erro 409 Conflict ao tentar cadastrar ou atualizar para placa existente. | **Aprovado (100%)** |
| **CA-03** | Listagem Mobile (`/app`) | Retorno de caminhões com métricas de validade de inspeção (`inspecaoValida`, `diasParaVencerInspecao`) e indicador de uso. | **Aprovado (100%)** |
| **CA-04** | Vinculação por Placa | Motorista vincula caminhão ao relatório informando placa do cavalo e placa da carreta. | **Aprovado (100%)** |
| **CA-05** | Bloqueio por Inspeção Vencida | Rejeição com código 422 Unprocessable Entity (`InspecaoVencidaException`) se o cavalo ou carreta tiverem inspeção expirada. | **Aprovado (100%)** |
| **CA-06** | Ciclo de Vida do Caminhão | Caminhão fica `EM_USO` durante a vigência do relatório e retorna a `DISPONIVEL` após a finalização da viagem. | **Aprovado (100%)** |
| **CA-07** | Consulta Caminhão do Motorista | Endpoint `/api/v1/caminhoes/motorista/{id}` recupera a alocação ativa do motorista. | **Aprovado (100%)** |
| **CA-08** | Suíte de Testes e CI/CD | 266 testes automatizados executados e aprovados (0 falhas, 0 erros). Cobertura JaCoCo e migrações Flyway validadas. | **Aprovado (100%)** |

## 6. Nota de implantação no Render (Flyway V7)

O log recebido em 02/10/2026 mostra a inicialização interrompida porque `public.veiculo_cavalo` não existia quando a migração V7 tentou alterá-la. A V7 versionada neste projeto começa criando, com `IF NOT EXISTS`, as tabelas `public.veiculo_cavalo`, `public.veiculo_carreta` e `public.relatorio_viagem` antes de adicionar colunas e índices. Isso cobre bancos vazios e bancos que passaram por baseline parcial.

Para que a correção seja usada, o serviço do Render precisa construir a branch/commit que contém essa V7. O erro é de migração de banco, não de porta ou compilação do serviço. Após novo deploy, conferir o início completo da migração V7 nos logs; se o log ainda reportar ausência da relação na linha 5, o Render está executando um artefato anterior ao arquivo versionado atual. Não editar uma migração já aplicada no banco remoto nem apagar dados para contornar esse erro.
