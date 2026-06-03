package com.pck4x.users_service.application.dto.command;

import lombok.Getter;

import java.util.Date;

@Getter
public class RegisterUserCommand {
    public String firstName;
    public String lastName;
    public String email;
    public String password;
    public String phone;
    public Date dateOfBirth;
    public String address;
    public String idDocument;
    public String occupation;

}
