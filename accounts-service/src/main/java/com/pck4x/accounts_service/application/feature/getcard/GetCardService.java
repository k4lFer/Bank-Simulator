package com.pck4x.accounts_service.application.feature.getcard;

import com.pck4x.accounts_service.application.dto.response.CardDetailResponse;
import com.pck4x.accounts_service.application.mapper.CardDtoMapper;
import com.pck4x.accounts_service.application.port.output.CardAccountRepository;
import com.pck4x.accounts_service.application.port.output.CardRepository;
import com.pck4x.sharedcontracts.result.OutputPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class GetCardService implements GetCardUseCase {

    private final CardRepository cardRepository;
    private final CardAccountRepository cardAccountRepository;

    public GetCardService(CardRepository cardRepository, CardAccountRepository cardAccountRepository) {
        this.cardRepository = cardRepository;
        this.cardAccountRepository = cardAccountRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OutputPort<CardDetailResponse> execute(UUID cardId, UUID userId) {
        var optCard = cardRepository.findByIdAndUserId(cardId, userId);
        if (optCard.isEmpty()) {
            return OutputPort.notFound("Card not found");
        }

        var card = optCard.get();
        var linkedAccounts = cardAccountRepository.findByCardId(cardId);

        return OutputPort.ok(CardDtoMapper.INSTANCE.toDetailResponse(card, linkedAccounts));
    }
}
