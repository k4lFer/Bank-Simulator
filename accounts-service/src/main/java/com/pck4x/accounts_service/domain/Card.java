package com.pck4x.accounts_service.domain;

import com.pck4x.accounts_service.domain.enums.CardStatus;
import lombok.Getter;
import lombok.Setter;

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
    private String pin6;
    private CardStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Card() {}

    public Card(UUID id, UUID userId, String pan, YearMonth expiryDate, String pin4, String pin6) {
        this.id = id;
        this.userId = userId;
        this.pan = pan;
        this.expiryDate = expiryDate;
        this.pin4 = pin4;
        this.pin6 = pin6;
        this.status = CardStatus.ACTIVE;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
