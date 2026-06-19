package com.pck4x.ledger_service.application.feature.getallentries;

import com.pck4x.ledger_service.application.dto.response.LedgerEntryResponse;
import com.pck4x.sharedcontracts.result.OutputPort;

import java.util.List;

public interface GetAllEntriesUseCase {
    OutputPort<List<LedgerEntryResponse>> execute();
}
