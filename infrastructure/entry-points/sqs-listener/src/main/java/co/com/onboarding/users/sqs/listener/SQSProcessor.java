package co.com.onboarding.users.sqs.listener;

import co.com.onboarding.users.model.User;
import co.com.onboarding.users.usecase.ProcessCreatedUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.services.sqs.model.Message;
import tools.jackson.databind.json.JsonMapper;

import java.util.function.Function;

@Service
@RequiredArgsConstructor
public class SQSProcessor implements Function<Message, Mono<Void>> {
    private final ProcessCreatedUserUseCase processCreatedUserUseCase;
    private final JsonMapper jsonMapper;

    @Override
    public Mono<Void> apply(Message message) {
        return Mono.fromCallable(() -> jsonMapper.readValue(message.body(), User.class))
                .flatMap(processCreatedUserUseCase::execute);
    }
}
