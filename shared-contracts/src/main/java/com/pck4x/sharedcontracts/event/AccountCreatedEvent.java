package com.pck4x.sharedcontracts.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountCreatedEvent implements Serializable {
    private UUID id;
    private String accountNumber;
    private BigDecimal balance;
    private String currency;
    private String status;
    private UUID userId;
    private String pin6;
    private String pin4;
    private String createdAt;

    public AccountCreatedEvent() {}

    public AccountCreatedEvent(UUID id, String accountNumber, BigDecimal balance,
                               String currency, String status, UUID userId,
                               String pin6, String pin4, String createdAt) {
        this.id = id;
        this.accountNumber = accountNumber;
        this.balance = balance;
        this.currency = currency;
        this.status = status;
        this.userId = userId;
        this.pin6 = pin6;
        this.pin4 = pin4;
        this.createdAt = createdAt;
    }

}
