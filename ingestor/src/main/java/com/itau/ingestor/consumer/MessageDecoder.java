package com.itau.ingestor.consumer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.itau.ingestor.exception.MalformedMessageException;
import com.itau.ingestor.message.TransactionMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class MessageDecoder {

    private final ObjectMapper objectMapper;

    public TransactionMessage decode(String body) {
        if (body == null || body.isBlank()) {
            throw new MalformedMessageException("Body da mensagem vazio");
        }
        try {
            return objectMapper.readValue(body, TransactionMessage.class);
        } catch (JsonProcessingException e) {
            throw new MalformedMessageException(
                    "Falha ao desserializar payload: " + e.getOriginalMessage(), e);
        }
    }
}
