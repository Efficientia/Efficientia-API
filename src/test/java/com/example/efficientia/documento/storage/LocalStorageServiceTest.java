package com.example.efficientia.documento.storage;

import com.example.efficientia.config.StorageProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.util.unit.DataSize;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.UUID;

import static com.example.efficientia.documento.storage.StorageValidationException.Reason.EMPTY_FILE;
import static com.example.efficientia.documento.storage.StorageValidationException.Reason.INVALID_INPUT;
import static com.example.efficientia.documento.storage.StorageValidationException.Reason.INVALID_KEY;
import static com.example.efficientia.documento.storage.StorageValidationException.Reason.SIZE_LIMIT_EXCEEDED;
import static com.example.efficientia.documento.storage.StorageValidationException.Reason.UNSUPPORTED_MEDIA_TYPE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LocalStorageServiceTest {

    @TempDir
    Path tempDir;

    @ParameterizedTest
    @CsvSource({
            "application/pdf,pdf,%PDF-conteudo-de-teste",
            "image/png,png,PNG-conteudo-de-teste",
            "application/zip,zip,PK-conteudo-de-teste"
    })
    void deveSalvarAbrirERemoverPorStreaming(String mimeType, String extensao, String texto)
            throws IOException {
        LocalStorageService storage = novoStorage(tempDir.resolve("storage"), 1_024, 1_024);
        byte[] conteudo = texto.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        UUID referenciaId = UUID.randomUUID();

        ArquivoArmazenado salvo = storage.salvar(
                referenciaId,
                "arquivo-original." + extensao,
                mimeType,
                new ByteArrayInputStream(conteudo)
        );

        String prefix = mimeType.equals("application/zip") ? "exportacoes/" : "documentos/";
        assertThat(salvo.storageKey())
                .matches(prefix + referenciaId + "/[0-9a-f-]{36}\\." + extensao);
        assertThat(salvo.tamanhoBytes()).isEqualTo(conteudo.length);
        assertThat(salvo.sha256()).isEqualTo(sha256(conteudo));
        assertThat(salvo.mimeType()).isEqualTo(mimeType);

        StoredDocument aberto = storage.abrir(salvo.storageKey());
        assertThat(aberto.mimeType()).isEqualTo(mimeType);
        assertThat(aberto.tamanhoBytes()).isEqualTo(conteudo.length);
        try (InputStream input = aberto.conteudo().getInputStream()) {
            assertThat(input.readAllBytes()).isEqualTo(conteudo);
        }

        storage.remover(salvo.storageKey());

        assertThat(aberto.conteudo().exists()).isFalse();
        assertThatCode(() -> storage.remover(salvo.storageKey())).doesNotThrowAnyException();
        assertThatThrownBy(() -> storage.abrir(salvo.storageKey()))
                .isInstanceOf(StorageFileNotFoundException.class);
    }

    @Test
    void nomeMaliciosoNaoDeveComporAChaveNemEscaparDaRaiz() throws IOException {
        Path root = tempDir.resolve("storage");
        Path foraDaRaiz = tempDir.resolve("escape.pdf");
        LocalStorageService storage = novoStorage(root, 1_024, 1_024);

        ArquivoArmazenado salvo = storage.salvar(
                UUID.randomUUID(),
                "../../escape.pdf",
                "application/pdf",
                new ByteArrayInputStream("conteudo".getBytes(java.nio.charset.StandardCharsets.UTF_8))
        );
        Path arquivoReal = storage.abrir(salvo.storageKey()).conteudo().getFile().toPath().toRealPath();

        assertThat(salvo.storageKey()).doesNotContain("..", "escape.pdf");
        assertThat(arquivoReal).startsWith(root.toRealPath());
        assertThat(foraDaRaiz).doesNotExist();

        assertThatThrownBy(() -> storage.abrir("../../escape.pdf"))
                .isInstanceOfSatisfying(StorageValidationException.class,
                        exception -> assertThat(exception.getReason()).isEqualTo(INVALID_KEY));
        assertThatThrownBy(() -> storage.remover("documentos/../escape.pdf"))
                .isInstanceOfSatisfying(StorageValidationException.class,
                        exception -> assertThat(exception.getReason()).isEqualTo(INVALID_KEY));
    }

    @Test
    void deveAplicarLimiteDuranteACopiaELimparTemporario() throws IOException {
        Path root = tempDir.resolve("storage");
        LocalStorageService storage = novoStorage(root, 4, 4);

        assertThatThrownBy(() -> storage.salvar(
                UUID.randomUUID(),
                "grande.pdf",
                "application/pdf",
                new ByteArrayInputStream(new byte[5])
        )).isInstanceOfSatisfying(StorageValidationException.class,
                exception -> assertThat(exception.getReason()).isEqualTo(SIZE_LIMIT_EXCEEDED));

        try (var paths = Files.walk(root)) {
            assertThat(paths.filter(Files::isRegularFile).toList()).isEmpty();
        }
    }

    @Test
    void deveRejeitarArquivoVazioSemDeixarResiduo() throws IOException {
        Path root = tempDir.resolve("storage");
        LocalStorageService storage = novoStorage(root, 100, 100);

        assertThatThrownBy(() -> storage.salvar(
                UUID.randomUUID(),
                "vazio.png",
                "image/png",
                InputStream.nullInputStream()
        )).isInstanceOfSatisfying(StorageValidationException.class,
                exception -> assertThat(exception.getReason()).isEqualTo(EMPTY_FILE));

        try (var paths = Files.walk(root)) {
            assertThat(paths.filter(Files::isRegularFile).toList()).isEmpty();
        }
    }

    @Test
    void deveRejeitarMimeForaDaListaPermitida() {
        LocalStorageService storage = novoStorage(tempDir.resolve("storage"), 100, 100);

        assertThatThrownBy(() -> storage.salvar(
                UUID.randomUUID(),
                "arquivo.txt",
                "text/plain",
                new ByteArrayInputStream(new byte[]{1})
        )).isInstanceOfSatisfying(StorageValidationException.class,
                exception -> assertThat(exception.getReason()).isEqualTo(UNSUPPORTED_MEDIA_TYPE));
    }

    @Test
    void deveRejeitarDadosObrigatoriosAusentesEFecharOStream() {
        LocalStorageService storage = novoStorage(tempDir.resolve("storage"), 100, 100);
        TrackingInputStream input = new TrackingInputStream(new byte[]{1});

        assertThatThrownBy(() -> storage.salvar(null, "arquivo.pdf", "application/pdf", input))
                .isInstanceOfSatisfying(StorageValidationException.class,
                        exception -> assertThat(exception.getReason()).isEqualTo(INVALID_INPUT));
        assertThat(input.closed()).isTrue();

        assertThatThrownBy(() -> storage.salvar(UUID.randomUUID(), "  ", "application/pdf",
                new ByteArrayInputStream(new byte[]{1})))
                .isInstanceOfSatisfying(StorageValidationException.class,
                        exception -> assertThat(exception.getReason()).isEqualTo(INVALID_INPUT));

        assertThatThrownBy(() -> storage.salvar(UUID.randomUUID(), "arquivo.pdf", "application/pdf", null))
                .isInstanceOfSatisfying(StorageValidationException.class,
                        exception -> assertThat(exception.getReason()).isEqualTo(INVALID_INPUT));
    }

    @Test
    void deveContinuarCopiaQuandoStreamRetornaZeroEReutilizarDiretoriosExistentes() throws IOException {
        LocalStorageService storage = novoStorage(tempDir.resolve("storage"), 100, 100);
        UUID referencia = UUID.randomUUID();
        byte[] conteudo = "conteudo-depois-de-leitura-zero".getBytes(java.nio.charset.StandardCharsets.UTF_8);

        ArquivoArmazenado primeiro = storage.salvar(referencia, "primeiro.pdf", "application/pdf",
                new ZeroThenDataInputStream(conteudo));
        ArquivoArmazenado segundo = storage.salvar(referencia, "segundo.pdf", "application/pdf",
                new ByteArrayInputStream(conteudo));

        assertThat(primeiro.tamanhoBytes()).isEqualTo(conteudo.length);
        assertThat(segundo.tamanhoBytes()).isEqualTo(conteudo.length);
        try (InputStream input = storage.abrir(primeiro.storageKey()).conteudo().getInputStream()) {
            assertThat(input.readAllBytes()).isEqualTo(conteudo);
        }
    }

    @Test
    void deveEncapsularFalhaDeLeituraELimparArquivoTemporario() throws IOException {
        Path root = tempDir.resolve("storage");
        LocalStorageService storage = novoStorage(root, 100, 100);
        TrackingFailingInputStream input = new TrackingFailingInputStream();

        assertThatThrownBy(() -> storage.salvar(UUID.randomUUID(), "falha.pdf", "application/pdf", input))
                .isInstanceOf(StorageException.class)
                .hasCauseInstanceOf(IOException.class);

        assertThat(input.closed()).isTrue();
        try (var paths = Files.walk(root)) {
            assertThat(paths.filter(Files::isRegularFile).toList()).isEmpty();
        }
    }

    @Test
    void deveLidarComRaizAusenteChavesInexistentesEDiretoriosNoLugarDeArquivo() throws IOException {
        Path root = tempDir.resolve("storage-ainda-nao-criado");
        LocalStorageService storage = novoStorage(root, 100, 100);
        String chave = chaveValida();

        assertThatThrownBy(() -> storage.abrir(chave)).isInstanceOf(StorageFileNotFoundException.class);
        assertThatCode(() -> storage.remover(chave)).doesNotThrowAnyException();
        assertThat(root).doesNotExist();

        Files.createDirectories(root);
        assertThatThrownBy(() -> storage.abrir(chave)).isInstanceOf(StorageFileNotFoundException.class);
        assertThatCode(() -> storage.remover(chave)).doesNotThrowAnyException();

        Path arquivoComoDiretorio = root.resolve(chave.replace('/', java.io.File.separatorChar));
        Files.createDirectories(arquivoComoDiretorio);
        assertThatThrownBy(() -> storage.abrir(chave))
                .isInstanceOfSatisfying(StorageValidationException.class,
                        exception -> assertThat(exception.getReason()).isEqualTo(INVALID_KEY));
        assertThatThrownBy(() -> storage.remover(chave))
                .isInstanceOfSatisfying(StorageValidationException.class,
                        exception -> assertThat(exception.getReason()).isEqualTo(INVALID_KEY));
    }

    @Test
    void deveRejeitarDiretorioDeEntidadeQueNaoSejaDiretorioSeguro() throws IOException {
        Path root = tempDir.resolve("storage");
        Files.createDirectories(root);
        Files.writeString(root.resolve("documentos"), "arquivo em conflito");
        LocalStorageService storage = novoStorage(root, 100, 100);

        assertThatThrownBy(() -> storage.salvar(UUID.randomUUID(), "arquivo.pdf", "application/pdf",
                new ByteArrayInputStream(new byte[]{1})))
                .isInstanceOfSatisfying(StorageValidationException.class,
                        exception -> assertThat(exception.getReason()).isEqualTo(INVALID_KEY));
    }

    @Test
    void deveRejeitarChavesNulasEComFormatoIncorreto() {
        LocalStorageService storage = novoStorage(tempDir.resolve("storage"), 100, 100);

        assertThatThrownBy(() -> storage.abrir(null))
                .isInstanceOfSatisfying(StorageValidationException.class,
                        exception -> assertThat(exception.getReason()).isEqualTo(INVALID_KEY));
        assertThatThrownBy(() -> storage.remover(""))
                .isInstanceOfSatisfying(StorageValidationException.class,
                        exception -> assertThat(exception.getReason()).isEqualTo(INVALID_KEY));
    }

    @Test
    void deveConsumirStreamEmBlocosSemCarregarArquivoInteiro() {
        LocalStorageService storage = novoStorage(tempDir.resolve("storage"), 128 * 1_024, 128 * 1_024);
        GeneratedInputStream input = new GeneratedInputStream(64 * 1_024);

        ArquivoArmazenado salvo = storage.salvar(
                UUID.randomUUID(),
                "stream.pdf",
                "application/pdf",
                input
        );

        assertThat(salvo.tamanhoBytes()).isEqualTo(64 * 1_024);
        assertThat(input.maxRequestedBytes()).isLessThanOrEqualTo(8 * 1_024);
        assertThat(input.closed()).isTrue();
    }

    private LocalStorageService novoStorage(Path root, long maxPdf, long maxPng) {
        return new LocalStorageService(new StorageProperties(
                root,
                DataSize.ofBytes(maxPdf),
                DataSize.ofBytes(maxPng)
        ));
    }

    private String sha256(byte[] conteudo) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(conteudo));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String chaveValida() {
        return "documentos/" + UUID.randomUUID() + "/" + UUID.randomUUID() + ".pdf";
    }

    private static final class TrackingInputStream extends ByteArrayInputStream {

        private boolean closed;

        private TrackingInputStream(byte[] conteudo) {
            super(conteudo);
        }

        @Override
        public void close() throws IOException {
            closed = true;
            super.close();
        }

        private boolean closed() {
            return closed;
        }
    }

    private static final class ZeroThenDataInputStream extends InputStream {

        private final byte[] conteudo;
        private boolean returnedZero;
        private int indice;

        private ZeroThenDataInputStream(byte[] conteudo) {
            this.conteudo = conteudo;
        }

        @Override
        public int read() {
            return indice < conteudo.length ? conteudo[indice++] & 0xff : -1;
        }

        @Override
        public int read(byte[] buffer, int offset, int length) {
            if (!returnedZero) {
                returnedZero = true;
                return 0;
            }
            if (indice == conteudo.length) {
                return -1;
            }
            int count = Math.min(length, conteudo.length - indice);
            System.arraycopy(conteudo, indice, buffer, offset, count);
            indice += count;
            return count;
        }
    }

    private static final class TrackingFailingInputStream extends InputStream {

        private boolean closed;

        @Override
        public int read() throws IOException {
            throw new IOException("Falha de leitura simulada.");
        }

        @Override
        public int read(byte[] buffer, int offset, int length) throws IOException {
            throw new IOException("Falha de leitura simulada.");
        }

        @Override
        public void close() {
            closed = true;
        }

        private boolean closed() {
            return closed;
        }
    }

    private static final class GeneratedInputStream extends InputStream {

        private long remaining;
        private int maxRequestedBytes;
        private boolean closed;

        private GeneratedInputStream(long tamanho) {
            this.remaining = tamanho;
        }

        @Override
        public int read() {
            if (remaining == 0) {
                return -1;
            }
            remaining--;
            return 'a';
        }

        @Override
        public int read(byte[] buffer, int offset, int length) {
            maxRequestedBytes = Math.max(maxRequestedBytes, length);
            if (remaining == 0) {
                return -1;
            }

            int count = (int) Math.min(remaining, length);
            Arrays.fill(buffer, offset, offset + count, (byte) 'a');
            remaining -= count;
            return count;
        }

        @Override
        public void close() {
            closed = true;
        }

        private int maxRequestedBytes() {
            return maxRequestedBytes;
        }

        private boolean closed() {
            return closed;
        }
    }
}
