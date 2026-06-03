package com.pck4x.users_service.application.port.output;

public interface EventPublisher {
    void publish(String topic, Object event);
}
