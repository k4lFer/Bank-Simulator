package com.pck4x.transfers_service.infrastructure.persistence.jpa.entities;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.pck4x.transfers_service.domain.enums.TransferStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "transfers")
@Getter
@Setter
public class TransferEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "transfer_id", nullable = false, unique = true)
    private UUID transferId;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "to_user_id")
    private UUID toUserId;

    @Column(name = "from_account", length = 20)
    private String fromAccount;

    @Column(name = "to_account", nullable = false, length = 20)
    private String toAccount;

    @Column(name = "amount", nullable = false, precision = 18, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "description")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private TransferStatus status;

    @Column(name = "rejection_reason", length = 255)
    private String rejectionReason;

    @Column(name="created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name="updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // Fase 2: NO mapear el outbox como coleccion con orphanRemoval.
    // Al guardar de nuevo la transferencia, Hibernate borraba los eventos
    // pendientes/enviados del outbox. El repositorio de eventos es el unico dueno.
}
