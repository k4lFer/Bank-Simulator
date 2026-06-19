package com.pck4x.ledger_service.application.feature.getaccountbalance;

import com.pck4x.ledger_service.application.dto.response.AccountBalanceResponse;
import com.pck4x.ledger_service.application.mapper.LedgerDtoMapper;
import com.pck4x.ledger_service.application.port.output.LedgerRepository;
import com.pck4x.sharedcontracts.result.OutputPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
public class GetAccountBalanceService implements GetAccountBalanceUseCase {

    private final LedgerRepository ledgerRepository;

    public GetAccountBalanceService(LedgerRepository ledgerRepository) {
        this.ledgerRepository = ledgerRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OutputPort<AccountBalanceResponse> execute(String accountNumber) {
        var entries = ledgerRepository.findByAccountNumber(accountNumber);

        if (entries.isEmpty()) {
            return OutputPort.notFound("No entries found for account: " + accountNumber);
        }

        String currency = entries.getFirst().getCurrency();
        BigDecimal balance = LedgerDtoMapper.INSTANCE.calculateBalance(entries);

        return OutputPort.ok(
                LedgerDtoMapper.INSTANCE.toBalanceResponse(accountNumber, currency, balance)
        );
    }
}
