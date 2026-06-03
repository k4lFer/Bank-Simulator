package com.pck4x.accounts_service.application.feature.depositmoney;

import java.util.UUID;

import com.pck4x.accounts_service.application.dto.command.DepositCommand;
import com.pck4x.accounts_service.application.dto.response.DepositResponse;
import com.pck4x.sharedcontracts.result.OutputPort;

public interface DepositMoneyUseCase {
    OutputPort<DepositResponse> execute(DepositCommand command, UUID userId);
}
