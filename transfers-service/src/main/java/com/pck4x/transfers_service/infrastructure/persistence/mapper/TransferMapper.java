package com.pck4x.transfers_service.infrastructure.persistence.mapper;

import com.pck4x.transfers_service.domain.Transfer;
import com.pck4x.transfers_service.infrastructure.persistence.jpa.entities.TransferEntity;

import java.time.LocalDateTime;

public class TransferMapper {

    public static final TransferMapper INSTANCE = new TransferMapper();

    private TransferMapper() {}

    public TransferEntity toEntity(Transfer transfer) {
        TransferEntity entity = new TransferEntity();
        entity.setId(transfer.getId());
        entity.setTransferId(transfer.getTransferId());
        entity.setUserId(transfer.getUserId());
        entity.setToUserId(transfer.getToUserId());
        entity.setFromAccount(transfer.getFromAccount());
        entity.setToAccount(transfer.getToAccount());
        entity.setAmount(transfer.getAmount());
        entity.setCurrency(transfer.getCurrency());
        entity.setDescription(transfer.getDescription());
        entity.setStatus(transfer.getStatus());
        entity.setRejectionReason(transfer.getRejectionReason());
        entity.setCreatedAt(transfer.getCreatedAt() != null ? transfer.getCreatedAt() : LocalDateTime.now());
        entity.setUpdatedAt(LocalDateTime.now());
        return entity;
    }

    public Transfer toDomain(TransferEntity entity) {
        Transfer transfer = new Transfer();
        transfer.setId(entity.getId());
        transfer.setTransferId(entity.getTransferId());
        transfer.setUserId(entity.getUserId());
        transfer.setToUserId(entity.getToUserId());
        transfer.setFromAccount(entity.getFromAccount());
        transfer.setToAccount(entity.getToAccount());
        transfer.setAmount(entity.getAmount());
        transfer.setCurrency(entity.getCurrency());
        transfer.setDescription(entity.getDescription());
        transfer.setStatus(entity.getStatus());
        transfer.setRejectionReason(entity.getRejectionReason());
        transfer.setCreatedAt(entity.getCreatedAt());
        transfer.setUpdatedAt(entity.getUpdatedAt());
        return transfer;
    }
}
