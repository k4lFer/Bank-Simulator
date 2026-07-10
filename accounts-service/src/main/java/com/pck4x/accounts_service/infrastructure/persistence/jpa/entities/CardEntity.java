package com.pck4x.accounts_service.infrastructure.persistence.jpa.entities;

import com.pck4x.accounts_service.domain.enums.CardStatus;
import com.pck4x.accounts_service.infrastructure.persistence.jpa.converters.YearMonthConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

@Entity
@Table(name = "cards")
@Getter
@Setter
public class CardEntity {
    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "pan", nullable = false, unique = true, length = 19)
    private String pan;

    @Convert(converter = YearMonthConverter.class)
    @Column(name = "expiry_date", nullable = false, length = 7)
    private YearMonth expiryDate;

    @Column(name = "pin4", nullable = false, length = 4)
    private String pin4;

    @Column(name = "pin6", nullable = false, length = 6)
    private String pin6;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private CardStatus status;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
