package com.example.efficientia.documento.persistence;

import com.example.efficientia.documento.domain.DocumentoCursor;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import com.example.efficientia.relatorioviagem.persistence.RelatorioViagemEntity;
import com.example.efficientia.security.DocumentoAccessScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;

public class DocumentoQueryRepositoryImpl implements DocumentoQueryRepository {

    private final EntityManager entityManager;

    public DocumentoQueryRepositoryImpl(EntityManager entityManager) {
        this.entityManager = entityManager;
    }

    @Override
    public Page<DocumentoEntity> buscarPagina(DocumentoFiltro filtro, Pageable pageable) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<DocumentoEntity> criteria = builder.createQuery(DocumentoEntity.class);
        Root<DocumentoEntity> root = criteria.from(DocumentoEntity.class);
        criteria.select(root).where(predicados(builder, criteria, root, filtro, null).toArray(Predicate[]::new));
        criteria.orderBy(ordenacao(builder, root, pageable.getSort()));

        TypedQuery<DocumentoEntity> query = entityManager.createQuery(criteria);
        query.setFirstResult(Math.toIntExact(pageable.getOffset()));
        query.setMaxResults(pageable.getPageSize());

        long total = contar(builder, filtro);
        return new PageImpl<>(query.getResultList(), pageable, total);
    }

    @Override
    public Slice<DocumentoEntity> buscarCursor(
            DocumentoFiltro filtro,
            DocumentoCursor cursor,
            int tamanho
    ) {
        CriteriaBuilder builder = entityManager.getCriteriaBuilder();
        CriteriaQuery<DocumentoEntity> criteria = builder.createQuery(DocumentoEntity.class);
        Root<DocumentoEntity> root = criteria.from(DocumentoEntity.class);
        criteria.select(root).where(predicados(builder, criteria, root, filtro, cursor).toArray(Predicate[]::new));
        criteria.orderBy(builder.asc(root.get("criadoEm")), builder.asc(root.get("id")));

        List<DocumentoEntity> encontrados = entityManager.createQuery(criteria)
                .setMaxResults(tamanho + 1)
                .getResultList();
        boolean temMais = encontrados.size() > tamanho;
        List<DocumentoEntity> conteudo = temMais
                ? List.copyOf(encontrados.subList(0, tamanho))
                : List.copyOf(encontrados);

        Pageable pageable = PageRequest.of(
                0,
                tamanho,
                Sort.by(Sort.Order.asc("criadoEm"), Sort.Order.asc("id"))
        );
        return new SliceImpl<>(conteudo, pageable, temMais);
    }

    private long contar(CriteriaBuilder builder, DocumentoFiltro filtro) {
        CriteriaQuery<Long> criteria = builder.createQuery(Long.class);
        Root<DocumentoEntity> root = criteria.from(DocumentoEntity.class);
        criteria.select(builder.count(root))
                .where(predicados(builder, criteria, root, filtro, null).toArray(Predicate[]::new));
        return entityManager.createQuery(criteria).getSingleResult();
    }

    private List<Predicate> predicados(
            CriteriaBuilder builder,
            CriteriaQuery<?> criteria,
            Root<DocumentoEntity> root,
            DocumentoFiltro filtro,
            DocumentoCursor cursor
    ) {
        List<Predicate> predicates = new ArrayList<>();
        if (filtro.viagemId() != null) {
            predicates.add(builder.equal(root.get("viagemId"), filtro.viagemId()));
        }
        if (filtro.assinanteId() != null) {
            predicates.add(builder.equal(root.get("assinanteId"), filtro.assinanteId()));
        }
        if (filtro.tipoDocumento() != null) {
            predicates.add(builder.equal(root.get("tipoDocumento"), filtro.tipoDocumento()));
        }
        if (filtro.origem() != null) {
            predicates.add(builder.equal(root.get("origem"), filtro.origem()));
        }
        if (filtro.modalidadeAssinatura() != null) {
            predicates.add(builder.equal(root.get("modalidadeAssinatura"), filtro.modalidadeAssinatura()));
        }
        if (filtro.criadoDe() != null) {
            predicates.add(builder.greaterThanOrEqualTo(root.get("criadoEm"), filtro.criadoDe()));
        }
        if (filtro.criadoAte() != null) {
            predicates.add(builder.lessThanOrEqualTo(root.get("criadoEm"), filtro.criadoAte()));
        }
        if (filtro.accessScope() == DocumentoAccessScope.MOTORISTA) {
            Subquery<Integer> viagens = criteria.subquery(Integer.class);
            Root<RelatorioViagemEntity> relatorio = viagens.from(RelatorioViagemEntity.class);
            viagens.select(relatorio.get("id")).where(
                    builder.equal(relatorio.get("motoristaId"), filtro.usuarioEscopoId())
            );
            predicates.add(root.get("viagemId").in(viagens));
        } else if (filtro.accessScope() == DocumentoAccessScope.RESPONSAVEL) {
            predicates.add(builder.or(
                    builder.equal(root.get("criadoPor"), filtro.usuarioEscopoId()),
                    builder.equal(root.get("assinanteId"), filtro.usuarioEscopoId())
            ));
        }
        if (cursor != null) {
            predicates.add(builder.or(
                    builder.greaterThan(root.get("criadoEm"), cursor.criadoEm()),
                    builder.and(
                            builder.equal(root.get("criadoEm"), cursor.criadoEm()),
                            builder.greaterThan(root.get("id"), cursor.id())
                    )
            ));
        }
        return predicates;
    }

    private List<jakarta.persistence.criteria.Order> ordenacao(
            CriteriaBuilder builder,
            Root<DocumentoEntity> root,
            Sort sort
    ) {
        return sort.stream()
                .map(order -> {
                    Path<?> path = switch (order.getProperty()) {
                        case "id" -> root.get("id");
                        case "criadoEm" -> root.get("criadoEm");
                        case "atualizadoEm" -> root.get("atualizadoEm");
                        case "tipoDocumento" -> root.get("tipoDocumento");
                        case "origem" -> root.get("origem");
                        default -> throw new IllegalArgumentException("Campo de ordenação não permitido.");
                    };
                    return order.isAscending() ? builder.asc(path) : builder.desc(path);
                })
                .toList();
    }
}
