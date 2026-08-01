package com.itau.api;

import com.itau.api.model.Account;
import com.itau.api.model.Balance;
import com.itau.api.repository.AccountRepository;
import com.itau.api.repository.BalanceRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class ApiIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("saldo_db")
            .withUsername("admin")
            .withPassword("admin123");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Test
    void shouldReturnBalanceForExistingAccount() {
        UUID accountId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        saveAccount(accountId, ownerId);
        saveBalance(accountId, new BigDecimal("123.45"), Instant.now());

        ResponseEntity<String> response = restTemplate.getForEntity(
                url(accountId), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .contains("\"id\":\"" + accountId + "\"")
                .contains("\"owner\":\"" + ownerId + "\"")
                .contains("\"amount\":123.45")
                .contains("\"currency\":\"BRL\"")
                .contains("\"updated_at\"");
    }

    @Test
    void shouldReturn404WhenAccountNotFound() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                url(UUID.randomUUID()), String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).contains("\"status\":404");
    }

    @Test
    void shouldReturn400WhenAccountIdIsNotAUuid() {
        ResponseEntity<String> response = restTemplate.getForEntity(
                "http://localhost:" + port + "/api/v1/balances/nao-e-um-uuid", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("\"status\":400");
    }

    private String url(UUID accountId) {
        return "http://localhost:" + port + "/api/v1/balances/" + accountId;
    }

    private void saveAccount(UUID accountId, UUID ownerId) {
        accountRepository.save(new Account(
                accountId, ownerId, Instant.now(), "ENABLED"));
    }

    private void saveBalance(UUID accountId, BigDecimal amount, Instant updatedAt) {
        balanceRepository.save(new Balance(accountId, amount, "BRL", updatedAt));
    }
}
