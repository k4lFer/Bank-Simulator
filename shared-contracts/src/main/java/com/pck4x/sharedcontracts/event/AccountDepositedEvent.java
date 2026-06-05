package com.pck4x.sharedcontracts.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountDepositedEvent implements Serializable {
    private UUID accountId;
    private String accountNumber;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String currency;

    public AccountDepositedEvent() {}

    public AccountDepositedEvent(UUID accountId, String accountNumber, BigDecimal amount,
                                 BigDecimal balanceAfter, String currency) {
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.currency = currency;
    }
}
