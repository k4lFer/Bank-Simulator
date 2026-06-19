package com.pck4x.accounts_service.domain;

import com.pck4x.accounts_service.domain.enums.CardStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

@Getter
@Setter
public class Card {
    private UUID id;
    private UUID userId;
    private String pan;
    private YearMonth expiryDate;
    private String pin4;
    private CardStatus status;
    private BigDecimal dailyLimit;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Card() {}

    public Card(UUID id, UUID userId, String pan, YearMonth expiryDate, String pin4, BigDecimal dailyLimit) {
        this.id = id;
        this.userId = userId;
        this.pan = pan;
        this.expiryDate = expiryDate;
        this.pin4 = pin4;
        this.dailyLimit = dailyLimit;
        this.status = CardStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
