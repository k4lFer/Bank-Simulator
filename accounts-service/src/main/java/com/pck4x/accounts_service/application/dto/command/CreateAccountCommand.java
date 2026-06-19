package com.pck4x.accounts_service.application.dto.command;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
public class CreateAccountCommand {
    @NotBlank
    private String currency;

    @NotNull
    private UUID cardId;
}
