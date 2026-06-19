package com.pck4x.accounts_service.application.feature.linkaccount;

import com.pck4x.accounts_service.application.dto.command.LinkAccountCommand;
import com.pck4x.accounts_service.application.dto.response.CardDetailResponse;
import com.pck4x.accounts_service.application.mapper.CardDtoMapper;
import com.pck4x.accounts_service.application.port.output.AccountRepository;
import com.pck4x.accounts_service.application.port.output.CardAccountRepository;
import com.pck4x.accounts_service.application.port.output.CardRepository;
import com.pck4x.accounts_service.domain.CardAccount;
import com.pck4x.sharedcontracts.result.OutputPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class LinkAccountService implements LinkAccountUseCase {

    private final CardRepository cardRepository;
    private final CardAccountRepository cardAccountRepository;
    private final AccountRepository accountRepository;

    public LinkAccountService(CardRepository cardRepository,
                              CardAccountRepository cardAccountRepository,
                              AccountRepository accountRepository) {
        this.cardRepository = cardRepository;
        this.cardAccountRepository = cardAccountRepository;
        this.accountRepository = accountRepository;
    }

    @Override
    @Transactional
    public OutputPort<CardDetailResponse> execute(UUID cardId, LinkAccountCommand command, UUID userId) {
        var optCard = cardRepository.findByIdAndUserId(cardId, userId);
        if (optCard.isEmpty()) {
            return OutputPort.notFound("Card not found");
        }

        var optAccount = accountRepository.findByIdAndUserId(command.getAccountId(), userId);
        if (optAccount.isEmpty()) {
            return OutputPort.notFound("Account not found or does not belong to you");
        }

        var card = optCard.get();
        var account = optAccount.get();

        var existing = cardAccountRepository.findByCardIdAndAccountId(cardId, command.getAccountId());
        if (existing.isPresent()) {
            return OutputPort.conflict("Account already linked to this card");
        }

        if (command.isPrimary()) {
            var linkedAccounts = cardAccountRepository.findByCardId(cardId);
            linkedAccounts.forEach(ca -> {
                ca.setPrimary(false);
                cardAccountRepository.save(ca);
            });
        }

        cardAccountRepository.save(new CardAccount(cardId, command.getAccountId(), command.isPrimary(), account.getCurrency()));

        var linkedAccounts = cardAccountRepository.findByCardId(cardId);
        return OutputPort.created(CardDtoMapper.INSTANCE.toDetailResponse(card, linkedAccounts), "Account linked to card");
    }
}
