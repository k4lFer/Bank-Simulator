package com.pck4x.accounts_service.application.feature.changepin;

import com.pck4x.accounts_service.application.dto.command.ChangePinCommand;
import com.pck4x.accounts_service.application.dto.response.CardResponse;
import com.pck4x.accounts_service.application.mapper.CardDtoMapper;
import com.pck4x.accounts_service.application.port.output.CardRepository;
import com.pck4x.sharedcontracts.result.OutputPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class ChangeCardPinService implements ChangeCardPinUseCase {

    private final CardRepository cardRepository;

    public ChangeCardPinService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    @Override
    @Transactional
    public OutputPort<CardResponse> execute(UUID cardId, ChangePinCommand command, UUID userId) {
        var optCard = cardRepository.findByIdAndUserId(cardId, userId);
        if (optCard.isEmpty()) {
            return OutputPort.notFound("Card not found");
        }

        var card = optCard.get();

        if (!card.getPin4().equals(command.getCurrentPin4())) {
            return OutputPort.badRequest("Current PIN is incorrect");
        }

        if (command.getNewPin4() == null || command.getNewPin4().length() != 4) {
            return OutputPort.badRequest("New PIN must be 4 digits");
        }

        card.setPin4(command.getNewPin4());
        card.setUpdatedAt(LocalDateTime.now());
        card = cardRepository.save(card);

        return OutputPort.ok(
                CardDtoMapper.INSTANCE.toResponse(card),
                "PIN changed successfully"
        );
    }
}
