package com.pck4x.transfers_service.application.feature.externaltransfer;

import com.pck4x.sharedcontracts.result.OutputPort;
import com.pck4x.transfers_service.application.dto.command.TransferCommand;
import com.pck4x.transfers_service.application.dto.response.TransferResponse;

import java.util.UUID;

public interface ExternalTransferUseCase {
    OutputPort<TransferResponse> execute(TransferCommand input, UUID userId);
}
