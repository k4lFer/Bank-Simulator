package com.pck4x.transfers_service.application.feature.externaltransfer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.pck4x.sharedcontracts.event.TransferRequestedEvent;
import com.pck4x.sharedcontracts.result.OutputPort;
import com.pck4x.transfers_service.application.dto.command.TransferCommand;
import com.pck4x.transfers_service.application.dto.response.TransferResponse;
import com.pck4x.transfers_service.application.port.output.TransferEventRepository;
import com.pck4x.transfers_service.application.port.output.TransferRepository;
import com.pck4x.transfers_service.domain.Transfer;
import com.pck4x.transfers_service.domain.TransferEvent;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class ExternalTransferService implements ExternalTransferUseCase {

    private final TransferRepository transferRepository;
    private final TransferEventRepository transferEventRepository;
    private final ObjectMapper objectMapper;

    public ExternalTransferService(TransferRepository transferRepository,
                                   TransferEventRepository transferEventRepository,
                                   ObjectMapper objectMapper) {
        this.transferRepository = transferRepository;
        this.transferEventRepository = transferEventRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public OutputPort<TransferResponse> execute(TransferCommand input, UUID idempotencyKey, UUID userId) {
        var existing = transferRepository.findByTransferId(idempotencyKey);
        if (existing.isPresent()) {
            var t = existing.get();
            return OutputPort.ok(toResponse(t), "Transfer (idempotent replay)");
        }

        boolean isCardPayment = input.getCardId() != null;

        if (isCardPayment && (input.getPin4() == null || input.getPin4().isBlank())) {
            return OutputPort.badRequest("PIN is required for card payments");
        }

        var transferId = idempotencyKey;

        var transfer = new Transfer(
                transferId,
                userId,
                input.getFromAccount(),
                input.getToAccount(),
                input.getAmount(),
                input.getCurrency(),
                input.getDescription()
        );
        transfer = transferRepository.save(transfer);

        var event = new TransferRequestedEvent(
                transferId,
                input.getFromAccount(),
                input.getToAccount(),
                input.getAmount(),
                input.getCurrency(),
                input.getDescription()
        );
        event.setTransferType(isCardPayment ? "CARD" : "EXTERNAL");
        event.setUserId(userId);
        event.setPin4(input.getPin4());
        if (isCardPayment) {
            event.setCardId(input.getCardId());
        }

        try {
            var payload = objectMapper.writeValueAsString(event);
            var outboxEvent = new TransferEvent(transferId, "TransferRequested", payload);
            transferEventRepository.save(outboxEvent);
        } catch (JsonProcessingException e) {
            return OutputPort.failure(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to serialize transfer event"
            );
        }

        return OutputPort.created(toResponse(transfer), isCardPayment ? "Card payment initiated" : "External transfer initiated");
    }

    private TransferResponse toResponse(Transfer t) {
        return new TransferResponse(
                t.getTransferId(),
                t.getUserId(),
                t.getToUserId(),
                t.getFromAccount(),
                t.getToAccount(),
                t.getAmount(),
                t.getCurrency(),
                t.getDescription(),
                t.getStatus(),
                t.getRejectionReason(),
                t.getCreatedAt()
        );
    }
}
