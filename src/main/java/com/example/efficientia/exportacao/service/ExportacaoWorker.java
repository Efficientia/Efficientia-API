package com.example.efficientia.exportacao.service;

import com.example.efficientia.documento.persistence.DocumentoEntity;
import com.example.efficientia.documento.persistence.DocumentoRepository;
import com.example.efficientia.documento.storage.ArquivoArmazenado;
import com.example.efficientia.documento.storage.StorageService;
import com.example.efficientia.documento.storage.StoredDocument;
import com.example.efficientia.exportacao.domain.EstadoExportacao;
import com.example.efficientia.exportacao.persistence.ExportacaoEntity;
import com.example.efficientia.exportacao.persistence.ExportacaoRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Component
public class ExportacaoWorker {

    private static final Logger log = LoggerFactory.getLogger(ExportacaoWorker.class);

    private final ExportacaoRepository exportacaoRepository;
    private final DocumentoRepository documentoRepository;
    private final StorageService storageService;

    public ExportacaoWorker(
            ExportacaoRepository exportacaoRepository,
            DocumentoRepository documentoRepository,
            StorageService storageService
    ) {
        this.exportacaoRepository = exportacaoRepository;
        this.documentoRepository = documentoRepository;
        this.storageService = storageService;
    }

    @Scheduled(fixedDelay = 5000)
    public void processarProximaExportacao() {
        Optional<ExportacaoEntity> exportacaoOpt = exportacaoRepository.findFirstByEstadoOrderByCriadoEmAsc(EstadoExportacao.NA_FILA);
        
        if (exportacaoOpt.isEmpty()) {
            return;
        }

        ExportacaoEntity exportacao = exportacaoOpt.get();
        
        try {
            exportacao.setEstado(EstadoExportacao.PROCESSANDO);
            exportacao.setIniciadoEm(Instant.now());
            exportacao = exportacaoRepository.save(exportacao);
        } catch (ObjectOptimisticLockingFailureException e) {
            log.debug("Conflito de concorrência ao tentar processar exportação {}", exportacao.getId());
            return;
        }

        Path tempFile = null;
        try {
            tempFile = Files.createTempFile("exportacao-", ".zip");

            Set<String> nomesUsados = new HashSet<>();
            try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(tempFile.toFile()))) {
                for (UUID docId : exportacao.getDocumentoIds()) {
                    DocumentoEntity doc = documentoRepository.findById(docId)
                            .orElseThrow(() -> new IllegalStateException("Documento não encontrado: " + docId));
                            
                    if (doc.getStorageKey() == null) {
                        throw new IllegalStateException("Documento sem storageKey: " + docId);
                    }
                    
                    StoredDocument stored = storageService.abrir(doc.getStorageKey());
                    String nomeBase = doc.getNomeOriginal() != null ? doc.getNomeOriginal() : doc.getId().toString();
                    String nomeEntrada = gerarNomeUnico(nomeBase, nomesUsados);
                    nomesUsados.add(nomeEntrada);

                    ZipEntry zipEntry = new ZipEntry(nomeEntrada);
                    zos.putNextEntry(zipEntry);

                    try (InputStream is = stored.conteudo().getInputStream()) {
                        is.transferTo(zos);
                    }
                    zos.closeEntry();
                }
            }

            long tamanhoZip = Files.size(tempFile);

            ArquivoArmazenado armazenado;
            try (InputStream is = new FileInputStream(tempFile.toFile())) {
                String nomeArquivo = "exportacao-" + exportacao.getId() + ".zip";
                armazenado = storageService.salvar(
                        exportacao.getId(),
                        nomeArquivo,
                        "application/zip",
                        is
                );
            }

            exportacao.setEstado(EstadoExportacao.CONCLUIDA);
            exportacao.setConcluidoEm(Instant.now());
            exportacao.setExpiraEm(Instant.now().plus(1, ChronoUnit.HOURS));
            exportacao.setTamanhoZipBytes(tamanhoZip);
            exportacao.setStorageKey(armazenado.storageKey());
            exportacao.setNomeArquivo(armazenado.nomeOriginal());
            
            exportacaoRepository.save(exportacao);

        } catch (Exception e) {
            log.error("Falha ao processar exportação {}", exportacao.getId(), e);
            exportacao.setEstado(EstadoExportacao.FALHA);
            exportacao.setErroCodigo("ERRO_PROCESSAMENTO");
            exportacaoRepository.save(exportacao);
        } finally {
            if (tempFile != null) {
                try {
                    Files.deleteIfExists(tempFile);
                } catch (Exception ignored) {
                }
            }
        }
    }

    private String gerarNomeUnico(String nomeBase, Set<String> nomesUsados) {
        String nome = nomeBase;
        int contador = 1;
        
        int lastDot = nomeBase.lastIndexOf('.');
        String nameWithoutExt = lastDot != -1 ? nomeBase.substring(0, lastDot) : nomeBase;
        String ext = lastDot != -1 ? nomeBase.substring(lastDot) : "";

        while (nomesUsados.contains(nome)) {
            nome = nameWithoutExt + " (" + contador + ")" + ext;
            contador++;
        }
        return nome;
    }
}
