package com.pck4x.transfers_service.application.feature.saga;

import com.pck4x.transfers_service.application.port.output.TransferRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/** Fase 2.9: delega en el orquestador el cierre o la compensacion de sagas abandonadas. */
@Component
@ConditionalOnProperty(prefix = "app.saga", name = "orchestrated", havingValue = "true", matchIfMissing = true)
public class TransferSagaTimeoutScheduler {

    private static final Logger log = LoggerFactory.getLogger(TransferSagaTimeoutScheduler.class);

    private final TransferRepository transferRepository;
    private final TransferSagaOrchestrator orchestrator;
    private final long timeoutSeconds;

    public TransferSagaTimeoutScheduler(TransferRepository transferRepository,
                                        TransferSagaOrchestrator orchestrator,
                                        @Value("${app.saga.timeout-seconds:60}") long timeoutSeconds) {
        this.transferRepository = transferRepository;
        this.orchestrator = orchestrator;
        this.timeoutSeconds = timeoutSeconds;
    }

    @Scheduled(fixedDelayString = "${app.saga.timeout-check-ms:60000}")
    public void timeoutStaleTransfers() {
        var cutoff = LocalDateTime.now().minusSeconds(timeoutSeconds);
        var stale = transferRepository.findStale(cutoff);
        if (!stale.isEmpty()) {
            log.warn("Found {} stale saga(s) older than {}s", stale.size(), timeoutSeconds);
        }
        for (var transfer : stale) {
            orchestrator.handleTimeout(transfer);
        }
    }
}
