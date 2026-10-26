package co.com.onboarding.users.sqs.sender;

import co.com.onboarding.users.model.User;
import co.com.onboarding.users.sqs.sender.config.SQSSenderProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;
import tools.jackson.databind.json.JsonMapper;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SQSSenderTest {

    private static final String QUEUE_URL = "http://localhost:4566/000000000000/user-created-events";
    private static final User USER = new User(1L, "george.bluth@reqres.in", "George", "Bluth", null);

    @Mock
    private SqsAsyncClient client;

    private final JsonMapper jsonMapper = JsonMapper.builder().build();

    private SQSSender sender;

    @BeforeEach
    void setUp() {
        sender = new SQSSender(new SQSSenderProperties("us-east-1", QUEUE_URL, null), client, jsonMapper);
    }

    @Test
    void publishUserCreatedSendsUserAsJsonToTheQueue() {
        when(client.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(SendMessageResponse.builder().messageId("msg-1").build()));

        StepVerifier.create(sender.publishUserCreated(USER))
                .verifyComplete();

        ArgumentCaptor<SendMessageRequest> request = ArgumentCaptor.forClass(SendMessageRequest.class);
        verify(client).sendMessage(request.capture());
        assertThat(request.getValue().queueUrl()).isEqualTo(QUEUE_URL);
        assertThat(jsonMapper.readValue(request.getValue().messageBody(), User.class)).isEqualTo(USER);
    }

    @Test
    void publishUserCreatedCompletesWhenQueueFails() {
        when(client.sendMessage(any(SendMessageRequest.class)))
                .thenReturn(CompletableFuture.failedFuture(SqsException.builder().message("queue does not exist").build()));

        StepVerifier.create(sender.publishUserCreated(USER))
                .verifyComplete();
    }
}
