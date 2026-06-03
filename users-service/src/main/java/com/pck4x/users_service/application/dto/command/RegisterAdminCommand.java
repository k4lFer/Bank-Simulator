package com.pck4x.users_service.application.dto.command;

import lombok.Getter;

@Getter
public class RegisterAdminCommand {
    public String firstName;
    public String lastName;
    public String email;
    public String password;
    public String phone;
}
