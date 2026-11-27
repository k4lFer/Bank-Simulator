package com.pck4x.transfers_service.infrastructure.kafka;

import com.pck4x.sharedcontracts.command.TransferCompensateCommand;
import com.pck4x.sharedcontracts.command.TransferCreditCommand;
import com.pck4x.sharedcontracts.command.TransferDebitCommand;
import com.pck4x.sharedcontracts.event.TransferRequestedEvent;

/**
 * Fase 2.3: el outbox distingue comandos de eventos y elige el topic destino.
 * Conserva "TransferRequested" para poder drenar filas del flujo anterior.
 */
public enum TransferOutboxType {
    TRANSFER_REQUESTED(TransferRequestedEvent.class, "bank.transfer.events", "TransferRequested"),
    TRANSFER_DEBIT_COMMAND(TransferDebitCommand.class, "bank.transfer.commands"),
    TRANSFER_CREDIT_COMMAND(TransferCreditCommand.class, "bank.transfer.commands"),
    TRANSFER_COMPENSATE_COMMAND(TransferCompensateCommand.class, "bank.transfer.commands");

    private final Class<?> messageClass;
    private final String topic;
    private final String legacyName;

    TransferOutboxType(Class<?> messageClass, String topic) {
        this(messageClass, topic, null);
    }

    TransferOutboxType(Class<?> messageClass, String topic, String legacyName) {
        this.messageClass = messageClass;
        this.topic = topic;
        this.legacyName = legacyName;
    }

    public Class<?> messageClass() {
        return messageClass;
    }

    public String topic() {
        return topic;
    }

    public static String nameOf(Object message) {
        if (message == null) {
            throw new IllegalArgumentException("Message must not be null");
        }
        for (TransferOutboxType type : values()) {
            if (type.messageClass.isInstance(message)) {
                return type.legacyName != null ? type.legacyName : type.messageClass.getSimpleName();
            }
        }
        throw new IllegalArgumentException("Unknown transfer outbox message: " + message.getClass().getName());
    }

    public static TransferOutboxType resolve(String typeName) {
        if (typeName == null) {
            throw new IllegalArgumentException("Message type must not be null");
        }
        for (TransferOutboxType type : values()) {
            if (type.name().equals(typeName)
                    || type.messageClass.getSimpleName().equals(typeName)
                    || type.messageClass.getName().equals(typeName)
                    || (type.legacyName != null && type.legacyName.equals(typeName))) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown transfer outbox message type: " + typeName);
    }
}
