package com.pck4x.accounts_service.application.feature.updatepin;

import com.pck4x.accounts_service.application.dto.command.UpdatePinCommand;
import com.pck4x.accounts_service.application.dto.response.PinChangeResponse;
import com.pck4x.sharedcontracts.result.OutputPort;

import java.util.UUID;

public interface UpdatePinUseCase {
    OutputPort<PinChangeResponse> execute(UUID accountId, UpdatePinCommand command, UUID userId);
}
