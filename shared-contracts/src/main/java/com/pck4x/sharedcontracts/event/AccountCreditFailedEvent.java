package com.pck4x.sharedcontracts.event;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

/**
 * Fase 2.5: fallo al acreditar DESPUES de un debito confirmado.
 * No es un rechazo: el origen ya fue debitado, por lo que el orquestador
 * debe pasar a COMPENSATING en lugar de REJECTED.
 */
@Getter
@Setter
public class AccountCreditFailedEvent implements Serializable, DomainEvent {
    private UUID eventId = UUID.randomUUID();

    public UUID getEventId() {
        return eventId;
    }
    private UUID correlationId;

    private UUID transferId;
    private String accountNumber;
    private String reason;
    private BigDecimal amount;
    private String currency;

    public AccountCreditFailedEvent() {}

    public AccountCreditFailedEvent(UUID transferId, String accountNumber, String reason,
                                    BigDecimal amount, String currency) {
        this.transferId = transferId;
        this.accountNumber = accountNumber;
        this.reason = reason;
        this.amount = amount;
        this.currency = currency;
    }
}
