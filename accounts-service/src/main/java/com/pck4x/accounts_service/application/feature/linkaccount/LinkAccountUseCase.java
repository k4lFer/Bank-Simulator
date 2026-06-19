package com.pck4x.accounts_service.application.feature.linkaccount;

import com.pck4x.accounts_service.application.dto.command.LinkAccountCommand;
import com.pck4x.accounts_service.application.dto.response.CardDetailResponse;
import com.pck4x.sharedcontracts.result.OutputPort;

import java.util.UUID;

public interface LinkAccountUseCase {
    OutputPort<CardDetailResponse> execute(UUID cardId, LinkAccountCommand command, UUID userId);
}
