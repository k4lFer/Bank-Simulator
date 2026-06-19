package com.pck4x.ledger_service.application.feature.getdailyreport;

import com.pck4x.ledger_service.application.dto.response.DailyReportResponse;
import com.pck4x.ledger_service.application.mapper.LedgerDtoMapper;
import com.pck4x.ledger_service.application.port.output.LedgerRepository;
import com.pck4x.sharedcontracts.result.OutputPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;

@Service
public class GetDailyReportService implements GetDailyReportUseCase {

    private final LedgerRepository ledgerRepository;

    public GetDailyReportService(LedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OutputPort<DailyReportResponse> execute(LocalDate date) {
        var start = date.atStartOfDay();
        var end = date.atTime(LocalTime.MAX);

        var dayEntries = ledgerRepository.findByCreatedAtBetween(start, end);
        var beforeEntries = ledgerRepository.findByCreatedAtBefore(start);

        var report = LedgerDtoMapper.INSTANCE.toDailyReport(date, dayEntries, beforeEntries);
        return OutputPort.ok(report);
    }
}
