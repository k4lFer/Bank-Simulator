package com.pck4x.transfers_service.application.feature.internaltransfer;

import com.pck4x.sharedcontracts.result.OutputPort;
import com.pck4x.transfers_service.application.dto.command.TransferCommand;
import com.pck4x.transfers_service.application.dto.response.TransferResponse;

import java.util.UUID;

public interface InternalTransferUseCase {
    OutputPort<TransferResponse> execute(TransferCommand input, UUID idempotencyKey, UUID userId);
}
