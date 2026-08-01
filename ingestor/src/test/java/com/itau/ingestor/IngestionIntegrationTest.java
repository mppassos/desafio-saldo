package com.itau.ingestor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itau.ingestor.consumer.SqsIngestionPipeline;
import com.itau.ingestor.message.TransactionMessage;
import com.itau.ingestor.persistence.entity.Balance;
import com.itau.ingestor.persistence.repository.BalanceRepository;
import com.itau.ingestor.persistence.repository.ProcessedTransactionRepository;
import com.itau.ingestor.service.IngestionService;
import com.itau.ingestor.service.ProcessingResult;
import com.itau.ingestor.service.TimestampConverter;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.containers.localstack.LocalStackContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.services.sqs.SqsClient;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Tag("integration")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@Testcontainers
@Transactional
class IngestionIntegrationTest {

    static final String QUEUE_NAME = "transacoes-financeiras-processadas";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15")
            .withDatabaseName("saldo_db")
            .withUsername("admin")
            .withPassword("admin123");

    @Container
    static final LocalStackContainer LOCALSTACK = new LocalStackContainer(
            DockerImageName.parse("localstack/localstack:3.7.2"))
            .withServices(LocalStackContainer.Service.SQS);

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("aws.sqs.endpoint", () -> LOCALSTACK.getEndpoint().toString());
        registry.add("aws.sqs.region", () -> "sa-east-1");
        registry.add("aws.sqs.access-key", () -> "test");
        registry.add("aws.sqs.secret-key", () -> "test");
        registry.add("ingestor.scheduling.enabled", () -> "false");
    }

    @Autowired
    private IngestionService ingestionService;

    @Autowired
    private SqsIngestionPipeline pipeline;

    @Autowired
    private BalanceRepository balanceRepository;

    @Autowired
    private ProcessedTransactionRepository processedTransactionRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private SqsClient sqsClient;

    @Test
    void shouldApplyConsolidatedBalanceFromApprovedMessage() {
        TransactionMessage message = buildMessage("CREDIT", "APPROVED",
                new BigDecimal("97.07"), new BigDecimal("183.12"),
                1_751_641_364_589_998L);

        ProcessingResult result = ingestionService.process(message, "msg-1");

        assertThat(result).isEqualTo(ProcessingResult.APPLIED);

        Balance balance = balanceRepository.findById(message.account().id()).orElseThrow();
        assertThat(balance.getAmount()).isEqualByComparingTo(new BigDecimal("183.12"));
        assertThat(balance.getCurrency()).isEqualTo("BRL");
        assertThat(balance.getUpdatedAt())
                .isEqualTo(TimestampConverter.fromMicros(1_751_641_364_589_998L));
    }

    @Test
    void shouldIgnoreDuplicateMessage() {
        TransactionMessage message = buildMessage("CREDIT", "APPROVED",
                new BigDecimal("10.00"), new BigDecimal("20.00"), 2_000_000_000_000_000L);

        assertThat(ingestionService.process(message, "msg-1")).isEqualTo(ProcessingResult.APPLIED);

        assertThat(ingestionService.process(message, "msg-1")).isEqualTo(ProcessingResult.DUPLICATE);
        assertThat(balanceRepository.findById(message.account().id()).orElseThrow().getAmount())
                .isEqualByComparingTo(new BigDecimal("20.00"));
    }

    @Test
    void shouldNotRegressBalanceWithOutOfOrderMessage() {
        UUID accountId = UUID.randomUUID();

        TransactionMessage newer = buildMessage(accountId, "CREDIT", "APPROVED",
                new BigDecimal("10.00"), new BigDecimal("50.00"), 2_000_000_000_000_000L);
        assertThat(ingestionService.process(newer, "msg-nova")).isEqualTo(ProcessingResult.APPLIED);

        TransactionMessage older = buildMessage(accountId, "DEBIT", "APPROVED",
                new BigDecimal("5.00"), new BigDecimal("45.00"), 1_000_000_000_000_000L);
        assertThat(ingestionService.process(older, "msg-antiga")).isEqualTo(ProcessingResult.STALE);

        Balance balance = balanceRepository.findById(accountId).orElseThrow();
        assertThat(balance.getAmount()).isEqualByComparingTo(new BigDecimal("50.00"));
        assertThat(balance.getUpdatedAt())
                .isEqualTo(TimestampConverter.fromMicros(2_000_000_000_000_000L));
    }

    @Test
    void shouldNotCreateBalanceForRejectedTransaction() {
        TransactionMessage message = buildMessage("DEBIT", "REJECTED",
                new BigDecimal("100.00"), new BigDecimal("0.00"), 2_000_000_000_000_000L);

        assertThat(ingestionService.process(message, "msg-rej"))
                .isEqualTo(ProcessingResult.NOT_APPROVED);

        assertThat(processedTransactionRepository.existsById(message.transaction().id())).isTrue();
        assertThat(balanceRepository.findById(message.account().id())).isEmpty();
    }

    @Test
    void shouldConsumeFromSqsAndUpdateBalance() throws Exception {
        String queueUrl = sqsClient.createQueue(r -> r.queueName(QUEUE_NAME)).queueUrl();

        TransactionMessage message = buildMessage("CREDIT", "APPROVED",
                new BigDecimal("97.07"), new BigDecimal("183.12"), 1_751_641_364_589_998L);

        String json = objectMapper.writeValueAsString(message);
        sqsClient.sendMessage(r -> r.queueUrl(queueUrl)
                .messageBody(json));

        pipeline.consume();

        Optional<Balance> balance = balanceRepository.findById(message.account().id());
        assertThat(balance).isPresent();
        assertThat(balance.get().getAmount()).isEqualByComparingTo(new BigDecimal("183.12"));

        long remaining = sqsClient.getQueueAttributes(r -> r
                        .queueUrl(queueUrl)
                        .attributeNames(software.amazon.awssdk.services.sqs.model.QueueAttributeName.APPROXIMATE_NUMBER_OF_MESSAGES))
                .attributes().values().stream().mapToLong(Long::parseLong).sum();
        assertThat(remaining).isZero();
    }

    private TransactionMessage buildMessage(String type, String status,
                                            BigDecimal txAmount, BigDecimal balanceAmount,
                                            long timestampMicros) {
        return buildMessage(UUID.randomUUID(), type, status,
                txAmount, balanceAmount, timestampMicros);
    }

    private TransactionMessage buildMessage(UUID accountId, String type, String status,
                                            BigDecimal txAmount, BigDecimal balanceAmount,
                                            long timestampMicros) {
        UUID transactionId = UUID.randomUUID();
        UUID ownerId = UUID.randomUUID();

        TransactionMessage.Transaction transaction = new TransactionMessage.Transaction(
                transactionId, type, txAmount, "BRL", status, timestampMicros);

        TransactionMessage.Account.Balance balance = new TransactionMessage.Account.Balance(
                balanceAmount, "BRL");

        TransactionMessage.Account account = new TransactionMessage.Account(
                accountId, ownerId, String.valueOf(Instant.now().getEpochSecond()), "ENABLED", balance);

        return new TransactionMessage(transaction, account);
    }
}
