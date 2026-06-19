package com.pck4x.sharedcontracts.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountCreditedEvent implements Serializable {
    private UUID transferId;
    private UUID accountId;
    private String accountNumber;
    private BigDecimal amount;
    private BigDecimal balanceAfter;
    private String currency;
    private UUID toUserId;

    public AccountCreditedEvent() {}

    public AccountCreditedEvent(UUID transferId, UUID accountId, String accountNumber, BigDecimal amount,
                                BigDecimal balanceAfter, String currency, UUID toUserId) {
        this.transferId = transferId;
        this.accountId = accountId;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.balanceAfter = balanceAfter;
        this.currency = currency;
        this.toUserId = toUserId;
    }
}
