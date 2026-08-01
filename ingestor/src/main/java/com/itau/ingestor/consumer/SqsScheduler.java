package com.itau.ingestor.consumer;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class SqsScheduler {

    private final SqsIngestionPipeline pipeline;

    @Scheduled(
            fixedDelayString = "${ingestor.poll-interval-ms:1000}",
            initialDelayString = "${ingestor.initial-delay-ms:3000}")
    public void poll() {
        try {
            pipeline.consume();
        } catch (Exception e) {

            log.warn("Ciclo de ingestão falhou (CB/retry atuaram): {}", e.getMessage());
        }
    }
}
