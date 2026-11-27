package com.pck4x.sharedcontracts.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

/** Fase 2.6: confirma que el debito fue revertido con un movimiento REVERSAL. */
@Getter
@Setter
public class AccountDebitCompensatedEvent implements Serializable, DomainEvent {
    private UUID eventId = UUID.randomUUID();

    public UUID getEventId() {
        return eventId;
    }
    private UUID correlationId;

    private UUID transferId;
    private String accountNumber;
    private BigDecimal amount;
    private String currency;
    private String reason;
    /** Numero del movimiento original que se revierte. */
    private String reversalOf;

    public AccountDebitCompensatedEvent() {}

    public AccountDebitCompensatedEvent(UUID transferId, String accountNumber, BigDecimal amount,
                                        String currency, String reason, String reversalOf) {
        this.transferId = transferId;
        this.accountNumber = accountNumber;
        this.amount = amount;
        this.currency = currency;
        this.reason = reason;
        this.reversalOf = reversalOf;
    }
}
