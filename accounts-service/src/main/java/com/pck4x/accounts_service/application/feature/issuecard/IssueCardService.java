package com.pck4x.accounts_service.application.feature.issuecard;

import com.pck4x.accounts_service.application.dto.command.IssueCardCommand;
import com.pck4x.accounts_service.application.dto.response.CardResponse;
import com.pck4x.accounts_service.application.mapper.CardDtoMapper;
import com.pck4x.accounts_service.application.port.output.CardRepository;
import com.pck4x.accounts_service.domain.Card;
import com.pck4x.sharedcontracts.result.OutputPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.Random;
import java.util.UUID;

@Service
public class IssueCardService implements IssueCardUseCase {

    private final CardRepository cardRepository;

    public IssueCardService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    private String generatePan() {
        var rnd = new Random();
        var sb = new StringBuilder("400000");
        for (int i = 0; i < 10; i++) {
            sb.append(rnd.nextInt(10));
        }
        return sb.toString();
    }

    private YearMonth generateExpiry() {
        return YearMonth.now().plusYears(5);
    }

    @Override
    @Transactional
    public OutputPort<CardResponse> execute(IssueCardCommand command, UUID userId) {
        if (command.getPin4() == null || command.getPin4().length() != 4) {
            return OutputPort.badRequest("PIN must be 4 digits");
        }
        if (command.getPin6() == null || command.getPin6().length() != 6) {
            return OutputPort.badRequest("PIN must be 6 digits");
        }

        var card = new Card(
                UUID.randomUUID(),
                userId,
                generatePan(),
                generateExpiry(),
                command.getPin4(),
                command.getPin6()
        );

        card = cardRepository.save(card);

        return OutputPort.created(
                CardDtoMapper.INSTANCE.toResponse(card),
                "Card issued successfully"
        );
    }
}
