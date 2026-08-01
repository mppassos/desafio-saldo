package com.itau.ingestor.service;

import com.itau.ingestor.exception.MalformedMessageException;
import com.itau.ingestor.message.TransactionMessage;
import com.itau.ingestor.persistence.repository.AccountRepository;
import com.itau.ingestor.persistence.repository.BalanceRepository;
import com.itau.ingestor.persistence.repository.ProcessedTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;

@Slf4j
@Service
@RequiredArgsConstructor
public class IngestionService {

    private final AccountRepository accountRepository;
    private final BalanceRepository balanceRepository;
    private final ProcessedTransactionRepository processedTransactionRepository;
    private final IngestionMetrics metrics;

    @Transactional
    public ProcessingResult process(TransactionMessage message, String messageId) {
        validate(message);
        return doProcess(message, messageId);
    }

    private ProcessingResult doProcess(TransactionMessage message, String messageId) {
        TransactionMessage.Transaction tx = message.transaction();
        TransactionMessage.Account acc = message.account();

        if (processedTransactionRepository.insertIfAbsent(tx.id(), Instant.now()) == 0) {
            metrics.duplicate();
            log.debug("Duplicata ignorada: tx={} msg={}", tx.id(), messageId);
            return ProcessingResult.DUPLICATE;
        }

        if (!message.isApproved()) {
            metrics.notApproved();
            log.info("Transação {} status={} não altera saldo (msg={})",
                    tx.id(), tx.status(), messageId);
            return ProcessingResult.NOT_APPROVED;
        }

        Instant accountCreatedAt = TimestampConverter.fromSeconds(acc.createdAt());
        accountRepository.insertIfAbsent(acc.id(), acc.owner(), accountCreatedAt, acc.status());

        TransactionMessage.Account.Balance balanceInfo = acc.balance();
        Instant transactionTimestamp = TimestampConverter.fromMicros(tx.timestamp());

        int rows = balanceRepository.upsertIfNewer(
                acc.id(), balanceInfo.amount(), balanceInfo.currency(), transactionTimestamp);

        if (rows == 0) {
            metrics.stale();
            log.info("Mensagem antiga ignorada: tx={} (saldo já mais recente)", tx.id());
            return ProcessingResult.STALE;
        }

        metrics.applied();
        log.info("Saldo da conta {} atualizado para {} {} (tx={})",
                acc.id(), balanceInfo.amount(), balanceInfo.currency(), tx.id());
        return ProcessingResult.APPLIED;
    }

    private void validate(TransactionMessage message) {
        TransactionMessage.Transaction tx = message.transaction();
        TransactionMessage.Account acc = message.account();
        TransactionMessage.Account.Balance balance =
                (acc == null) ? null : acc.balance();

        if (tx == null || acc == null || balance == null
                || tx.id() == null || acc.id() == null || acc.owner() == null
                || acc.createdAt() == null || acc.status() == null
                || balance.amount() == null || balance.currency() == null
                || tx.amount() == null || tx.currency() == null) {
            metrics.malformed();
            throw new MalformedMessageException(
                    "Payload incompleto: campos obrigatórios ausentes ou nulos");
        }
    }
}
