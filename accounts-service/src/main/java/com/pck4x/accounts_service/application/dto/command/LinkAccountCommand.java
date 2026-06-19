package com.pck4x.accounts_service.application.dto.command;

import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class LinkAccountCommand {
    private UUID accountId;
    private boolean primary;
}
