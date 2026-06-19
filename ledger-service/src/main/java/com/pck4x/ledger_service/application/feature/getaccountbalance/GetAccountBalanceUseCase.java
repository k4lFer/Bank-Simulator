package com.pck4x.ledger_service.application.feature.getaccountbalance;

import com.pck4x.ledger_service.application.dto.response.AccountBalanceResponse;
import com.pck4x.sharedcontracts.result.OutputPort;

public interface GetAccountBalanceUseCase {
    OutputPort<AccountBalanceResponse> execute(String accountNumber);
}
