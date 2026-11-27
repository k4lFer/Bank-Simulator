package com.pck4x.sharedcontracts.event;

import java.util.UUID;

/**
 * Fase 1.2: contrato comun de los eventos de dominio.
 * Permite que cualquier outbox registre el eventId sin conocer el tipo concreto.
 */
public interface DomainEvent {
    UUID getEventId();
}
