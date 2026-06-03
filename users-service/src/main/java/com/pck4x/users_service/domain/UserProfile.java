package com.pck4x.users_service.domain;

import java.time.LocalDateTime;
import java.util.Date;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserProfile {
    private Date dateOfBirth;
    private String address;
    private String idDocument;
    private String occupation;
    private LocalDateTime createdAt;

    public UserProfile() {
    }

    public UserProfile(Date dateOfBirth, String address, String idDocument, String occupation) {
        this.dateOfBirth = dateOfBirth;
        this.address = address;
        this.idDocument = idDocument;
        this.occupation = occupation;
        this.createdAt = LocalDateTime.now();
    }

}
