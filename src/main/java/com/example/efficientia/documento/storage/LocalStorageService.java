package com.example.efficientia.documento.storage;

import com.example.efficientia.config.StorageProperties;
import org.springframework.core.io.FileSystemResource;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

import static com.example.efficientia.documento.storage.StorageValidationException.Reason.EMPTY_FILE;
import static com.example.efficientia.documento.storage.StorageValidationException.Reason.INVALID_INPUT;
import static com.example.efficientia.documento.storage.StorageValidationException.Reason.INVALID_KEY;
import static com.example.efficientia.documento.storage.StorageValidationException.Reason.SIZE_LIMIT_EXCEEDED;
import static com.example.efficientia.documento.storage.StorageValidationException.Reason.UNSUPPORTED_MEDIA_TYPE;

@Service
public class LocalStorageService implements StorageService {

    private static final int BUFFER_SIZE = 8 * 1024;
    private static final String PDF_MIME_TYPE = "application/pdf";
    private static final String PNG_MIME_TYPE = "image/png";
    private static final Pattern STORAGE_KEY = Pattern.compile(
            "documentos/[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}/"
                    + "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}\\.(pdf|png)"
    );

    private final Path configuredRoot;
    private final long maxPdfBytes;
    private final long maxPngBytes;

    public LocalStorageService(StorageProperties properties) {
        this.configuredRoot = properties.path().toAbsolutePath().normalize();
        this.maxPdfBytes = properties.maxPdfSize().toBytes();
        this.maxPngBytes = properties.maxPngSize().toBytes();
    }

    @Override
    public ArquivoArmazenado salvar(
            UUID documentoId,
            String nomeOriginal,
            String mimeType,
            InputStream conteudo
    ) {
        if (conteudo == null) {
            throw new StorageValidationException(INVALID_INPUT, "O conteúdo do arquivo é obrigatório.");
        }

        Path temporario = null;
        try (InputStream input = conteudo) {
            validarEntrada(documentoId, nomeOriginal);
            MediaDefinition media = mediaDefinition(mimeType);
            Path root = prepararRaiz();
            Path documentos = criarSubdiretorioSeguro(root, root, "documentos");
            Path diretorioDocumento = criarSubdiretorioSeguro(
                    root,
                    documentos,
                    documentoId.toString()
            );

            String nomeInterno = UUID.randomUUID() + "." + media.extension();
            String storageKey = "documentos/" + documentoId + "/" + nomeInterno;
            Path destino = diretorioDocumento.resolve(nomeInterno).normalize();
            garantirSobRaiz(root, destino);

            temporario = Files.createTempFile(diretorioDocumento, ".upload-", ".tmp");
            CopyResult result = copiarComHash(input, temporario, media.maxBytes());
            if (result.tamanhoBytes() == 0) {
                throw new StorageValidationException(EMPTY_FILE, "O arquivo não pode estar vazio.");
            }

            moverAtomicoQuandoPossivel(temporario, destino);
            temporario = null;

            return new ArquivoArmazenado(
                    nomeOriginal,
                    media.mimeType(),
                    result.tamanhoBytes(),
                    result.sha256(),
                    storageKey
            );
        } catch (StorageException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new StorageException("Não foi possível salvar o arquivo no storage privado.", exception);
        } finally {
            removerTemporario(temporario);
        }
    }

    @Override
    public StoredDocument abrir(String storageKey) {
        Path arquivo = localizar(storageKey, true).orElseThrow(StorageFileNotFoundException::new);
        try {
            return new StoredDocument(
                    new FileSystemResource(arquivo),
                    mimeTypeDaChave(storageKey),
                    Files.size(arquivo)
            );
        } catch (IOException exception) {
            throw new StorageException("Não foi possível abrir o arquivo do storage privado.", exception);
        }
    }

    @Override
    public void remover(String storageKey) {
        Optional<Path> arquivo = localizar(storageKey, false);
        if (arquivo.isEmpty()) {
            return;
        }

        try {
            Files.delete(arquivo.get());
        } catch (IOException exception) {
            throw new StorageException("Não foi possível remover o arquivo do storage privado.", exception);
        }
    }

    private void validarEntrada(UUID documentoId, String nomeOriginal) {
        if (documentoId == null) {
            throw new StorageValidationException(INVALID_INPUT, "documentoId é obrigatório.");
        }
        if (nomeOriginal == null || nomeOriginal.isBlank()) {
            throw new StorageValidationException(INVALID_INPUT, "nomeOriginal é obrigatório.");
        }
    }

    private MediaDefinition mediaDefinition(String mimeType) {
        if (PDF_MIME_TYPE.equals(mimeType)) {
            return new MediaDefinition(PDF_MIME_TYPE, "pdf", maxPdfBytes);
        }
        if (PNG_MIME_TYPE.equals(mimeType)) {
            return new MediaDefinition(PNG_MIME_TYPE, "png", maxPngBytes);
        }
        throw new StorageValidationException(
                UNSUPPORTED_MEDIA_TYPE,
                "O storage privado aceita somente application/pdf e image/png."
        );
    }

    private Path prepararRaiz() throws IOException {
        Files.createDirectories(configuredRoot);
        Path root = configuredRoot.toRealPath();
        if (!Files.isDirectory(root, LinkOption.NOFOLLOW_LINKS)) {
            throw new StorageException("O caminho configurado para o storage não é um diretório.");
        }
        return root;
    }

    private Path criarSubdiretorioSeguro(Path root, Path parent, String nome) throws IOException {
        Path diretorio = parent.resolve(nome).normalize();
        garantirSobRaiz(root, diretorio);

        if (Files.exists(diretorio, LinkOption.NOFOLLOW_LINKS)) {
            if (Files.isSymbolicLink(diretorio)
                    || !Files.isDirectory(diretorio, LinkOption.NOFOLLOW_LINKS)) {
                throw chaveInvalida();
            }
        } else {
            try {
                Files.createDirectory(diretorio);
            } catch (java.nio.file.FileAlreadyExistsException ignored) {
                if (Files.isSymbolicLink(diretorio)
                        || !Files.isDirectory(diretorio, LinkOption.NOFOLLOW_LINKS)) {
                    throw chaveInvalida();
                }
            }
        }

        Path real = diretorio.toRealPath();
        garantirSobRaiz(root, real);
        return real;
    }

    private CopyResult copiarComHash(InputStream input, Path destino, long maxBytes) throws IOException {
        MessageDigest digest = sha256Digest();
        byte[] buffer = new byte[BUFFER_SIZE];
        long total = 0;

        try (OutputStream output = Files.newOutputStream(
                destino,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING
        )) {
            int lidos;
            while ((lidos = input.read(buffer)) != -1) {
                if (lidos == 0) {
                    continue;
                }
                if (total > maxBytes - lidos) {
                    throw new StorageValidationException(
                            SIZE_LIMIT_EXCEEDED,
                            "O arquivo excede o limite configurado para o tipo informado."
                    );
                }

                output.write(buffer, 0, lidos);
                digest.update(buffer, 0, lidos);
                total += lidos;
            }
        }

        return new CopyResult(total, HexFormat.of().formatHex(digest.digest()));
    }

    private MessageDigest sha256Digest() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 não está disponível na JVM.", exception);
        }
    }

    private void moverAtomicoQuandoPossivel(Path origem, Path destino) throws IOException {
        try {
            Files.move(origem, destino, StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException exception) {
            Files.move(origem, destino);
        }
    }

    private Optional<Path> localizar(String storageKey, boolean obrigatorio) {
        validarChave(storageKey);
        if (!Files.exists(configuredRoot, LinkOption.NOFOLLOW_LINKS)) {
            return inexistente(obrigatorio);
        }

        try {
            Path root = configuredRoot.toRealPath();
            Path candidato = root.resolve(storageKey.replace('/', java.io.File.separatorChar)).normalize();
            garantirSobRaiz(root, candidato);

            if (!Files.exists(candidato, LinkOption.NOFOLLOW_LINKS)) {
                return inexistente(obrigatorio);
            }
            if (Files.isSymbolicLink(candidato)) {
                throw chaveInvalida();
            }

            Path real = candidato.toRealPath();
            garantirSobRaiz(root, real);
            if (!Files.isRegularFile(real, LinkOption.NOFOLLOW_LINKS)) {
                throw chaveInvalida();
            }
            return Optional.of(real);
        } catch (StorageException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new StorageException("Não foi possível acessar o storage privado.", exception);
        }
    }

    private Optional<Path> inexistente(boolean obrigatorio) {
        if (obrigatorio) {
            throw new StorageFileNotFoundException();
        }
        return Optional.empty();
    }

    private void validarChave(String storageKey) {
        if (storageKey == null || !STORAGE_KEY.matcher(storageKey).matches()) {
            throw chaveInvalida();
        }
    }

    private void garantirSobRaiz(Path root, Path path) {
        if (!path.normalize().startsWith(root)) {
            throw chaveInvalida();
        }
    }

    private StorageValidationException chaveInvalida() {
        return new StorageValidationException(INVALID_KEY, "Chave de storage inválida.");
    }

    private String mimeTypeDaChave(String storageKey) {
        return storageKey.endsWith(".pdf") ? PDF_MIME_TYPE : PNG_MIME_TYPE;
    }

    private void removerTemporario(Path temporario) {
        if (temporario == null) {
            return;
        }
        try {
            Files.deleteIfExists(temporario);
        } catch (IOException ignored) {
            // A falha principal é preservada. O nome temporário não é exposto.
        }
    }

    private record MediaDefinition(String mimeType, String extension, long maxBytes) {
    }

    private record CopyResult(long tamanhoBytes, String sha256) {
    }
}
