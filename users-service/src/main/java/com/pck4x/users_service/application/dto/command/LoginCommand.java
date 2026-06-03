package com.pck4x.users_service.application.dto.command;

import lombok.Getter;

@Getter
public class LoginCommand {
    public String email;
    public String password;
}
