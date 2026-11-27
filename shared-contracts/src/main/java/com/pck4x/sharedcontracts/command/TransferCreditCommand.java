package com.pck4x.sharedcontracts.command;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

/** Fase 2.3: orden del orquestador a accounts para acreditar la cuenta destino. */
@Getter
@Setter
public class TransferCreditCommand implements Serializable, SagaCommand {
    private UUID commandId = UUID.randomUUID();
    private UUID correlationId;
    private UUID transferId;
    private String toAccount;
    private String fromAccount;
    private BigDecimal amount;
    private String currency;
    private String description;
    private UUID toUserId;

    public TransferCreditCommand() {}

    public TransferCreditCommand(UUID transferId, String fromAccount, String toAccount,
                                 BigDecimal amount, String currency, String description) {
        this.transferId = transferId;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
    }
}
