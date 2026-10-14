# Integração do diário de rota mobile

## Contrato novo

- `GET /api/v1/locais-diario/fazendas?q=&pagina=0&tamanho=20` e `GET /api/v1/locais-diario/unidades-frigorificas?...`: consulta autenticada, paginada (máximo 50), com `itens: [{id, nome, cidade, uf}]` e metadados `pagina`, `tamanho`, `total`, `totalPaginas`. Os locais são um catálogo para seleção; o JWT identifica o usuário, mas a modelagem atual não relaciona todas as fazendas à empresa transportadora.
- `GET /api/v1/relatorios-viagem/me?pagina=0&tamanho=4`: só relatórios cujo `motoristaId` é o `usuario_id` do JWT, ordenados por atualização decrescente. A listagem geral também aplica esse filtro para `ROLE_MOTORISTA`.
- `GET /api/v1/relatorios-viagem/{id}`: motorista não acessa o relatório de outro motorista (404).
- `POST/PUT /api/v1/relatorios-viagem`: `nomeOrigem` e `nomeDestino` podem ser digitados ou preenchidos pela seleção do catálogo. Na submissão final, cada local precisa ter nome não vazio ou ID de cadastro. IDs não são inferidos do texto: quando o nome muda sem ID no mesmo envio, o vínculo anterior é removido para evitar associação incorreta. Um nome livre fica no relatório, sem criar automaticamente uma fazenda ou unidade no cadastro mestre.
- `paradasImprevistas[].descricaoMotivo` preserva o motivo digitado quando `motivo` é `outro`. Para horários de parada, enviar ISO local completo em `dataHoraInicio`/`dataHoraFim`. Anomalia de embarque livre continua como `anomalia: "outro"`, `descricaoOutros` e `quantidadeAnimais`.

## Persistência

V15 torna anuláveis os campos e as quatro referências de assinatura ainda não conhecidos enquanto `status=rascunho`, preservando `motorista_id` obrigatório e a validação de completude na transição para `pendente`. Também adiciona os nomes de origem/destino e a descrição de motivo da parada. O app pode criar um rascunho na primeira etapa, atualizar o mesmo ID nas próximas, enviar três PNGs e concluir com `Idempotency-Key` estável.

Mesmo com `app.security.enabled=false` para desenvolvimento local, as rotas de diário e catálogo agora exigem JWT válido; o restante da API preserva a configuração local existente.

## Validação necessária no ambiente

Aplicar V15 em banco de homologação antes de testar o APK. Confirmar que `SECURITY_ENABLED=true` no Render e que o JWT de motorista tem `usuario_id` e `ROLE_MOTORISTA`. Exercitar um relatório com e sem paradas/anomalias, retomada do rascunho e login em segundo aparelho. As migrations existentes de schemas devem estar aplicadas antes de V15.
