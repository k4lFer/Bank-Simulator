package com.pck4x.accounts_service.application.feature.getmovements;

import com.pck4x.accounts_service.application.dto.response.MovementItemResponse;
import com.pck4x.sharedcontracts.result.OutputPort;

import java.util.List;
import java.util.UUID;

public interface GetMovementsUseCase {
    OutputPort<List<MovementItemResponse>> execute(UUID accountId, UUID userId);
}
