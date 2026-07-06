#!/bin/sh
# Se ejecuta automáticamente al arrancar LocalStack (docker-entrypoint-initaws.d).
# Crea el tópico SNS de Incidentes, la cola SQS (+ DLQ) de Asignaciones y la
# suscripción SNS -> SQS, simulando localmente lo que el equipo de Incidentes
# tiene configurado en AWS real.
set -e

REGION="us-east-1"
TOPIC_NAME="queue_assignations"
QUEUE_NAME="queue_assignations"
DLQ_NAME="queue_assignations-dlq"

DLQ_URL=$(awslocal sqs create-queue --queue-name "$DLQ_NAME" --region "$REGION" --query QueueUrl --output text)
DLQ_ARN=$(awslocal sqs get-queue-attributes --queue-url "$DLQ_URL" --attribute-names QueueArn --region "$REGION" --query Attributes.QueueArn --output text)

QUEUE_URL=$(awslocal sqs create-queue --queue-name "$QUEUE_NAME" --region "$REGION" \
  --attributes "{\"RedrivePolicy\":\"{\\\"deadLetterTargetArn\\\":\\\"$DLQ_ARN\\\",\\\"maxReceiveCount\\\":\\\"5\\\"}\"}" \
  --query QueueUrl --output text)
QUEUE_ARN=$(awslocal sqs get-queue-attributes --queue-url "$QUEUE_URL" --attribute-names QueueArn --region "$REGION" --query Attributes.QueueArn --output text)

TOPIC_ARN=$(awslocal sns create-topic --name "$TOPIC_NAME" --region "$REGION" --query TopicArn --output text)

awslocal sqs set-queue-attributes --queue-url "$QUEUE_URL" --region "$REGION" --attributes "{
  \"Policy\": \"{\\\"Version\\\":\\\"2012-10-17\\\",\\\"Statement\\\":[{\\\"Effect\\\":\\\"Allow\\\",\\\"Principal\\\":\\\"*\\\",\\\"Action\\\":\\\"sqs:SendMessage\\\",\\\"Resource\\\":\\\"$QUEUE_ARN\\\",\\\"Condition\\\":{\\\"ArnEquals\\\":{\\\"aws:SourceArn\\\":\\\"$TOPIC_ARN\\\"}}}]}\"
}"

awslocal sns subscribe --topic-arn "$TOPIC_ARN" --protocol sqs --notification-endpoint "$QUEUE_ARN" --region "$REGION"

echo "LocalStack listo: topic=$TOPIC_ARN queue=$QUEUE_URL dlq=$DLQ_URL"