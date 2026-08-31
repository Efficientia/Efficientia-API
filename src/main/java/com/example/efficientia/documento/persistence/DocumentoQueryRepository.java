package com.example.efficientia.documento.persistence;

import com.example.efficientia.documento.domain.DocumentoCursor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

public interface DocumentoQueryRepository {

    Page<DocumentoEntity> buscarPagina(DocumentoFiltro filtro, Pageable pageable);

    Slice<DocumentoEntity> buscarCursor(
            DocumentoFiltro filtro,
            DocumentoCursor cursor,
            int tamanho
    );
}
