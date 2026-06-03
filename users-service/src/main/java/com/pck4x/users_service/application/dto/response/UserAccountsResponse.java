package com.pck4x.users_service.application.dto.response;

import java.util.List;
import java.util.UUID;

public record UserAccountsResponse(
        UUID userId,
        String firstName,
        String lastName,
        List<AccountResponse> accounts
) {}
