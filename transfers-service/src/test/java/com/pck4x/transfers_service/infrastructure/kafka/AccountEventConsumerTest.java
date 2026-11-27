package com.pck4x.transfers_service.infrastructure.kafka;

import com.pck4x.sharedcontracts.event.AccountRejectedEvent;
import com.pck4x.transfers_service.application.feature.saga.TransferSagaOrchestrator;
import com.pck4x.transfers_service.application.port.output.EventPublisher;
import com.pck4x.transfers_service.application.port.output.ProcessedEventRepository;
import com.pck4x.transfers_service.application.port.output.TransferRepository;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import java.util.UUID;

import static org.mockito.Mockito.*;

/** Fase 2.7: el listener delega en el orquestador; ya no emite notificaciones. */
class AccountEventConsumerTest {

    @Test
    void conSagaActivadaDelegaEnElOrquestador() {
        var transferRepository = mock(TransferRepository.class);
        var statusPublisher = mock(ApplicationEventPublisher.class);
        var kafkaPublisher = mock(EventPublisher.class);
        var processedEvents = mock(ProcessedEventRepository.class);
        var orchestrator = mock(TransferSagaOrchestrator.class);
        var consumer = new AccountEventConsumer(transferRepository, statusPublisher,
                kafkaPublisher, processedEvents, orchestrator, true);

        var event = new AccountRejectedEvent(UUID.randomUUID(), "111", "Insufficient funds");
        consumer.handleAccountEvent(new ConsumerRecord<>("bank.account.events", 0, 0L, "key", event));

        verify(orchestrator).handle(event);
        verifyNoInteractions(kafkaPublisher, transferRepository, processedEvents);
    }
}
