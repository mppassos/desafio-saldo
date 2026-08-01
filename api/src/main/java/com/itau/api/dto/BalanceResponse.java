package com.itau.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record BalanceResponse(
        UUID id,
        UUID owner,
        BalanceInfo balance,
        String updatedAt) {

    public record BalanceInfo(BigDecimal amount, String currency) {
    }
}
