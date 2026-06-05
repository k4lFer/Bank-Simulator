package com.pck4x.transfers_service.application.feature.transfer;

import com.pck4x.sharedcontracts.result.OutputPort;
import com.pck4x.transfers_service.application.dto.response.TransferResponse;

import java.util.UUID;

public interface GetTransferUseCase {
    OutputPort<TransferResponse> execute(UUID transferId);
}
