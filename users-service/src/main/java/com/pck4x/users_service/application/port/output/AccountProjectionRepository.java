package com.pck4x.users_service.application.port.output;

import com.pck4x.sharedcontracts.objects.QueryResult;
import com.pck4x.users_service.application.dto.response.AccountResponse;
import com.pck4x.users_service.application.dto.response.UserAccountsResponse;
import com.pck4x.users_service.domain.AccountProjection;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountProjectionRepository {
    void save(AccountProjection projection);
    Optional<AccountProjection> findById(UUID id);
    List<AccountProjection> findByUserId(UUID userId);
    UserAccountsResponse findUserAccountsByUserId(UUID userId);
    QueryResult<List<AccountResponse>> findAccountsByUserId(UUID userId, int page, int size);
}
