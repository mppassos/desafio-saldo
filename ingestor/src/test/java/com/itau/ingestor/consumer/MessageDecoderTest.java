package com.itau.ingestor.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.itau.ingestor.exception.MalformedMessageException;
import com.itau.ingestor.message.TransactionMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@Tag("unit")
class MessageDecoderTest {

    private ObjectMapper objectMapper;
    private MessageDecoder decoder;

    @BeforeEach
    void setUp() {

        objectMapper = new ObjectMapper()
                .setPropertyNamingStrategy(
                        com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE);
        decoder = new MessageDecoder(objectMapper);
    }

    @Test
    void shouldDecodeValidPayloadFromChallenge() throws Exception {
        String json = """
                {
                  "transaction": {
                    "id": "8e8ae808-b154-48b5-9f3e-553935cc4543",
                    "type": "CREDIT",
                    "amount": 97.07,
                    "currency": "BRL",
                    "status": "APPROVED",
                    "timestamp": 1751641364589998
                  },
                  "account": {
                    "id": "5b19c8b6-0cc4-4c72-a989-0c2ee15fa975",
                    "owner": "315e3cfe-f4af-4cd2-b298-a449e614349a",
                    "created_at": "1634874339",
                    "status": "ENABLED",
                    "balance": {
                      "amount": 183.12,
                      "currency": "BRL"
                    }
                  }
                }
                """;

        TransactionMessage message = decoder.decode(json);

        assertThat(message.transaction().id())
                .isEqualTo(UUID.fromString("8e8ae808-b154-48b5-9f3e-553935cc4543"));
        assertThat(message.transaction().amount()).isEqualByComparingTo(new BigDecimal("97.07"));
        assertThat(message.transaction().timestamp()).isEqualTo(1751641364589998L);
        assertThat(message.account().createdAt()).isEqualTo("1634874339");
        assertThat(message.account().balance().amount()).isEqualByComparingTo(new BigDecimal("183.12"));
        assertThat(message.isApproved()).isTrue();
    }

    @Test
    void shouldIgnoreUnknownFields() throws Exception {
        String json = """
                {"transaction": {"id": "%s", "type": "DEBIT", "amount": 10,
                  "currency": "BRL", "status": "REJECTED", "timestamp": 1,
                  "campo_extra": "x"},
                 "account": {"id": "%s", "owner": "%s", "created_at": "1634874339",
                  "status": "ENABLED", "balance": {"amount": 10, "currency": "BRL"},
                  "outro_extra": true}}
                """.formatted(
                        UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());

        TransactionMessage message = decoder.decode(json);

        assertThat(message.isApproved()).isFalse();
        assertThat(message.transaction().status()).isEqualTo("REJECTED");
    }

    @Test
    void shouldRejectMalformedJson() {
        assertThatThrownBy(() -> decoder.decode("{\"transaction\": "))
                .isInstanceOf(MalformedMessageException.class);
    }

    @Test
    void shouldRejectNullBody() {
        assertThatThrownBy(() -> decoder.decode(null))
                .isInstanceOf(MalformedMessageException.class);
    }
}
