# EFFICIENTI-221 — Sprint 04: storage privado e integridade

## Objetivo

Implementar o armazenamento privado de PDF e PNG da API REST principal, com
leitura e escrita em streaming, cálculo confiável de SHA-256 e proteção contra
travessia de diretórios. O PostgreSQL continua responsável pelos metadados; o
MongoDB permanece exclusivo da Efficientia AI API.

## Contrato do storage

`StorageService` mantém a aplicação independente do provedor físico:

```java
public interface StorageService {
    ArquivoArmazenado salvar(
            UUID documentoId,
            String nomeOriginal,
            String mimeType,
            InputStream conteudo
    );

    StoredDocument abrir(String storageKey);

    void remover(String storageKey);
}
```

O método `salvar` assume a posse do `InputStream`, fecha-o ao final e devolve:

- nome original, apenas como metadado;
- MIME aceito pelo storage;
- tamanho real em bytes;
- SHA-256 calculado pela API;
- chave interna usada para reabrir ou remover o conteúdo.

O hash e o tamanho são calculados durante a própria cópia para o destino. Eles
não são recebidos prontos do cliente e não exigem uma segunda leitura.

## Organização física

O primeiro provedor é `LocalStorageService`. Cada conteúdo recebe uma chave no
formato:

```text
documentos/{documentoId}/{uuid-interno}.pdf
documentos/{documentoId}/{uuid-interno}.png
```

O nome original nunca compõe diretório, nome físico ou `storageKey`. Assim, um
nome como `../../escape.pdf` continua sendo somente metadado e não consegue
alterar o destino do arquivo.

## Streaming e integridade

A cópia usa blocos fixos de 8 KiB e não carrega o arquivo inteiro em `byte[]`.
Na mesma passagem, a implementação:

1. contabiliza os bytes realmente lidos;
2. interrompe a cópia quando o limite do tipo é ultrapassado;
3. atualiza o digest SHA-256;
4. grava primeiro em arquivo temporário;
5. move o temporário para a chave definitiva, de forma atômica quando o sistema
   de arquivos oferece esse recurso.

Em caso de arquivo vazio, excesso de tamanho ou falha de escrita, o temporário
é removido e não fica um arquivo definitivo parcial.

## Proteções de caminho

As operações `abrir` e `remover` aceitam somente chaves que correspondam ao
formato interno definido pela API. Além dessa lista permitida estrita:

- o caminho é normalizado e precisa permanecer sob a raiz configurada;
- links simbólicos nos subdiretórios e no arquivo são rejeitados;
- apenas arquivos regulares podem ser abertos ou removidos;
- uma chave malformada gera erro de validação;
- remover uma chave válida que já não existe é uma operação idempotente;
- abrir uma chave válida inexistente gera `StorageFileNotFoundException`.

## Tipos e limites

Nesta etapa, o storage recebe somente os MIME types exatos:

- `application/pdf`;
- `image/png`.

Os limites são positivos e configurados por ambiente:

```properties
app.document-storage.path=${DOCUMENT_STORAGE_PATH:./data/documentos}
app.document-storage.max-pdf-size=${MAX_PDF_SIZE:25MB}
app.document-storage.max-png-size=${MAX_PNG_SIZE:10MB}
```

O `.env.example` documenta `DOCUMENT_STORAGE_PATH`, `MAX_PDF_SIZE` e
`MAX_PNG_SIZE` sem conter credenciais.

## Erros de domínio

- `StorageException`: falhas técnicas de leitura, escrita ou remoção;
- `StorageValidationException`: entrada ou chave inválida, MIME não suportado,
  conteúdo vazio ou limite excedido;
- `StorageFileNotFoundException`: conteúdo solicitado não encontrado.

Esses erros ficam no módulo de storage e serão traduzidos para respostas HTTP
quando o controller de documentos for implementado.

## Critérios de aceite atendidos

| Critério | Evidência |
| --- | --- |
| Salvar, abrir e remover PDF e PNG | teste parametrizado cobre o ciclo completo dos dois tipos |
| Chave interna independente do nome original | UUID interno é gerado pela API; teste usa nome `../../escape.pdf` |
| SHA-256 e tamanho calculados durante o upload | teste compara hash e tamanho com o conteúdo enviado |
| Nenhum arquivo inteiro em memória | teste confirma leituras de no máximo 8 KiB por bloco |
| Limites por tipo | teste ultrapassa o limite durante a cópia e verifica a exceção |
| Proteção contra path traversal | formato estrito, normalização, confinamento à raiz e testes com chaves maliciosas |
| Sem resíduos em falhas de validação | testes de excesso e arquivo vazio confirmam ausência de arquivos regulares |
| Configuração externa validada | `StorageProperties` rejeita limites nulos ou não positivos |

## Testes

Foram adicionados 8 casos executados nesta sprint: 7 do ciclo e das proteções
do `LocalStorageService` e 1 das propriedades de configuração. Em 30/08/2026,
`mvnw clean test` executou a suíte completa com 28 testes, sem falhas ou erros.

Os testes usam diretórios temporários isolados e não deixam conteúdo no
repositório.

## Fora do escopo desta sprint

- endpoint multipart e serviço transacional de documentos;
- detecção do MIME real pelos bytes do conteúdo;
- regras específicas de foto, desenho e assinatura textual;
- persistência dos metadados retornados em `DocumentoEntity`;
- autorização de leitura e download;
- provedor MinIO/S3.

Esses itens pertencem às próximas tarefas do épico da API REST principal.
