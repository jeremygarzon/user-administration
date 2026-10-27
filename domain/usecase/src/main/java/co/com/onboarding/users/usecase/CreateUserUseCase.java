package co.com.onboarding.users.usecase;

import co.com.onboarding.users.model.User;
import co.com.onboarding.users.model.UserCreation;
import co.com.onboarding.users.model.exception.BusinessException;
import co.com.onboarding.users.model.gateways.UserEventPublisherPort;
import co.com.onboarding.users.model.gateways.UserProviderPort;
import co.com.onboarding.users.model.gateways.UserRepositoryPort;
import lombok.RequiredArgsConstructor;
import reactor.core.publisher.Mono;

import static co.com.onboarding.users.model.exception.BusinessErrorMessage.USER_NOT_FOUND;

@RequiredArgsConstructor
public class CreateUserUseCase {

    private final UserRepositoryPort userRepositoryPort;
    private final UserProviderPort userProviderPort;
    private final UserEventPublisherPort userEventPublisherPort;

    public Mono<UserCreation> execute(Long id) {
        return userRepositoryPort.findById(id)
                .map(existingUser -> new UserCreation(existingUser, false))
                .switchIfEmpty(Mono.defer(() -> createFromProvider(id)
                        .map(createdUser -> new UserCreation(createdUser, true))));
    }

    private Mono<User> createFromProvider(Long id) {
        return userProviderPort.findById(id)
                .switchIfEmpty(Mono.error(() -> new BusinessException(USER_NOT_FOUND)))
                .flatMap(userRepositoryPort::save)
                .flatMap(user -> userEventPublisherPort.publishUserCreated(user).thenReturn(user));
    }
}
