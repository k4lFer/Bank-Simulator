package com.pck4x.sharedcontracts.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferRequestedEvent implements Serializable {
    private UUID transferId;
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String pin4;
    private String transferType;
    private UUID userId;
    private UUID cardId;

    public TransferRequestedEvent() {}

    public TransferRequestedEvent(UUID transferId, String fromAccount, String toAccount,
                                  BigDecimal amount, String currency, String description) {
        this.transferId = transferId;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.transferType = "EXTERNAL";
    }
}
