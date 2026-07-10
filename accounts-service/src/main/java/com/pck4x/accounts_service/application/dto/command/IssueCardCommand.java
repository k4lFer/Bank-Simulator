package com.pck4x.accounts_service.application.dto.command;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IssueCardCommand {
    private String pin4;
    private String pin6;
}
