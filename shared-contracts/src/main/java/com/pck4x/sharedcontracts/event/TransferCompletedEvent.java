package com.pck4x.sharedcontracts.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferCompletedEvent implements Serializable, DomainEvent {
    private UUID eventId = UUID.randomUUID();

    public UUID getEventId() {
        return eventId;
    }
    private UUID correlationId;

    private UUID transferId;
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private String currency;

    public TransferCompletedEvent() {}

    public TransferCompletedEvent(UUID transferId, String fromAccount, String toAccount,
                                  BigDecimal amount, String currency) {
        this.transferId = transferId;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.currency = currency;
    }
}
