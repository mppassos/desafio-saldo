# ADR 004 — Deploy (Blue/Green) e Observabilidade

- **Status:** Aceito (proposta para produção)
- **Data:** Julho/2026

## Contexto

O desafio pede uma estratégia de deploy que **mitigue o risco de um bug
impactar todos os clientes** e uma aplicação *production-ready* (logging,
métricas, health checks).

## Decisões

### D1. Deploy Blue/Green (com canary opcional)

Dois ambientes idênticos (Blue = atual, Green = nova versão):

1. CI verde (`build` + `unit-tests` + `integration-tests`);
2. Imagem publicada no ECR e nova revisão criada no CodeDeploy/ECS;
3. O Green é validado por **health checks** (Actuator) antes de receber tráfego;
4. Tráfego migrado **gradualmente** (weighted routing — 5% → 25% → 100%);
5. **Rollback automático** para o Blue se os health checks falharem (ou
   rollback manual imediato via RunBook).

Alternativa citada nas referências do desafio: **canary releases** — liberar
a nova versão para uma fração dos usuários e observar métricas de erro antes
de generalizar. Pode ser combinado com Blue/Green (canary dentro do Green).

### D2. Observabilidade

- **Health checks:** Spring Actuator (`/actuator/health`, `/liveness`,
  `/readiness`) — usados pelos target groups do ALB e pelo orquestrador;
- **Métricas:** Micrometer + Prometheus (`/actuator/prometheus`) — contadores
  do ingestor (`applied`, `duplicate`, `stale`, `not_approved`, `malformed`),
  latência HTTP da API, pool de conexões, JVM;
- **Logs:** estruturados (JSON em produção), com `messageId`/`transactionId`
  para correlação; erros com pilha completa (sem expor detalhes ao cliente);
- **Alertas sugeridos:** taxa de `stale` alta (atraso na fila), `malformed`
  crescendo (contrato quebrado), erro 5xx, fila com mais de X mensagens.

### D3. Infraestrutura em cloud (proposta)

`Internet → API Gateway → ALB → ECS Fargate (api) → RDS PostgreSQL Multi-AZ`

`SQS → ECS Fargate (ingestor) → RDS PostgreSQL Multi-AZ`

- ElastiCache (Redis) como cache opcional de saldo;
- CloudWatch (logs + métricas + alarmes), S3 (artefatos/backups);
- Ver `docs/diagrama-cloud.puml`.

## Consequências

**Positivas**

- Risco de bug generalizado mitigado (rollback em segundos);
- Sistema operável: métricas e health checks permitem SLOs/alertas
  (princípios de SRE/Well-Architected).

**Negativas / trade-offs**

- Custo de manter dois ambientes simultâneos (Blue/Green);
- Automação de deploy real exige conta AWS/credenciais — no repositório fica
  a proposta executável (pipeline) com os comandos de produção parametrizados.
