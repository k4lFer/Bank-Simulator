package com.pck4x.transfers_service.application.dto.command;

import java.math.BigDecimal;
import java.util.UUID;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TransferCommand {
    private String fromAccount;
    private String toAccount;
    private BigDecimal amount;
    private String currency;
    private String description;
    private String pin4;
    private UUID cardId;
}
