package com.pck4x.accounts_service.application.dto.command;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class DepositCommand {
    private String accountNumber;
    private String currency;
    private String pin4;
    private BigDecimal amount;
}
