package com.pck4x.ledger_service.application.feature.getentriesbyaccount;

import com.pck4x.ledger_service.application.dto.response.LedgerEntryResponse;
import com.pck4x.sharedcontracts.result.OutputPort;

import java.util.List;

public interface GetEntriesByAccountUseCase {
    OutputPort<List<LedgerEntryResponse>> execute(String accountNumber);
}
