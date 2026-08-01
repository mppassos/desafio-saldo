package com.itau.api.service;

import com.itau.api.dto.BalanceResponse;
import com.itau.api.exception.AccountNotFoundException;
import com.itau.api.model.Account;
import com.itau.api.model.Balance;
import com.itau.api.repository.AccountRepository;
import com.itau.api.repository.BalanceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@Tag("unit")
@ExtendWith(MockitoExtension.class)
class BalanceServiceTest {

    @Mock
    private BalanceRepository balanceRepository;

    @Mock
    private AccountRepository accountRepository;

    private BalanceService service;

    @BeforeEach
    void setUp() {
        service = new BalanceService(balanceRepository, accountRepository,
                ZoneId.of("America/Sao_Paulo"));
    }

    @Test
    void shouldReturnBalanceResponseWhenAccountExists() {
        UUID accountId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();
        Instant updatedAt = Instant.parse("2025-07-05T21:04:13.433Z");

        Balance balance = new Balance(accountId, new BigDecimal("183.12"), "BRL", updatedAt);
        Account account = new Account(accountId, ownerId, updatedAt, "ENABLED");

        when(balanceRepository.findById(accountId)).thenReturn(Optional.of(balance));
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));

        BalanceResponse response = service.getBalance(accountId);

        assertThat(response.id()).isEqualTo(accountId);
        assertThat(response.owner()).isEqualTo(ownerId);
        assertThat(response.balance().amount()).isEqualByComparingTo(new BigDecimal("183.12"));
        assertThat(response.balance().currency()).isEqualTo("BRL");

        assertThat(response.updatedAt()).isEqualTo("2025-07-05T18:04:13.433-03:00");
    }

    @Test
    void shouldThrowAccountNotFoundExceptionWhenBalanceMissing() {
        UUID accountId = UUID.randomUUID();

        when(balanceRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBalance(accountId))
                .isInstanceOf(AccountNotFoundException.class)
                .hasMessageContaining(accountId.toString());
    }

    @Test
    void shouldThrowAccountNotFoundExceptionWhenAccountMissing() {
        UUID accountId = UUID.randomUUID();

        when(balanceRepository.findById(accountId))
                .thenReturn(Optional.of(new Balance(accountId, BigDecimal.TEN, "BRL", Instant.now())));
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getBalance(accountId))
                .isInstanceOf(AccountNotFoundException.class);
    }
}
