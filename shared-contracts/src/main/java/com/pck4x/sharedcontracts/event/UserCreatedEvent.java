package com.pck4x.sharedcontracts.event;

import java.io.Serializable;
import java.util.UUID;

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

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}
