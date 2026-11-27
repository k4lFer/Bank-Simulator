package com.pck4x.transfers_service.infrastructure.kafka;

import com.pck4x.sharedcontracts.event.TransferRequestedEvent;
import org.apache.kafka.clients.producer.RecordMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Fase 0.3: el publicador solo termina cuando Kafka confirma el ACK. */
class TransferEventPublisherTest {

    private KafkaTemplate<String, Object> kafkaTemplate;
    private TransferEventPublisher publisher;
    private TransferRequestedEvent event;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        kafkaTemplate = mock(KafkaTemplate.class);
        publisher = new TransferEventPublisher(kafkaTemplate);
        event = new TransferRequestedEvent(UUID.randomUUID(), "111", "222",
                new BigDecimal("10.00"), "USD", "test");
    }

    @Test
    @SuppressWarnings("unchecked")
    void esperaAckYTerminaSinExcepcion() {
        RecordMetadata metadata = mock(RecordMetadata.class);
        when(metadata.partition()).thenReturn(0);
        when(metadata.offset()).thenReturn(10L);
        SendResult<String, Object> result = mock(SendResult.class);
        when(result.getRecordMetadata()).thenReturn(metadata);
        CompletableFuture<SendResult<String, Object>> future = CompletableFuture.completedFuture(result);
        when(kafkaTemplate.send(eq("bank.transfer.events"), same(event))).thenReturn(future);

        assertDoesNotThrow(() -> publisher.publish("bank.transfer.events", event));
        verify(kafkaTemplate).send("bank.transfer.events", event);
    }

    @Test
    @SuppressWarnings("unchecked")
    void ackFallidoLanzaExcepcionParaQueElOutboxNoMarqueSENT() {
        CompletableFuture<SendResult<String, Object>> future = new CompletableFuture<>();
        future.completeExceptionally(new TimeoutException("ACK timeout"));
        when(kafkaTemplate.send(eq("bank.transfer.events"), same(event))).thenReturn(future);

        var thrown = assertThrows(IllegalStateException.class,
                () -> publisher.publish("bank.transfer.events", event));

        assertTrue(thrown.getMessage().contains("Failed to publish"));
        assertInstanceOf(ExecutionException.class, thrown.getCause());
    }
}
