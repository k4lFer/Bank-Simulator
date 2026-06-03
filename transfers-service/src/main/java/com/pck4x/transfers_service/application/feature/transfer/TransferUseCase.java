package com.pck4x.transfers_service.application.feature.transfer;

import java.util.UUID;

import com.pck4x.sharedcontracts.result.OutputPort;
import com.pck4x.transfers_service.application.dto.command.TransferCommand;
import com.pck4x.transfers_service.application.dto.response.TransferResponse;

public interface TransferUseCase {
    OutputPort<TransferResponse> execute(TransferCommand input, UUID userId);
}
