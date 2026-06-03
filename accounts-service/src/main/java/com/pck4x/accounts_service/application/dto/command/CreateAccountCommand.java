package com.pck4x.accounts_service.application.dto.command;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateAccountCommand {
    private String currency;
    private String pin6;
    private String pin4;
}
