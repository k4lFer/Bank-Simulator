package com.pck4x.accounts_service.application.feature.issuecard;

import com.pck4x.accounts_service.application.dto.command.IssueCardCommand;
import com.pck4x.accounts_service.application.dto.response.CardResponse;
import com.pck4x.sharedcontracts.result.OutputPort;

import java.util.UUID;

public interface IssueCardUseCase {
    OutputPort<CardResponse> execute(IssueCardCommand command, UUID userId);
}
