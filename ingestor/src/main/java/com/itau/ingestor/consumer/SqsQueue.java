package com.itau.ingestor.consumer;

import lombok.extern.slf4j.Slf4j;
import software.amazon.awssdk.services.sqs.SqsClient;
import software.amazon.awssdk.services.sqs.model.QueueDoesNotExistException;

@Slf4j
public class SqsQueue {

    private final SqsClient sqsClient;
    private final String queueName;

    private volatile String url;

    public SqsQueue(SqsClient sqsClient, String queueName) {
        this.sqsClient = sqsClient;
        this.queueName = queueName;
    }

    public String url() {
        String current = url;
        if (current != null) {
            return current;
        }
        synchronized (this) {
            if (url == null) {
                url = resolve();
            }
            return url;
        }
    }

    private String resolve() {
        try {
            return sqsClient.getQueueUrl(r -> r.queueName(queueName)).queueUrl();
        } catch (QueueDoesNotExistException e) {
            log.info("Fila '{}' não existe — criando...", queueName);
            sqsClient.createQueue(r -> r.queueName(queueName));
            return sqsClient.getQueueUrl(r -> r.queueName(queueName)).queueUrl();
        }
    }
}
