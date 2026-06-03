package com.pck4x.users_service.application.feature.register;

import com.pck4x.sharedcontracts.result.OutputPort;
import com.pck4x.users_service.application.dto.command.RegisterAdminCommand;

import java.util.UUID;

public interface RegisterAdminUseCase {
    OutputPort<UUID> execute(RegisterAdminCommand command);
}
