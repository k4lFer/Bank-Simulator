package com.pck4x.ledger_service.application.dto.response;

import java.math.BigDecimal;

public record DailyReportItemResponse(
        String accountNumber,
        String currency,
        BigDecimal openingBalance,
        BigDecimal totalDebits,
        BigDecimal totalCredits,
        BigDecimal closingBalance
) {}
