package com.pck4x.sharedcontracts.command;

import java.util.UUID;

/**
 * Fase 2.3: contrato comun de los comandos de la saga.
 * Los comandos son ordenes: un solo productor (el orquestador) y un solo consumidor.
 */
public interface SagaCommand {
    /** Identificador unico del comando; el consumidor lo usa como clave de Inbox. */
    UUID getCommandId();

    UUID getCorrelationId();

    UUID getTransferId();
}
