package co.com.onboarding.users.dynamodb.config;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;

import java.net.URI;

import static org.assertj.core.api.Assertions.assertThat;

@ExtendWith(MockitoExtension.class)
class DynamoDBConfigTest {

    @Mock
    private MetricPublisher publisher;

    @Mock
    private DynamoDbAsyncClient dynamoDbAsyncClient;

    private final DynamoDBConfig dynamoDBConfig = new DynamoDBConfig();

    @Test
    void usesEndpointOverrideWhenConfigured() {
        try (DynamoDbAsyncClient client = dynamoDBConfig.amazonDynamoDB("http://localhost:4566", "us-east-1", publisher)) {
            assertThat(client.serviceClientConfiguration().endpointOverride()).contains(URI.create("http://localhost:4566"));
        }
    }

    @Test
    void usesRegionalEndpointWhenEndpointIsBlank() {
        try (DynamoDbAsyncClient client = dynamoDBConfig.amazonDynamoDB("", "us-east-1", publisher)) {
            assertThat(client.serviceClientConfiguration().endpointOverride()).isEmpty();
        }
    }

    @Test
    void buildsEnhancedClient() {
        DynamoDbEnhancedAsyncClient result = dynamoDBConfig.getDynamoDbEnhancedAsyncClient(dynamoDbAsyncClient);

        assertThat(result).isNotNull();
    }
}
