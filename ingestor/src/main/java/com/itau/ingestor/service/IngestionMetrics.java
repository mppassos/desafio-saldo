package com.itau.ingestor.service;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class IngestionMetrics {

    private final Counter applied;
    private final Counter duplicate;
    private final Counter stale;
    private final Counter notApproved;
    private final Counter malformed;

    public IngestionMetrics(MeterRegistry registry) {
        this.applied = Counter.builder("ingestor.messages.applied")
                .description("Mensagens cujo saldo foi atualizado")
                .register(registry);
        this.duplicate = Counter.builder("ingestor.messages.duplicate")
                .description("Duplicatas ignoradas (idempotência)")
                .register(registry);
        this.stale = Counter.builder("ingestor.messages.stale")
                .description("Mensagens mais antigas que o saldo atual")
                .register(registry);
        this.notApproved = Counter.builder("ingestor.messages.not_approved")
                .description("Transações rejeitadas (não alteram saldo)")
                .register(registry);
        this.malformed = Counter.builder("ingestor.messages.malformed")
                .description("Payloads inválidos roteados para a DLQ")
                .register(registry);
    }

    public void applied()       { applied.increment(); }
    public void duplicate()     { duplicate.increment(); }
    public void stale()         { stale.increment(); }
    public void notApproved()   { notApproved.increment(); }
    public void malformed()     { malformed.increment(); }
}
