package com.pck4x.accounts_service.application.feature.getcardsbyuser;

import com.pck4x.accounts_service.application.dto.response.CardResponse;
import com.pck4x.sharedcontracts.result.OutputPort;

import java.util.List;
import java.util.UUID;

public interface GetCardsByUserUseCase {
    OutputPort<List<CardResponse>> execute(UUID userId);
}
