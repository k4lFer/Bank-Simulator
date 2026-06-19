package com.pck4x.accounts_service.application.dto.command;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class IssueCardCommand {
    private String pin4;
    private BigDecimal dailyLimit;
}
