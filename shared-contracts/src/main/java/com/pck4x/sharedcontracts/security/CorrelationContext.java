package com.pck4x.sharedcontracts.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.util.UUID;

/** Fase 1.5: utilidad para llevar el correlation ID de MDC a los eventos. */
public final class CorrelationContext {

    private static final Logger log = LoggerFactory.getLogger(CorrelationContext.class);

    private CorrelationContext() {}

    public static String get() {
        return org.slf4j.MDC.get(CorrelationIdFilter.MDC_KEY);
    }

    public static void stamp(Object event) {
        set(event, get());
    }

    public static void copy(Object source, Object target) {
        if (source == null) {
            return;
        }
        try {
            Field field = findField(source.getClass(), "correlationId");
            if (field == null) {
                return;
            }
            field.setAccessible(true);
            Object value = field.get(source);
            set(target, value == null ? null : value.toString());
        } catch (Exception e) {
            log.warn("No se pudo copiar correlationId desde {}: {}",
                    source.getClass().getSimpleName(), e.getMessage());
        }
    }

    private static void set(Object event, String correlationId) {
        if (event == null || correlationId == null || correlationId.isBlank()) {
            return;
        }
        try {
            Field field = findField(event.getClass(), "correlationId");
            if (field == null) {
                return;
            }
            field.setAccessible(true);
            if (field.get(event) != null) {
                return;
            }
            field.set(event, field.getType().equals(String.class)
                    ? correlationId
                    : UUID.fromString(correlationId));
        } catch (Exception e) {
            log.warn("No se pudo propagar correlationId al evento {}: {}",
                    event.getClass().getSimpleName(), e.getMessage());
        }
    }

    private static Field findField(Class<?> type, String name) {
        for (Class<?> current = type; current != null; current = current.getSuperclass()) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException ignored) {
                // seguir por la superclase
            }
        }
        return null;
    }
}
