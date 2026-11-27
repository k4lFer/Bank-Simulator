package com.pck4x.ledger_service.application.port.output;

public interface ProcessedEventRepository {
    boolean isProcessed(String eventId, String consumer);
    void markProcessed(String eventId, String consumer);
}
