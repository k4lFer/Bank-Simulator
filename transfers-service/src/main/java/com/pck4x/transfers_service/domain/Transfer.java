package com.pck4x.transfers_service.domain;

import com.pck4x.transfers_service.domain.enums.TransferStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
public class Transfer {
    private Long id;
    private UUID transferId;
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private String currency;
    private String description;
    private TransferStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public Transfer() {}

    public Transfer(UUID transferId, String fromAccount, String toAccount, BigDecimal amount, String currency, String description) {
        this.transferId = transferId;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.status = TransferStatus.PENDING;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }
}
