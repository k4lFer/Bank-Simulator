package com.pck4x.ledger_service.domain;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LedgerEntries {
    private Long id;
    private UUID transferId;
    private String accountNumber;
    private String entryType;
    private BigDecimal amount;
    private String currency;
    private LocalDateTime createdAt;

    public LedgerEntries() {}

    public LedgerEntries(UUID transferId, String accountNumber, String entryType, BigDecimal amount, String currency) {
        this.transferId = transferId;
        this.accountNumber = accountNumber;
        this.entryType = entryType;
        this.amount = amount;
        this.currency = currency;
        this.createdAt = LocalDateTime.now();
    }
}
