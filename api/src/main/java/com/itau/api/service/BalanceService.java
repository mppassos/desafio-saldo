package com.itau.api.service;

import com.itau.api.dto.BalanceResponse;
import com.itau.api.exception.AccountNotFoundException;
import com.itau.api.model.Account;
import com.itau.api.model.Balance;
import com.itau.api.repository.AccountRepository;
import com.itau.api.repository.BalanceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class BalanceService {

    private static final DateTimeFormatter ISO_OFFSET =
            DateTimeFormatter.ISO_OFFSET_DATE_TIME;

    private final BalanceRepository balanceRepository;
    private final AccountRepository accountRepository;
    private final ZoneId appZoneId;

    @Transactional(readOnly = true)
    public BalanceResponse getBalance(UUID accountId) {
        Balance balance = balanceRepository.findById(accountId)
                .orElseThrow(() -> notFound(accountId));

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> notFound(accountId));

        String updatedAt = balance.getUpdatedAt()
                .atZone(appZoneId)
                .format(ISO_OFFSET);

        return new BalanceResponse(
                accountId,
                account.getOwner(),
                new BalanceResponse.BalanceInfo(balance.getAmount(), balance.getCurrency()),
                updatedAt);
    }

    private AccountNotFoundException notFound(UUID accountId) {
        return new AccountNotFoundException("Conta não encontrada: " + accountId);
    }
}
