package com.pck4x.transfers_service.application.feature.internaltransfer;

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
public class InternalTransferService implements InternalTransferUseCase {

    private final TransferRepository transferRepository;
    private final TransferEventRepository transferEventRepository;
    private final ObjectMapper objectMapper;

    public InternalTransferService(TransferRepository transferRepository,
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
            return OutputPort.ok(toResponse(t), "Internal transfer (idempotent replay)");
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
        event.setTransferType("INTERNAL");
        event.setUserId(userId);

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

        return OutputPort.created(toResponse(transfer), "Internal transfer initiated");
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
