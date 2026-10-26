package co.com.onboarding.users.sqs.sender;

import co.com.onboarding.users.model.User;
import co.com.onboarding.users.model.gateways.UserEventPublisherPort;
import co.com.onboarding.users.sqs.sender.config.SQSSenderProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.SendMessageRequest;
import software.amazon.awssdk.services.sqs.model.SendMessageResponse;
import tools.jackson.databind.json.JsonMapper;

@Service
@Slf4j
@RequiredArgsConstructor
public class SQSSender implements UserEventPublisherPort {
    private final SQSSenderProperties properties;
    private final SqsAsyncClient client;
    private final JsonMapper jsonMapper;

    @Override
    public Mono<Void> publishUserCreated(User user) {
        return Mono.fromCallable(() -> jsonMapper.writeValueAsString(user))
                .flatMap(this::send)
                .doOnNext(messageId -> log.info("User created event sent for user {} with message id {}",
                        user.id(), messageId))
                .onErrorResume(error -> {
                    log.error("Could not send user created event for user {}", user.id(), error);
                    return Mono.empty();
                })
                .then();
    }

    public Mono<String> send(String message) {
        return Mono.fromCallable(() -> buildRequest(message))
                .flatMap(request -> Mono.fromFuture(client.sendMessage(request)))
                .doOnNext(response -> log.debug("Message sent {}", response.messageId()))
                .map(SendMessageResponse::messageId);
    }

    private SendMessageRequest buildRequest(String message) {
        return SendMessageRequest.builder()
                .queueUrl(properties.queueUrl())
                .messageBody(message)
                .build();
    }
}
