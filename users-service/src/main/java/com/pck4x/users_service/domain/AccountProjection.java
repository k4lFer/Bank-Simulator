package com.pck4x.users_service.domain;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class AccountProjection {
    private UUID id;
    private String accountNumber;
    private BigDecimal balance;
    private String currency;
    private String status;
    private UUID userId;
    private LocalDateTime createdAt;

    public AccountProjection() {}

    public AccountProjection(UUID id, String accountNumber, BigDecimal balance,
                             String currency, String status, UUID userId,
                             LocalDateTime createdAt) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.currency = currency;
        this.status = status;
        this.userId = userId;
        this.createdAt = createdAt;
    }
}
