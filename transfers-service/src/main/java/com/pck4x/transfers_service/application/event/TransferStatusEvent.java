package com.pck4x.transfers_service.application.event;

import com.pck4x.transfers_service.domain.Transfer;

import java.util.UUID;

public record TransferStatusEvent(Transfer transfer) {
    public UUID transferId() { return transfer.getTransferId(); }
}
