package com.pck4x.sharedcontracts.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferFailedEvent implements Serializable {
    private UUID transferId;
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private String currency;
    private String reason;

    public TransferFailedEvent() {}

    public TransferFailedEvent(UUID transferId, String fromAccount, String toAccount,
                               BigDecimal amount, String currency, String reason) {
        this.transferId = transferId;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.currency = currency;
        this.reason = reason;
    }
}
