package co.com.onboarding.users.sqs.listener;

import co.com.onboarding.users.model.User;
import co.com.onboarding.users.usecase.ProcessCreatedUserUseCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;
import tools.jackson.databind.json.JsonMapper;

import java.util.function.Function;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@Service
@RequiredArgsConstructor
public class SQSProcessor implements Function<Message, Mono<Void>> {

    private static final String EVENT_PROCESSED = "User created event processed";

    private final ProcessCreatedUserUseCase processCreatedUserUseCase;
    private final JsonMapper jsonMapper;

    @Override
    public Mono<Void> apply(Message message) {
        return Mono.fromCallable(() -> jsonMapper.readValue(message.body(), User.class))
                .flatMap(user -> processCreatedUserUseCase.execute(user)
                        .doOnSuccess(ignored -> log.info(EVENT_PROCESSED,
                                kv("userId", user.id()), kv("messageId", message.messageId()))));
    }
}
