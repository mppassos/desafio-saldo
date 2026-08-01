package com.itau.ingestor.persistence.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedTransaction {

    @Id
    @Column(name = "transaction_id")
    private UUID transactionId;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;
}
