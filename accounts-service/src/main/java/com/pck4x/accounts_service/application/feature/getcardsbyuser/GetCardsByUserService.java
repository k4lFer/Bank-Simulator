package com.pck4x.accounts_service.application.feature.getcardsbyuser;

import com.pck4x.accounts_service.application.dto.response.CardResponse;
import com.pck4x.accounts_service.application.mapper.CardDtoMapper;
import com.pck4x.accounts_service.application.port.output.CardRepository;
import com.pck4x.sharedcontracts.result.OutputPort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class GetCardsByUserService implements GetCardsByUserUseCase {

    private final CardRepository cardRepository;

    public GetCardsByUserService(CardRepository cardRepository) {
        this.cardRepository = cardRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public OutputPort<List<CardResponse>> execute(UUID userId) {
        var cards = cardRepository.findByUserId(userId);
        return OutputPort.ok(CardDtoMapper.INSTANCE.toResponseList(cards));
    }
}
