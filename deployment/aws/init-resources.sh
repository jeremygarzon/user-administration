#!/usr/bin/env sh
# Creates the AWS resources used by the application on a local emulator (MiniStack/LocalStack).
# Safe to run more than once.
set -eu

ENDPOINT="${AWS_ENDPOINT:-http://localhost:4566}"
export AWS_ACCESS_KEY_ID="${AWS_ACCESS_KEY_ID:-test}"
export AWS_SECRET_ACCESS_KEY="${AWS_SECRET_ACCESS_KEY:-test}"
export AWS_REGION="${AWS_REGION:-us-east-1}"

aws_local() {
  aws --endpoint-url "$ENDPOINT" "$@"
}

# Messages that fail 3 times are moved to the dead-letter queue instead of being retried forever.
DLQ_URL=$(aws_local sqs create-queue --queue-name user-created-events-dlq --query QueueUrl --output text)
DLQ_ARN=$(aws_local sqs get-queue-attributes --queue-url "$DLQ_URL" \
  --attribute-names QueueArn --query Attributes.QueueArn --output text)
REDRIVE_POLICY=$(printf '{"RedrivePolicy":"{\\"deadLetterTargetArn\\":\\"%s\\",\\"maxReceiveCount\\":\\"3\\"}"}' "$DLQ_ARN")
aws_local sqs create-queue --queue-name user-created-events --attributes "$REDRIVE_POLICY" \
  --query QueueUrl --output text

if ! aws_local dynamodb describe-table --table-name users >/dev/null 2>&1; then
  aws_local dynamodb create-table --table-name users \
    --attribute-definitions AttributeName=id,AttributeType=N \
    --key-schema AttributeName=id,KeyType=HASH \
    --billing-mode PAY_PER_REQUEST \
    --query TableDescription.TableName --output text
fi
