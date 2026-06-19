package com.pck4x.ledger_service.application.feature.getentriesbyaccount;

import com.pck4x.ledger_service.application.dto.response.LedgerEntryResponse;
import com.pck4x.ledger_service.application.mapper.LedgerDtoMapper;
import com.pck4x.ledger_service.application.port.output.LedgerRepository;
import com.pck4x.sharedcontracts.result.OutputPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GetEntriesByAccountService implements GetEntriesByAccountUseCase {

    private final LedgerRepository ledgerRepository;

    public GetEntriesByAccountService(LedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OutputPort<List<LedgerEntryResponse>> execute(String accountNumber) {
        var entries = ledgerRepository.findByAccountNumber(accountNumber);
        return OutputPort.ok(LedgerDtoMapper.INSTANCE.toResponseList(entries));
    }
}
