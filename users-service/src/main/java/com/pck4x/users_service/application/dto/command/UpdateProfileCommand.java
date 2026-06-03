package com.pck4x.users_service.application.dto.command;

import lombok.Getter;

import java.util.Date;

@Getter
public class UpdateProfileCommand {
    private Date dateOfBirth;
    private String address;
    private String idDocument;
    private String occupation;
}
