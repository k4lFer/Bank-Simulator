package com.pck4x.ledger_service.application.feature.getentriesbytransfer;

import com.pck4x.ledger_service.application.dto.response.LedgerEntryResponse;
import com.pck4x.ledger_service.application.mapper.LedgerDtoMapper;
import com.pck4x.ledger_service.application.port.output.LedgerRepository;
import com.pck4x.sharedcontracts.result.OutputPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GetEntriesByTransferService implements GetEntriesByTransferUseCase {

    private final LedgerRepository ledgerRepository;

    public GetEntriesByTransferService(LedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OutputPort<List<LedgerEntryResponse>> execute(UUID transferId) {
        var entries = ledgerRepository.findByTransferId(transferId);
        return OutputPort.ok(LedgerDtoMapper.INSTANCE.toResponseList(entries));
    }
}
