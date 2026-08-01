#!/usr/bin/env bash
# ============================================================================
# Cria a fila SQS (e a DLQ com RedrivePolicy) no Localstack.
# Opcional: o message-generator do docker-compose já cria a fila principal.
# Uso: ./scripts/create-queue.sh
# ============================================================================
set -uo pipefail

ENDPOINT="${LOCALSTACK_ENDPOINT:-http://localhost:4566}"
REGION="${AWS_DEFAULT_REGION:-sa-east-1}"
QUEUE_NAME="transacoes-financeiras-processadas"
DLQ_NAME="${QUEUE_NAME}-dlq"
ACCOUNT_ID="000000000000"
MAX_RECEIVE_COUNT="5"

aws --version >/dev/null 2>&1 || { echo "AWS CLI não encontrado. Instale: https://aws.amazon.com/cli/"; exit 1; }

echo "==> Criando DLQ '${DLQ_NAME}'..."
DLQ_URL=$(aws --endpoint-url "${ENDPOINT}" --region "${REGION}" sqs create-queue \
  --queue-name "${DLQ_NAME}" \
  --query 'QueueUrl' --output text 2>/dev/null) || echo "    (DLQ já existe ou erro; seguindo...)"

DLQ_ARN="arn:aws:sqs:${REGION}:${ACCOUNT_ID}:${DLQ_NAME}"
REDRIVE="{\"deadLetterTargetArn\":\"${DLQ_ARN}\",\"maxReceiveCount\":\"${MAX_RECEIVE_COUNT}\"}"

echo "==> Criando fila principal '${QUEUE_NAME}' com RedrivePolicy (maxReceiveCount=${MAX_RECEIVE_COUNT})..."
aws --endpoint-url "${ENDPOINT}" --region "${REGION}" sqs create-queue \
  --queue-name "${QUEUE_NAME}" \
  --attributes "{\"RedrivePolicy\":\"${REDRIVE}\"}" >/dev/null 2>&1 \
  || echo "    (fila já existe — verifique se a RedrivePolicy está configurada)"

echo "==> Filas disponíveis:"
aws --endpoint-url "${ENDPOINT}" --region "${REGION}" sqs list-queues

echo ""
echo "Para conferir as mensagens (10 primeiras):"
echo "  aws --endpoint-url ${ENDPOINT} --region ${REGION} sqs receive-message \\"
echo "      --queue-url ${ENDPOINT}/${ACCOUNT_ID}/${QUEUE_NAME} --max-number-of-messages 10"
