package com.pck4x.users_service.infrastructure.kafka;

import com.pck4x.users_service.application.port.output.EventPublisher;
import com.pck4x.sharedcontracts.security.CorrelationContext;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Component
public class UserEventPublisher implements EventPublisher {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    public UserEventPublisher(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void publish(String topic, Object event) {
        CorrelationContext.stamp(event);
        kafkaTemplate.send(topic, event);
    }
}
