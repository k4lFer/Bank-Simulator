package com.pck4x.ledger_service.infrastructure.persistence.jpa.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "processed_events", uniqueConstraints = @UniqueConstraint(
        name = "uk_processed_event", columnNames = {"consumer", "event_id"}))
@Getter
@Setter
public class ProcessedEventEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "event_id", nullable = false, length = 64)
    private String eventId;

    @Column(name = "consumer", nullable = false, length = 64)
    private String consumer;

    @Column(name = "processed_at", nullable = false)
    private LocalDateTime processedAt;
}
