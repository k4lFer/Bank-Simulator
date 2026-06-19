package com.pck4x.accounts_service.domain;

import com.pck4x.sharedcontracts.enums.AccountStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class Account {
    private UUID id;
    private String accountNumber;
    private BigDecimal balance;
    private String currency;
    private AccountStatus status;
    private UUID userId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Account() {}

    public Account(UUID id, String accountNumber, String currency, UUID userId) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.balance = BigDecimal.ZERO;
        this.currency = currency;
        this.status = AccountStatus.ACTIVE;
        this.userId = userId;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
