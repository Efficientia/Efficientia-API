package com.example.efficientia.documento.audit;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentoAuditRepository extends JpaRepository<DocumentoAuditEntity, UUID> {
}
