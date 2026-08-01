package com.itau.ingestor.persistence.repository;

import com.itau.ingestor.persistence.entity.Balance;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public interface BalanceRepository extends JpaRepository<Balance, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO balances (account_id, amount, currency, updated_at)
            VALUES (:accountId, :amount, :currency, :updatedAt)
            ON CONFLICT (account_id) DO UPDATE
               SET amount = EXCLUDED.amount,
                   currency = EXCLUDED.currency,
                   updated_at = EXCLUDED.updated_at
            WHERE balances.updated_at < EXCLUDED.updated_at
            """, nativeQuery = true)
    int upsertIfNewer(@Param("accountId") UUID accountId,
                      @Param("amount") BigDecimal amount,
                      @Param("currency") String currency,
                      @Param("updatedAt") Instant updatedAt);
}
