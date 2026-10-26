package co.com.onboarding.users.usecase;

import co.com.onboarding.users.model.User;
import co.com.onboarding.users.model.exception.UserNotFoundException;
import co.com.onboarding.users.model.gateways.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@RequiredArgsConstructor
public class FindUsersUseCase {

    private final UserRepositoryPort userRepositoryPort;

    public Mono<User> findById(Long id) {
        return userRepositoryPort.findById(id)
                .switchIfEmpty(Mono.error(() -> new UserNotFoundException(id)));
    }

    public Flux<User> findAll() {
        return userRepositoryPort.findAll();
    }

    public Flux<User> findByName(String name) {
        return userRepositoryPort.findByName(name.trim());
    }
}
