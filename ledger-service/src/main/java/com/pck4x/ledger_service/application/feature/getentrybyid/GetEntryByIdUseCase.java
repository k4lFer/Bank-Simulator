package com.pck4x.ledger_service.application.feature.getentrybyid;

import com.pck4x.ledger_service.application.dto.response.LedgerEntryResponse;
import com.pck4x.sharedcontracts.result.OutputPort;

public interface GetEntryByIdUseCase {
    OutputPort<LedgerEntryResponse> execute(Long id);
}
