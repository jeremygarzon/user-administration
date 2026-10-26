package co.com.onboarding.users.usecase;

import co.com.onboarding.users.model.User;
import co.com.onboarding.users.model.gateways.UppercaseUserRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class ProcessCreatedUserUseCase {

    private final UppercaseUserRepositoryPort uppercaseUserRepositoryPort;

    public Mono<Void> execute(User user) {
        return uppercaseUserRepositoryPort.save(user.toUpperCase())
                .then();
    }
}
