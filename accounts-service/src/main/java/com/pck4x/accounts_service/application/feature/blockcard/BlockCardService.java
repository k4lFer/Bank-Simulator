package com.pck4x.accounts_service.application.feature.blockcard;

import com.pck4x.accounts_service.application.dto.response.CardResponse;
import com.pck4x.accounts_service.application.mapper.CardDtoMapper;
import com.pck4x.accounts_service.application.port.output.CardRepository;
import com.pck4x.accounts_service.domain.enums.CardStatus;
import com.pck4x.sharedcontracts.result.OutputPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class BlockCardService implements BlockCardUseCase {

    private final CardRepository cardRepository;

    public BlockCardService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    @Override
    @Transactional
    public OutputPort<CardResponse> execute(UUID cardId, UUID userId) {
        var optCard = cardRepository.findByIdAndUserId(cardId, userId);
        if (optCard.isEmpty()) {
            return OutputPort.notFound("Card not found");
        }

        var card = optCard.get();
        if (card.getStatus() == CardStatus.BLOCKED) {
            return OutputPort.conflict("Card is already blocked");
        }

        card.setStatus(CardStatus.BLOCKED);
        card.setUpdatedAt(LocalDateTime.now());
        card = cardRepository.save(card);

        return OutputPort.ok(
                CardDtoMapper.INSTANCE.toResponse(card),
                "Card blocked successfully"
        );
    }
}
