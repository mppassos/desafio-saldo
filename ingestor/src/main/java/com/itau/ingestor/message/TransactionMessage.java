package com.itau.ingestor.message;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.math.BigDecimal;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TransactionMessage(Transaction transaction, Account account) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Transaction(UUID id,
                              String type,
                              BigDecimal amount,
                              String currency,
                              String status,
                              long timestamp) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Account(UUID id,
                          UUID owner,
                          String createdAt,
                          String status,
                          Balance balance) {

        @JsonIgnoreProperties(ignoreUnknown = true)
        public record Balance(BigDecimal amount, String currency) {
        }
    }

    public boolean isApproved() {
        return transaction != null && "APPROVED".equalsIgnoreCase(transaction.status());
    }
}
