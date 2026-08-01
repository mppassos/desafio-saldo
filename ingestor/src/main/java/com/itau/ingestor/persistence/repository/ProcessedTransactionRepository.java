package com.itau.ingestor.persistence.repository;

import com.itau.ingestor.persistence.entity.ProcessedTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface ProcessedTransactionRepository extends JpaRepository<ProcessedTransaction, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO processed_transactions (transaction_id, processed_at)
            VALUES (:transactionId, :processedAt)
            ON CONFLICT (transaction_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("transactionId") UUID transactionId,
                       @Param("processedAt") Instant processedAt);
}
