# ADR 002 — Estratégia de Ingestão (SQS → PostgreSQL)

- **Status:** Aceito
- **Data:** Julho/2026

## Contexto

A fila **SQS standard** entrega mensagens com garantia **at-least-once**
(redelivery é normal) e **sem ordem garantida** (best-effort). Cada mensagem
traz uma transação e o **saldo consolidado** da conta pós-transação, com
`timestamp` em **microssegundos**. O sistema deve processar até
**2.000 mensagens/s** em picos sem perder nem duplicar atualizações de saldo.

## Decisões

### D1. Polling com long polling (em vez de listener/push)

O consumidor usa `@Scheduled(fixedDelay)` + `ReceiveMessage` com
`WaitTimeSeconds=5` (long polling). Justificativa:

- Controle explícito de **batch size**, **visibilidade** e **retry**;
- `fixedDelay` garante que ciclos nunca se sobreponham na mesma instância;
- Long polling evita chamadas vazias (custo ~zero quando a fila está vazia);
- Escala horizontal: réplicas do ingestor aumentam o throughput sem mudança
  de código (o SQS distribui mensagens entre consumidores).

### D2. Idempotência: `processed_transactions` + `ON CONFLICT DO NOTHING`

Cada mensagem é registrada na tabela `processed_transactions` (PK =
`transaction_id`) **dentro da mesma transação** da atualização de saldo:

```sql
INSERT INTO processed_transactions (transaction_id, processed_at)
VALUES (?, ?) ON CONFLICT (transaction_id) DO NOTHING;
```

- Se retorna `0`, a transação já foi processada → **DUPLICATE** (ignora);
- A constraint é a barreira atômica mesmo com dois workers processando a
  mesma mensagem em paralelo (sem janela de corrida de SELECT→INSERT).

### D3. Concorrência e fora de ordem: upsert atômico com last-write-wins

Em vez de lock otimista (`@Version`) + retry, o saldo é atualizado com uma
única instrução atômica:

```sql
INSERT INTO balances (account_id, amount, currency, updated_at)
VALUES (?, ?, ?, ?)
ON CONFLICT (account_id) DO UPDATE
   SET amount = EXCLUDED.amount, currency = EXCLUDED.currency,
       updated_at = EXCLUDED.updated_at
WHERE balances.updated_at < EXCLUDED.updated_at;
```

- **Por que não `@Version`:** exigiria `SELECT` + `UPDATE` + tratamento de
  `OptimisticLockException` + retry — mais estados e mais código. O upsert
  resolve criação, concorrência e fora-de-ordem em **uma** operação.
- Mensagens antigas (timestamp menor) retornam `0` linhas → **STALE**,
  registradas como processadas, mas sem regredir saldo.

### D4. O saldo persistido é o consolidado da mensagem

`account.balance.amount` é o estado pós-transação (fonte da verdade), e **não**
`transaction.amount` (movimento). O ingestor persiste o valor consolidado com o
`transaction.timestamp` (micros → `Instant`, conversão com `floorDiv`/`floorMod`).

### D5. Confirmação seletiva + DLQ

- Só é feito `delete` (ack) de mensagens processadas com sucesso; falha de
  banco não confirma nada → redelivery após o visibility timeout (retry natural);
- Payload malformado não é confirmado → após `maxReceiveCount` (RedrivePolicy)
  a mensagem vai para a **DLQ** (`transacoes-financeiras-processadas-dlq`),
  não trava a fila e permite análise manual;
- Resilience4j: retry com backoff exponencial + circuit breaker nas falhas
  transientes (banco/SQS).

## Consequências

**Positivas**

- Correção sob as três condições mais difíceis do SQS: duplicata, fora de
  ordem e concorrência entre workers.
- Nenhum estado em memória: o ingestor é stateless e escala horizontalmente.

**Negativas / trade-offs**

- Não há histórico de transações (só o registro de idempotência) — o escopo é
  consulta de saldo; se um dia houver requisito de extrato, aí sim será preciso
  persistir a tabela de transações completa (novo ADR).
- Dependência de SQL nativo PostgreSQL (`ON CONFLICT`) — o banco não é
  portável sem reescrita (aceitável: banco é decisão de infraestrutura).
- Mensagens consumidas são processadas sequencialmente por ciclo — throughput
  por instância é limitado pelo batch; escala com mais réplicas.
