# SPEC-SPRINT-22: Integração do diário de rota mobile

## Contexto do projeto (TAP)

O Termo de Abertura descreve o Efficientia como a digitalização do diário de transporte de bovinos da fazenda ao frigorífico. O motorista registra percurso, ocorrências, animais e assinaturas; a solução busca reduzir erros de transcrição e papel, facilitar a operação em campo e preservar dados para auditoria e relatórios. Esta entrega completa o contrato entre as seis etapas do app e a API existente.

O TAP é referência de contexto e objetivo do produto. Os requisitos de implementação desta especificação vêm do relatório de integração fornecido pelo usuário e da inspeção do código da API.

## Escopo

- Persistir e devolver dados do formulário nos endpoints de relatório.
- Permitir criação progressiva como rascunho e retomada por atualização.
- Submeter um relatório completo para análise com chave de idempotência.
- Derivar motorista e empresa do JWT quando o motorista autenticado usa o app.
- Calcular no servidor duração e distância quando houver dados suficientes.
- Validar coerência, erros por campo, paradas, anomalias e assinaturas.
- Expor o contrato no Swagger/OpenAPI e manter este documento, o mapa da API e o diagrama de banco alinhados.

## Contrato HTTP

### `POST /api/v1/relatorios-viagem`

Cria um diário. Sem `status`, ou com `status: "rascunho"`, salva uma etapa parcial. `status: "pendente"` envia para análise e exige formulário completo, as quatro assinaturas e header `Idempotency-Key` com UUID. A repetição da mesma chave retorna o mesmo relatório.

Exemplo de rascunho:

```http
POST /api/v1/relatorios-viagem
Authorization: Bearer <jwt>
Content-Type: application/json

{
  "fazendaId": 12,
  "numeroGta": "GTA-2026-0042",
  "status": "rascunho"
}
```

Exemplo de submissão (abreviado):

```http
POST /api/v1/relatorios-viagem
Authorization: Bearer <jwt>
Idempotency-Key: 3b241101-e2bb-4255-8caf-4136c566a962
Content-Type: application/json

{
  "fazendaId": 12,
  "unidadeFrigorificaId": 4,
  "numeroGta": "GTA-2026-0042",
  "numeroNotaFiscal": "NF-873",
  "dataEmbarque": "2026-10-04",
  "horarioEmbarque": "08:00:00",
  "horarioSaidaPropriedade": "08:30:00",
  "kmSaidaEmbarcadouro": 18200,
  "dataChegadaUnidade": "2026-10-04",
  "horarioChegadaUnidade": "11:15:00",
  "horarioDesembarque": "11:40:00",
  "kmChegadaDesembarcadouro": 18325,
  "numeroCurral": "C-07",
  "sireneReFuncionou": true,
  "quantidadeMachos": 20,
  "quantidadeFemeas": 15,
  "quantidadeMarrucos": 5,
  "quantidadeEmPe": 38,
  "quantidadeDeitado": 1,
  "quantidadeMorto": 1,
  "quantidadeEmergencia": 0,
  "motivoEmergencia": null,
  "comentarios": "Sem intercorrências adicionais.",
  "urlAssinaturaPecuarista": "/api/v1/assinaturas/pecuarista/91/conteudo",
  "urlAssinaturaManobrista": "/api/v1/assinaturas/manobrista/24/conteudo",
  "urlAssinaturaCurraleiro": "/api/v1/assinaturas/curraleiro/35/conteudo",
  "paradasImprevistas": [],
  "anomaliasEmbarque": [],
  "anomaliasDesembarque": [],
  "status": "pendente"
}
```

O aplicativo não envia `motoristaId` ou `empresaId` como fonte de autoridade. O motorista é obtido pelo subject/claim do JWT e a empresa pelo claim `empresa_id` ou vínculo persistido do usuário. `urlAssinaturaMotorista` é ignorada: a API busca a assinatura fixa da conta e salva a referência no relatório como cópia daquele lançamento.

### `PUT /api/v1/relatorios-viagem/{id}`

Atualiza os campos recebidos e mantém os valores omitidos, permitindo retomar o rascunho. Listas de paradas e anomalias são substituídas quando presentes no corpo. Para transição a `pendente`, enviar o formulário completo e `Idempotency-Key` (exceto quando o próprio relatório já tiver uma chave gravada).

### Consulta

- `GET /api/v1/relatorios-viagem` lista registros com paginação (`pagina`, `tamanho`).
- `GET /api/v1/relatorios-viagem/{id}` devolve todos os campos, inclusive paradas, anomalias e assinaturas.
- `PATCH /api/v1/relatorios-viagem/{id}/enviar`, `PATCH /{id}/status?status=pendente` e `PATCH /{id}/finalizar` aplicam as mesmas exigências de completude, assinatura e idempotência para transições finais.
- A resposta inclui `status`, `criadoEm`, `atualizadoEm`, `enviadoEm`, `finalizadoEm`, `duracaoViagemMinutos` e `distanciaPercorridaKm` calculados ou registrados pela API.

## Dados do diário

| Grupo | Campos persistidos |
| --- | --- |
| Documentos e locais | `numeroGta`, `numeroNotaFiscal`, `fazendaId`, `unidadeFrigorificaId` |
| Embarque e chegada | Datas e horários, km inicial/final, horário de desembarque, curral e sirene de ré |
| Animais | Quantidades de machos, fêmeas, marrucos, em pé, deitados, mortos e em emergência |
| Ocorrências | Motivo de emergência, comentários, `paradasImprevistas` com motivo/início/fim e listas de anomalias por etapa com animais envolvidos |
| Assinaturas | Referências do pecuarista, motorista, manobrista e curraleiro; a do motorista é capturada da conta |
| Metadados | IDs de rota/veículos, motorista/empresa, status, datas de auditoria, chave de idempotência, duração e distância |

## Validações e erros

- Datas/horários devem respeitar embarque, saída, chegada e desembarque; a quilometragem final não pode ser menor que a inicial.
- Valores de quilometragem e contagens não podem ser negativos.
- Na submissão, os totais por categoria devem ser informados e maiores que zero; a soma por condição física deve corresponder ao total transportado. Emergência exige motivo.
- GTA deve ser única. Status aceitos: `rascunho`, `pendente`, `aprovado`, `concluido` e `reprovado`.
- A submissão para análise ou finalização requer as quatro assinaturas.
- Erros de validação retornam `application/problem+json` com `fieldErrors`, um mapa campo/mensagem utilizável pelo app.

Exemplo:

```json
{
  "type": "about:blank",
  "title": "Inconsistência no Diário de Rota",
  "status": 400,
  "detail": "Inconsistência nos dados do diário de rota.",
  "fieldErrors": {
    "kmChegadaDesembarcadouro": "O quilômetro de chegada não pode ser inferior ao quilômetro de saída."
  }
}
```

## Persistência e implantação

As listas de paradas e anomalias são persistidas em tabelas próprias e devolvidas em consultas. O migration V9 adiciona destino, auditoria de atualização, chave de idempotência e quantidade de animais das anomalias. A chave recebe índice único parcial para permitir vários rascunhos sem chave e impedir reutilização entre relatórios.

O erro do Render registrado em 02/10/2026 (`relation "public.veiculo_cavalo" does not exist` durante V7) ocorre quando o serviço inicia com um artefato que não cria a tabela antes de alterá-la. A V7 desta branch contém a criação idempotente das tabelas de cavalo, carreta e relatório no início. A publicação deve construir esta branch/commit; os logs precisam refletir o script versionado atual.

## Critérios de aceite

- As seis etapas podem salvar e retomar dados sem perder os campos já enviados.
- Uma submissão completa gera um único relatório mesmo com repetição da requisição.
- Respostas de criação, atualização e consulta contêm todos os grupos do formulário e metadados calculados.
- O cliente não consegue atribuir o relatório a outro motorista/empresa nem trocar a referência da assinatura fixa do motorista.
- Dados incoerentes retornam erros por campo no mesmo formato.
- Swagger/OpenAPI apresenta endpoints, campos e exemplos de request/response.
