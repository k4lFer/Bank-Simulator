package com.pck4x.ledger_service.application.feature.getdailyreport;

import com.pck4x.ledger_service.application.dto.response.DailyReportResponse;
import com.pck4x.sharedcontracts.result.OutputPort;

import java.time.LocalDate;

public interface GetDailyReportUseCase {
    OutputPort<DailyReportResponse> execute(LocalDate date);
}
