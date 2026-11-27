package com.pck4x.sharedcontracts.command;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

/** Fase 2.3: orden del orquestador para revertir un debito ya confirmado. */
@Getter
@Setter
public class TransferCompensateCommand implements Serializable, SagaCommand {
    private UUID commandId = UUID.randomUUID();
    private UUID correlationId;
    private UUID transferId;
    private String fromAccount;
    private BigDecimal amount;
    private String currency;
    private String reason;

    public TransferCompensateCommand() {}

    public TransferCompensateCommand(UUID transferId, String fromAccount, BigDecimal amount,
                                     String currency, String reason) {
        this.transferId = transferId;
        this.fromAccount = fromAccount;
        this.amount = amount;
        this.currency = currency;
        this.reason = reason;
    }
}
