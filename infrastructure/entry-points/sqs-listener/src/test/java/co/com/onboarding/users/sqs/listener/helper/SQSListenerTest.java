package co.com.onboarding.users.sqs.listener.helper;

import co.com.onboarding.users.sqs.listener.config.SQSProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.DeleteMessageResponse;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageResponse;
import software.amazon.awssdk.services.sqs.model.SqsException;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class SQSListenerTest {

    @Mock
    private SqsAsyncClient asyncClient;

    private SQSProperties sqsProperties;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        sqsProperties = new SQSProperties(
                "us-east-1",
                "http://localhost:4566",
                "http://localhost:4566/00000000000/queueName",
                20,
                30,
                10,
                1
        );

        var message = Message.builder().body("message").build();
        var deleteMessageResponse = DeleteMessageResponse.builder().build();
        var messageResponse = ReceiveMessageResponse.builder().messages(message).build();

        when(asyncClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(messageResponse));
        when(asyncClient.deleteMessage(any(DeleteMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(deleteMessageResponse));
    }

    @Test
    void listenerTest() {
        var sqsListener = SQSListener.builder()
                .client(asyncClient)
                .properties(sqsProperties)
                .processor(message -> Mono.empty())
                .operation("operation")
                .build();

        Flux<Void> flow = ReflectionTestUtils.invokeMethod(sqsListener, "listen");
        StepVerifier.create(flow).verifyComplete();
    }

    @Test
    void failedMessageIsNotDeletedWhileOthersAre() {
        var ok = Message.builder().messageId("ok").receiptHandle("ok-handle").body("ok").build();
        var poison = Message.builder().messageId("poison").receiptHandle("poison-handle").body("poison").build();
        when(asyncClient.receiveMessage(any(ReceiveMessageRequest.class)))
                .thenReturn(CompletableFuture.completedFuture(ReceiveMessageResponse.builder().messages(ok, poison).build()));

        var sqsListener = SQSListener.builder()
                .client(asyncClient)
                .properties(sqsProperties)
                .processor(message -> "poison".equals(message.body())
                        ? Mono.error(new IllegalStateException("cannot process"))
                        : Mono.empty())
                .operation("operation")
                .build();

        Flux<Void> flow = ReflectionTestUtils.invokeMethod(sqsListener, "listen");
        StepVerifier.create(flow).verifyComplete();

        ArgumentCaptor<DeleteMessageRequest> deleted = ArgumentCaptor.forClass(DeleteMessageRequest.class);
        verify(asyncClient).deleteMessage(deleted.capture());
        assertThat(deleted.getValue().receiptHandle()).isEqualTo("ok-handle");
    }

    @Test
    void keepsPollingAfterAReceiveFailure() {
        AtomicInteger calls = new AtomicInteger();
        when(asyncClient.receiveMessage(any(ReceiveMessageRequest.class))).thenAnswer(invocation ->
                calls.incrementAndGet() == 1
                        ? CompletableFuture.failedFuture(SqsException.builder().message("queue unreachable").build())
                        : CompletableFuture.completedFuture(ReceiveMessageResponse.builder().build()));

        SQSListener.builder()
                .client(asyncClient)
                .properties(sqsProperties)
                .processor(message -> Mono.empty())
                .build()
                .start();

        verify(asyncClient, timeout(5000).atLeast(2)).receiveMessage(any(ReceiveMessageRequest.class));
    }
}
