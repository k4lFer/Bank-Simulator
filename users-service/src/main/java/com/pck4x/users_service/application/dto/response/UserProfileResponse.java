package com.pck4x.users_service.application.dto.response;

import java.time.LocalDateTime;
import java.util.Date;

public record UserProfileResponse (
        Date dateOfBirth,
        String address,
        String idDocument,
        String occupation,
        LocalDateTime createdAt
) { }
