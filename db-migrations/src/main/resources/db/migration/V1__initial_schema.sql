-- ============================================================================
-- V1 - Schema inicial
-- Contas, saldos e idempotência (desafio técnico Itaú)
-- ============================================================================
--
-- Notas de design:
--  * TIMESTAMPTZ: timestamps são Instant no Java (UTC). Evita ambiguidade de fuso.
--  * NUMERIC(19,2): moeda em centavos (BRL). Nunca usar float/double para dinheiro.
--  * PK de balances = account_id (uma linha por conta -> last-write-wins).
--  * processed_transactions: a constraint PRIMARY KEY(transaction_id) é a barreira
--    de idempotência usada com INSERT ... ON CONFLICT DO NOTHING no ingestor.

CREATE TABLE IF NOT EXISTS accounts (
    id          UUID PRIMARY KEY,
    owner       UUID        NOT NULL,
    created_at  TIMESTAMPTZ NOT NULL,
    status      VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS balances (
    account_id  UUID PRIMARY KEY REFERENCES accounts (id),
    amount      NUMERIC(19, 2) NOT NULL,
    currency    VARCHAR(3)     NOT NULL DEFAULT 'BRL',
    updated_at  TIMESTAMPTZ    NOT NULL
);

CREATE TABLE IF NOT EXISTS processed_transactions (
    transaction_id UUID PRIMARY KEY,
    processed_at   TIMESTAMPTZ NOT NULL
);
