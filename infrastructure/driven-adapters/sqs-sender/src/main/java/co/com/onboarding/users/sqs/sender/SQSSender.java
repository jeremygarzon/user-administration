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

import static net.logstash.logback.argument.StructuredArguments.kv;

@Service
@Slf4j
@RequiredArgsConstructor
public class SQSSender implements UserEventPublisherPort {

    private static final String EVENT_SENT = "User created event sent";
    private static final String EVENT_NOT_SENT = "Could not send user created event";

    private final SQSSenderProperties properties;
    private final SqsAsyncClient client;
    private final JsonMapper jsonMapper;

    @Override
    public Mono<Void> publishUserCreated(User user) {
        return Mono.fromCallable(() -> jsonMapper.writeValueAsString(user))
                .flatMap(this::send)
                .doOnNext(messageId -> log.info(EVENT_SENT, kv("userId", user.id()), kv("messageId", messageId)))
                .onErrorResume(error -> {
                    log.error(EVENT_NOT_SENT, kv("userId", user.id()), error);
                    return Mono.empty();
                })
                .then();
    }

    private Mono<String> send(String message) {
        return Mono.fromFuture(() -> client.sendMessage(buildRequest(message)))
                .map(SendMessageResponse::messageId);
    }

    private SendMessageRequest buildRequest(String message) {
        return SendMessageRequest.builder()
                .queueUrl(properties.queueUrl())
                .messageBody(message)
                .build();
    }
}
