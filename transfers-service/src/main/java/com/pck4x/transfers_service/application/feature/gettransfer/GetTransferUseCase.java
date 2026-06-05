package com.pck4x.transfers_service.application.feature.gettransfer;

import java.util.UUID;

import com.pck4x.sharedcontracts.result.OutputPort;
import com.pck4x.transfers_service.application.dto.response.TransferResponse;

public interface GetTransferUseCase {
    OutputPort<TransferResponse> execute(UUID transferId);
}
