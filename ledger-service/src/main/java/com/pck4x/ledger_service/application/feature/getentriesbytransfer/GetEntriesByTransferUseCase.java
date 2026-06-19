package com.pck4x.ledger_service.application.feature.getentriesbytransfer;

import com.pck4x.ledger_service.application.dto.response.LedgerEntryResponse;
import com.pck4x.sharedcontracts.result.OutputPort;

import java.util.List;
import java.util.UUID;

public interface GetEntriesByTransferUseCase {
    OutputPort<List<LedgerEntryResponse>> execute(UUID transferId);
}
