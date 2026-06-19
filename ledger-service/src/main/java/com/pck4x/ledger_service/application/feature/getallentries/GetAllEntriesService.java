package com.pck4x.ledger_service.application.feature.getallentries;

import com.pck4x.ledger_service.application.dto.response.LedgerEntryResponse;
import com.pck4x.ledger_service.application.mapper.LedgerDtoMapper;
import com.pck4x.ledger_service.application.port.output.LedgerRepository;
import com.pck4x.sharedcontracts.result.OutputPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class GetAllEntriesService implements GetAllEntriesUseCase {

    private final LedgerRepository ledgerRepository;

    public GetAllEntriesService(LedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OutputPort<List<LedgerEntryResponse>> execute() {
        var entries = ledgerRepository.findAll();
        return OutputPort.ok(LedgerDtoMapper.INSTANCE.toResponseList(entries));
    }
}
