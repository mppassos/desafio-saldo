package com.itau.api.controller;

import com.itau.api.dto.BalanceResponse;
import com.itau.api.service.BalanceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/balances")
@RequiredArgsConstructor
public class BalanceController {

    private final BalanceService balanceService;

    @GetMapping("/{accountId}")
    @Operation(summary = "Consulta o saldo mais atual de uma conta",
            description = "Retorna identificador da conta, titular, saldo (valor e moeda ISO 4217) " +
                    "e a data/hora da última atualização em ISO 8601 com offset.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Saldo encontrado",
                    content = @Content(schema = @Schema(implementation = BalanceResponse.class))),
            @ApiResponse(responseCode = "400", description = "accountId não é um UUID válido"),
            @ApiResponse(responseCode = "404", description = "Conta não encontrada")
    })
    public ResponseEntity<BalanceResponse> getBalance(
            @Parameter(description = "Identificador da conta (UUID)", required = true)
            @PathVariable UUID accountId) {

        return ResponseEntity.ok(balanceService.getBalance(accountId));
    }
}
