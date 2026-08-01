# ADR 001 — Escolha do Banco de Dados

- **Status:** Aceito
- **Data:** Julho/2026

## Contexto

O sistema precisa persistir o saldo mais atual de cada conta bancária e
consultá-lo de forma consistente. O saldo é um valor financeiro: erros de
consistência são inaceitáveis (uma conta pode "duplicar" ou "perder" dinheiro
se dois eventos forem aplicados de forma incorreta). Além disso, o volume de
escrita é alto (até 2.000 mensagens/s em picos), mas o acesso é simples: uma
linha por conta (UUID) e consulta por chave primária.

## Decisão

**PostgreSQL 15** como banco de dados relacional, com:

- Migrações versionadas via **Flyway** (módulo compartilhado `db-migrations`,
  schema como contrato único entre os microsserviços);
- `NUMERIC(19,2)` para valores monetários (nunca `float`/`double`);
- `TIMESTAMPTZ` para timestamps (mapeados como `Instant` no Java);
- `ddl-auto: validate` (o schema nunca é criado pelo Hibernate);
- Índices: apenas PKs — o acesso é sempre por chave primária (`account_id`,
  `transaction_id`), sem varredura.

## Alternativas consideradas

| Alternativa | Por quê foi descartada |
|---|---|
| **DynamoDB / NoSQL** | Escala bem para escrita, mas exigiria modelar a consistência manualmente (idempotência + last-write-wins "à mão"). O acesso por PK não justifica o custo. |
| **Redis como fonte da verdade** | Excelente cache, mas persistência/durabilidade e consultas ad-hoc são mais frágeis; adequado como cache, não como sistema de registro. |
| **Banco de eventos (event sourcing)** | Over-engineering: a mensagem já carrega o saldo consolidado; não precisamos reconstruir o estado a partir de um log de eventos. |

## Consequências

**Positivas**

- Transações ACID: idempotência (registro da transação) e atualização de saldo
  na **mesma transação** — consistência forte.
- Relacionamento `balances.account_id → accounts.id` via FK: integridade.
- Ferramentas maduras: backups (PITR), réplicas de leitura, Multi-AZ no RDS.
- No teorema CAP, o sistema prioriza **Consistência (CP)** — o trade-off
  correto para saldo financeiro: em partição de rede, prefere-se recusar
  leituras (ou servir de réplica consistente) a servir um saldo errado.

**Negativas / trade-offs**

- Escrita no Postgres é o gargalo potencial: em escala real exigiria réplicas,
  particionamento ou escritas em lote maiores (documentado no roadmap).
- Um banco relacional é menos "elástico" que um NoSQL gerenciado para picos
  de escrita — mitigado por pool de conexões e batch.
