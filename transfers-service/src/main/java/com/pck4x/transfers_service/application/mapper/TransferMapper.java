package com.pck4x.transfers_service.application.mapper;

import com.pck4x.transfers_service.application.dto.response.TransferResponse;
import com.pck4x.transfers_service.domain.Transfer;

public class TransferMapper {

    public static final TransferMapper INSTANCE = new TransferMapper();

    private TransferMapper() {}

    public TransferResponse toResponse(Transfer transfer) {
        return new TransferResponse(
                transfer.getTransferId(),
                transfer.getUserId(),
                transfer.getToUserId(),
                transfer.getFromAccount(),
                transfer.getToAccount(),
                transfer.getAmount(),
                transfer.getCurrency(),
                transfer.getDescription(),
                transfer.getStatus(),
                transfer.getRejectionReason(),
                transfer.getCreatedAt()
        );
    }
}
