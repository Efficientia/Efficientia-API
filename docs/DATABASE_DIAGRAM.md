# Diagrama e Especificação do Banco de Dados - Efficientia

Este documento contém o Esquema Relacional do Banco de Dados do sistema **Efficientia**, capturado da modelagem oficial no `dbdiagram.io`.

---

## Visualização do Modelo ER (Mermaid)

```mermaid
erDiagram
    usuario {
        int id PK
        tipo_usuario tipo
        varchar11 cpf UK
        varchar50 codigo_interno UK
        varchar150 nome
        date data_nascimento
        varchar150 email UK
        varchar20 telefone
        varchar255 senha_hash
        boolean ativo
    }

    veiculo_cavalo {
        int id PK
        varchar7 placa
        boolean ativo
    }

    veiculo_carreta {
        int id PK
        varchar7 placa
        integer capacidade_cabecas
    }

    endereco {
        int id PK
        varchar10 cep
        varchar150 logradouro
        varchar20 numero
        varchar100 cidade
        varchar2 estado
    }

    fazenda {
        int id PK
        int pecuarista_id FK
        int endereco_id FK
        varchar150 nome
    }

    unidade_frigorifica {
        int id PK
        int analista_id FK
        int endereco_id FK
        varchar150 nome
    }

    relatorio_viagem {
        int id PK
        int fazenda_id FK
        int motorista_id FK
        int manobrista_id FK
        int curraleiro_id FK
        int cavalo_id FK
        int carreta_id FK
        varchar50 numero_gta
        varchar50 numero_nota_fiscal
        date data_embarque
        time horario_embarque
        time horario_saida_propriedade
        int km_saida_embarcadouro
        date data_chegada_unidade
        time horario_chegada_unidade
        time horario_desembarque
        int km_chegada_desembarcadouro
        varchar20 numero_curral
        boolean sirene_re_funcionou
        int qtd_machos
        int qtd_femeas
        int qtd_marrucos
        int qtd_em_pe
        int qtd_deitado
        int qtd_morto
        int qtd_emergencia
        text motivo_emergencia
        text comentarios
        varchar255 url_assinatura_pecuarista
        varchar255 url_assinatura_motorista
        varchar255 url_assinatura_manobrista
        varchar255 url_assinatura_curraleiro
        timestamp criado_em
    }

    parada_imprevista {
        int id PK
        int relatorio_id FK
        motivo_parada motivo
        timestamp data_hora_inicio
        timestamp data_hora_fim
    }

    anomalia_embarque {
        int id PK
        int relatorio_id FK
        anomalia_embarque anomalia
        varchar150 descricao_outros
    }

    anomalia_desembarque {
        int id PK
        int relatorio_id FK
        anomalia_desembarque anomalia
        varchar150 descricao_outros
    }

    auditoria_analise {
        int id PK
        int relatorio_id FK
        int analista_id FK
        timestamp data_hora_analise
        boolean aprovado
        text parecer_tecnico
    }

    usuario ||--o{ fazenda : "pecuarista"
    usuario ||--o{ unidade_frigorifica : "analista"
    usuario ||--o{ relatorio_viagem : "motorista / manobrista / curraleiro"
    endereco ||--o{ fazenda : ""
    endereco ||--o{ unidade_frigorifica : ""
    fazenda ||--o{ relatorio_viagem : ""
    veiculo_cavalo ||--o{ relatorio_viagem : ""
    veiculo_carreta ||--o{ relatorio_viagem : ""
    relatorio_viagem ||--o{ parada_imprevista : ""
    relatorio_viagem ||--o{ anomalia_embarque : ""
    relatorio_viagem ||--o{ anomalia_desembarque : ""
    relatorio_viagem ||--o{ auditoria_analise : ""
    usuario ||--o{ auditoria_analise : "analista"
```

---

## Enumeradores (Enums)

### `tipo_usuario`
- `motorista`
- `manobrista`
- `analista`
- `pecuarista`
- `curraleiro`

### `motivo_parada`
- `transbordo`
- `acidente_pista`
- `atoleiro`
- `problema_mecanico`
- `outro`

### `anomalia_embarque`
- `sangrando`
- `excesso_magreza_debilitado`
- `cutucoes_fortes`
- `mancando`
- `sujo_pisoteio`
- `tentativa_quebrar_cauda`
- `gaiola_cheia`
- `chute_paulada_ferrao`
- `arraste`
- `outro`

### `anomalia_desembarque`
- `gaiola_buraco_estrado_solto`
- `porteiras_nao_abrem`
- `uso_abusivo_choque`
- `problema_mecanico_veiculo`
- `outros_atos_abuso`

---

## Tabela `usuario` (Detalhes para Autenticação / Cadastro)

| Coluna | Tipo | Restrições | Descrição |
| --- | --- | --- | --- |
| `id` | `INT` | PK, AUTO_INCREMENT | Identificador único do usuário |
| `tipo` | `tipo_usuario` | NOT NULL | Tipo/Papel do usuário |
| `cpf` | `VARCHAR(11)` | NOT NULL, UNIQUE | CPF sem pontuação (11 dígitos) |
| `codigo_interno` | `VARCHAR(50)` | UNIQUE | Código interno / Código da empresa |
| `nome` | `VARCHAR(150)` | NOT NULL | Nome completo |
| `data_nascimento` | `DATE` | | Data de nascimento |
| `email` | `VARCHAR(150)` | UNIQUE | E-mail de login / contato |
| `telefone` | `VARCHAR(20)` | | Telefone de contato |
| `senha_hash` | `VARCHAR(255)` | NOT NULL | Senha criptografada (BCrypt) |
| `ativo` | `BOOLEAN` | NOT NULL | Status do usuário (true/false) |
