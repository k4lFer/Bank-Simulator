package com.pck4x.sharedcontracts.event;

import java.io.Serializable;
import java.util.UUID;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCreatedEvent implements Serializable {
    private UUID userId;
    private String email;
    private String fullName;
    private String role;

    public UserCreatedEvent() {
    }

    public UserCreatedEvent(UUID userId, String email, String fullName, String role) {
        this.userId = userId;
        this.email = email;
        this.fullName = fullName;
        this.role = role;
    }

}
