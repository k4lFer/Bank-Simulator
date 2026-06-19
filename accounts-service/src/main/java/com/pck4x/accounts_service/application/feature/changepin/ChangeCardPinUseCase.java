package com.pck4x.accounts_service.application.feature.changepin;

import com.pck4x.accounts_service.application.dto.command.ChangePinCommand;
import com.pck4x.accounts_service.application.dto.response.CardResponse;
import com.pck4x.sharedcontracts.result.OutputPort;

import java.util.UUID;

public interface ChangeCardPinUseCase {
    OutputPort<CardResponse> execute(UUID cardId, ChangePinCommand command, UUID userId);
}
