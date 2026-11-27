package com.pck4x.transfers_service.infrastructure.kafka;

import com.pck4x.transfers_service.application.port.output.EventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Component
public class TransferEventPublisher implements EventPublisher {

    private static final Logger log = LoggerFactory.getLogger(TransferEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public TransferEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(String topic, Object event) {
        // Fase 0.3: esperar el ACK antes de devolver. El outbox solo marca SENT
        // cuando esta llamada termina sin excepcion. Si falla, el poller deja el
        // evento en PENDING para reintentarlo en el siguiente tick.
        try {
            var result = kafkaTemplate.send(topic, event).get(10, TimeUnit.SECONDS);
            log.info("Event published to topic {}: partition={}, offset={}",
                    topic,
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Interrupted while publishing event to " + topic, e);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to publish event to " + topic, e);
        }
    }
}
