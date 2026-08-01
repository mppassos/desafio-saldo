package com.itau.ingestor.persistence.repository;

import com.itau.ingestor.persistence.entity.Account;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.UUID;

public interface AccountRepository extends JpaRepository<Account, UUID> {

    @Modifying
    @Query(value = """
            INSERT INTO accounts (id, owner, created_at, status)
            VALUES (:id, :owner, :createdAt, :status)
            ON CONFLICT (id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(@Param("id") UUID id,
                       @Param("owner") UUID owner,
                       @Param("createdAt") Instant createdAt,
                       @Param("status") String status);
}
