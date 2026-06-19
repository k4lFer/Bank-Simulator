package com.pck4x.ledger_service.application.dto.response;

import java.time.LocalDate;
import java.util.List;

public record DailyReportResponse(
        LocalDate date,
        int totalAccounts,
        int totalEntries,
        List<DailyReportItemResponse> accounts
) {}
