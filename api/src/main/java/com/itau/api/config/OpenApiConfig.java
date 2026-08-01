package com.itau.api.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI saldoOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("API de Consulta de Saldo — Itaú Unibanco")
                        .description("""
                                Desafio técnico: consulta do saldo mais atual de uma conta bancária,
                                com base nas transações ingeridas da fila AWS SQS.
                                """)
                        .version("v1"));
    }
}
