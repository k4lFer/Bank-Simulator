package com.pck4x.accounts_service.application.dto.command;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangePinCommand {
    private String currentPin4;
    private String newPin4;
}
