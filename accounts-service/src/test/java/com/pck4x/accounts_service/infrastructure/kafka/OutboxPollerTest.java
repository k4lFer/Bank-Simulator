package com.pck4x.accounts_service.infrastructure.kafka;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pck4x.accounts_service.application.port.output.OutboxEventRepository;
import com.pck4x.accounts_service.domain.OutboxEvent;
import com.pck4x.accounts_service.domain.enums.OutboxStatus;
import com.pck4x.sharedcontracts.event.AccountDebitedEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Fase 0.3 + 1.3: ACK de Kafka, backoff y DLQ del outbox de accounts. */
class OutboxPollerTest {

    private OutboxEventRepository repository;
    private KafkaTemplate<String, Object> kafkaTemplate;
    private OutboxPoller poller;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        repository = mock(OutboxEventRepository.class);
        kafkaTemplate = mock(KafkaTemplate.class);
        poller = new OutboxPoller(repository, kafkaTemplate, objectMapper);
        when(repository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
    }

    private OutboxEvent event(int attempts) throws Exception {
        var payload = objectMapper.writeValueAsString(new AccountDebitedEvent(
                UUID.randomUUID(), UUID.randomUUID(), "111",
                new BigDecimal("25.00"), new BigDecimal("75.00"), "USD"));
        var event = new OutboxEvent("bank.account.events", "ACCOUNT_DEBITED", payload);
        event.setId(3L);
        event.setAttempts(attempts);
        event.setNextAttemptAt(LocalDateTime.now().minusMinutes(1));
        return event;
    }

    @SuppressWarnings("unchecked")
    private void ackOk() {
        SendResult<String, Object> result = mock(SendResult.class);
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(result);
        when(kafkaTemplate.send(anyString(), any())).thenReturn(future);
    }

    @Test
    void esperaAckAntesDeMarcarSENT() throws Exception {
        var event = event(0);
        ackOk();
        when(repository.findRetryable(any())).thenReturn(List.of(event));

        poller.processPendingEvents();

        assertEquals(OutboxStatus.SENT, event.getStatus());
        assertNotNull(event.getSentAt());
        verify(kafkaTemplate).send(eq("bank.account.events"), any());
    }

    @Test
    void falloDeAckIncrementeIntentosYProgrameBackoff() throws Exception {
        var event = event(2);
        when(kafkaTemplate.send(anyString(), any())).thenThrow(new IllegalStateException("broker caído"));
        when(repository.findRetryable(any())).thenReturn(List.of(event));

        var before = LocalDateTime.now();
        poller.processPendingEvents();

        assertEquals(3, event.getAttempts());
        assertEquals(OutboxStatus.PENDING, event.getStatus());
        assertFalse(event.getNextAttemptAt().isBefore(before.plusSeconds(7)),
                "el tercer reintento debe esperar ~8 s");
    }

    @Test
    void agotaIntentosYDerivaADLQ() throws Exception {
        var event = event(4);
        when(kafkaTemplate.send(anyString(), any())).thenThrow(new IllegalStateException("broker caído"));
        when(repository.findRetryable(any())).thenReturn(List.of(event));

        poller.processPendingEvents();

        assertEquals(5, event.getAttempts());
        assertEquals(OutboxStatus.FAILED, event.getStatus());
        assertTrue(event.getNextAttemptAt().isAfter(LocalDateTime.now().plusDays(300)));
    }
}
