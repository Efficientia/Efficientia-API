# HTTP 400 no fluxo do diário de rota — 15/10/2026

## Diagnóstico

A base desta correção é a `main` no commit `d938d1738032c26e5ed7b4440c8d78476dc3510b`, que já contém a PR #43 e a migration V15. Os campos `nomeOrigem`/`nomeDestino`, locais digitados sem IDs e rascunhos parciais já estão nessa base. GTA alfanumérica, data ISO e horários com segundos são aceitos.

O PUT preservava campos omitidos na persistência, mas validava a completude somente no corpo recebido. Por isso um diário completo podia receber 400 ao salvar uma edição parcial de `pendente`, mesmo com a chave da submissão já vinculada. O mesmo problema afetava a submissão final quando os campos obrigatórios tinham sido salvos nas etapas anteriores.

A correção prepara uma cópia separada da entidade, aplica os campos enviados e valida o estado resultante nas submissões/edições finais. A entidade persistida só é alterada depois da validação. Assinatura do motorista continua vindo da conta, e não da URL enviada pelo aplicativo.

Em rascunhos, permanece a validação da etapa enviada. A retomada pode corrigir uma etapa anterior antes de ajustar a seguinte: por exemplo, reduzir o total de animais e depois corrigir as condições físicas. O estado completo deve estar coerente antes de submeter.

## Contrato de salvamento

| Operação/estado | Chave | Resultado esperado |
| --- | --- | --- |
| POST parcial com `status=rascunho`, locais digitados e sem IDs | Sem chave | 201, ID e rascunho |
| PUT das etapas/edição de rascunho | Sem chave | 200, mesmo ID, campos omitidos preservados |
| Primeira submissão por PUT com `status=pendente` | Obrigatória | 200 se dados salvos + enviados estiverem completos e coerentes |
| Edição parcial de `pendente` com chave já vinculada | Header opcional | 200, mesma chave e data de envio |
| Edição de `pendente` legado sem chave | Obrigatória na primeira edição | Sem header: 400 com erro `Idempotency-Key`; com header válido: vincula ao mesmo ID |
| Nova chave diferente da vinculada ou já usada em outro diário | Rejeitada | 400; dados anteriores preservados |
| Submissão incompleta/incoerente | Chave válida | 400 com `fieldErrors`; rascunho e chave anteriores preservados |

Se todas as etapas já estiverem salvas, o PUT final pode conter somente `{"status":"pendente"}`, com o header `Idempotency-Key`. A API não considera campos omitidos como ausentes no estado salvo. As quatro assinaturas e os demais requisitos finais continuam obrigatórios.

O mobile deve sincronizar/avançar somente depois do sucesso da API. Se a atualização for rejeitada, deve manter a etapa para correção, exibir `fieldErrors` e não criar outro relatório para contornar o erro. Um diário legado sem chave precisa seguir a regra acima; o aplicativo não deve presumir que todo `pendente` já possui chave.

## Regressões e migrations

- Testes de serviço: edição parcial de pendente, submissão com etapas já salvas, chave imutável, pendente legado, assinatura fixa, rollback e coerência de horários/km/animais.
- `DiarioRotaLifecycleIntegrationTest`: HTTP/JSON, validação, serviço transacional e H2; seis etapas no mesmo ID, consulta `/me`, retomada, edição de rascunho e pendente e recuperação após 400.
- A reprodução usa GTA `6767a`, NF `56767`, `Fazenda Do Osmar`, `Fazenda Do Noé`, `2026-09-11`, `08:44:00`, `10:12:00` e km 67 em banco isolado, sem criar registros de produção.
- `PostgresIntegrationTest`: aplica Flyway em PostgreSQL 16 isolado, exige sucesso da V15, verifica os 28 campos tornados anuláveis e insere rascunho com nomes digitados e etapas futuras ausentes.
- Migrations existentes permanecem intactas. Não executar `repair`, editar checksum ou remover constraints manualmente para liberar o deploy.
- O gate do CI exige os dois testes PostgreSQL executados, sem skips. `clean verify` continua exigindo pelo menos 80% de linhas e 60% de branches, sem exclusões novas.

## Conferência de publicação

O endpoint `/api/v1/status` passa a incluir `commit`, obtido de `RENDER_GIT_COMMIT` no runtime. Fora do Render, o valor padrão é `unknown`. Esse campo identifica o artefato, mas não comprova sozinho a aplicação das migrations.

Após o merge e deploy do SHA aprovado:

1. Conferir no Render que o deploy usa o mesmo SHA da `main` e que os logs terminam com Flyway V15 aplicada/validada e inicialização concluída, sem erro de checksum, DDL ou Hibernate.
2. Confirmar HTTP 200 e `commit` esperado em `/api/v1/status`, e saúde em `/actuator/health`.
3. Em `/v3/api-docs`, confirmar `CriarRelatorioViagemRequest.properties.nomeOrigem`, `nomeDestino`, `GET /api/v1/relatorios-viagem/me` e `GET /api/v1/locais-diario/fazendas`. Há teste automatizado desse contrato.
4. Com uma conta de motorista de teste, consultar primeiro `/me` para reutilizar o diário de teste, se existir. Criar somente se necessário; manter o mesmo ID e chave ao salvar/repetir a submissão.
5. Repetir criação, seis etapas, retomada e edição de rascunho/pendente conforme a tabela. Não registrar JWT nos logs/evidências.

Conferência de histórico, somente leitura, pelo operador do banco:

```sql
SELECT version, script, success
FROM public.flyway_schema_history
WHERE version = '15';
```

Se o schema do Flyway foi configurado via `DB_SCHEMA`, usar esse schema no lugar de `public`.

Para qualquer 400 inesperado, guardar método, caminho, ID/estado anterior, presença da chave (sem JWT), corpo enviado sem credenciais, resposta ProblemDetail completa, horário/log correspondente e SHA retornado pelo status. Um 400 de chave ausente no legado ou de incoerência final é rejeição prevista; deve ser distinguido de falha de contrato/deploy.

## Limites da confirmação de produção nesta investigação

Em 15/10, os GETs a `/api/v1/status`, `/actuator/health` e `/v3/api-docs` expiraram em 35 segundos por rota a partir deste ambiente. O GitHub confirma a `main` em `d938d17`, mas não expõe registros de deployment do Render. Portanto, o SHA em execução, o schema publicado atual e os logs Flyway de produção ainda exigem confirmação autenticada no Render. Nenhum deploy ou cadastro de teste foi feito em produção durante a correção.

Fontes da identificação de commit no Render: [variáveis padrão](https://render.com/docs/environment-variables) e [runtime Docker](https://render.com/docs/docker).
