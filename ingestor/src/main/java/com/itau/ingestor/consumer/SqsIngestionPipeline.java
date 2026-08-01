package com.itau.ingestor.consumer;

import com.itau.ingestor.exception.MalformedMessageException;
import com.itau.ingestor.message.TransactionMessage;
import com.itau.ingestor.service.IngestionService;
import com.itau.ingestor.service.ProcessingResult;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageBatchRequestEntry;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class SqsIngestionPipeline {

    private final SqsClient sqsClient;
    private final SqsQueue sqsQueue;
    private final MessageDecoder messageDecoder;
    private final IngestionService ingestionService;

    @Value("${ingestor.batch-size:10}")
    private int batchSize;

    @Value("${ingestor.wait-time-seconds:5}")
    private int waitTimeSeconds;

    @Value("${ingestor.visibility-timeout:30}")
    private int visibilityTimeout;

    @Retry(name = "sqsConsumer")
    @CircuitBreaker(name = "sqsConsumer")
    public void consume() {
        List<Message> messages = receive();

        if (messages.isEmpty()) {
            return;
        }

        List<String> confirmedHandles = processMessages(messages);

        if (!confirmedHandles.isEmpty()) {
            delete(confirmedHandles);
        }

        log.info("Ciclo concluído: recebidas={}, confirmadas={}",
                messages.size(), confirmedHandles.size());
    }

    private List<Message> receive() {
        ReceiveMessageResponse response = sqsClient.receiveMessage(ReceiveMessageRequest.builder()
                .queueUrl(sqsQueue.url())
                .maxNumberOfMessages(batchSize)
                .waitTimeSeconds(waitTimeSeconds)
                .visibilityTimeout(visibilityTimeout)
                .build());
        return response.messages();
    }

    private List<String> processMessages(List<Message> messages) {
        List<String> confirmedHandles = new ArrayList<>(messages.size());

        for (Message message : messages) {
            try {
                TransactionMessage payload = messageDecoder.decode(message.body());
                ProcessingResult result = ingestionService.process(payload, message.messageId());
                log.info("msg={} tx={} resultado={}", message.messageId(), payload.transaction().id(), result);
                confirmedHandles.add(message.receiptHandle());
            } catch (MalformedMessageException e) {

                log.warn("msg={} payload malformado — não confirmada (DLQ após maxReceiveCount): {}",
                        message.messageId(), e.getMessage());
            }
        }
        return confirmedHandles;
    }

    private void delete(List<String> receiptHandles) {
        List<DeleteMessageBatchRequestEntry> entries = receiptHandles.stream()
                .map(handle -> DeleteMessageBatchRequestEntry.builder()
                        .id(UUID.randomUUID().toString())
                        .receiptHandle(handle)
                        .build())
                .toList();

        sqsClient.deleteMessageBatch(DeleteMessageBatchRequest.builder()
                .queueUrl(sqsQueue.url())
                .entries(entries)
                .build());
    }
}
