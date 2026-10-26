package co.com.onboarding.users.usecase;

import co.com.onboarding.users.model.User;
import co.com.onboarding.users.model.exception.UserNotFoundException;
import co.com.onboarding.users.model.gateways.UserProviderPort;
import co.com.onboarding.users.model.gateways.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class CreateUserUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final UserProviderPort userProviderPort;

    public Mono<User> execute(Long id) {
        return userRepositoryPort.findById(id)
                .switchIfEmpty(Mono.defer(() -> createFromProvider(id)));
    }

    private Mono<User> createFromProvider(Long id) {
        return userProviderPort.findById(id)
                .switchIfEmpty(Mono.error(() -> new UserNotFoundException(id)))
                .flatMap(userRepositoryPort::save);
    }
}
