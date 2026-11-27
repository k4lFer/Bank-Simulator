package com.pck4x.sharedcontracts.command;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

/** Fase 2.3: orden del orquestador a accounts para validar y debitar la cuenta origen. */
@Getter
@Setter
public class TransferDebitCommand implements Serializable, SagaCommand {
    private UUID commandId = UUID.randomUUID();
    private UUID correlationId;
    private UUID transferId;
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String pin4;
    private UUID cardId;
    private UUID userId;
    private String transferType;

    public TransferDebitCommand() {}

    public TransferDebitCommand(UUID transferId, String fromAccount, String toAccount,
                                BigDecimal amount, String currency, String description) {
        this.transferId = transferId;
        this.fromAccount = fromAccount;
        this.toAccount = toAccount;
        this.amount = amount;
        this.currency = currency;
        this.description = description;
        this.transferType = "INTERNAL";
    }
}
