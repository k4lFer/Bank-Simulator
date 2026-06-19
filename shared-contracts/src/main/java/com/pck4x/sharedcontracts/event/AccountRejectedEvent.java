package com.pck4x.sharedcontracts.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountRejectedEvent implements Serializable {
    private UUID transferId;
    private String accountNumber;
    private String reason;
    private BigDecimal amount;
    private String currency;

    public AccountRejectedEvent() {}

    public AccountRejectedEvent(UUID transferId, String accountNumber, String reason) {
        this.transferId = transferId;
        this.accountNumber = accountNumber;
        this.reason = reason;
    }

    public AccountRejectedEvent(UUID transferId, String accountNumber, String reason,
                                 BigDecimal amount, String currency) {
        this.transferId = transferId;
        this.accountNumber = accountNumber;
        this.reason = reason;
        this.amount = amount;
        this.currency = currency;
    }
}
