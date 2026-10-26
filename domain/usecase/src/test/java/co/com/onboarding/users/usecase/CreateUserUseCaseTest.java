package co.com.onboarding.users.usecase;

import co.com.onboarding.users.model.User;
import co.com.onboarding.users.model.UserCreation;
import co.com.onboarding.users.model.exception.UserNotFoundException;
import co.com.onboarding.users.model.exception.UserProviderException;
import co.com.onboarding.users.model.gateways.UserEventPublisherPort;
import co.com.onboarding.users.model.gateways.UserProviderPort;
import co.com.onboarding.users.model.gateways.UserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CreateUserUseCaseTest {

    private static final Long USER_ID = 1L;
    private static final User USER = new User(
            USER_ID, "george.bluth@reqres.in", "George", "Bluth", "https://reqres.in/img/faces/1-image.jpg");

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private UserProviderPort userProviderPort;

    @Mock
    private UserEventPublisherPort userEventPublisherPort;

    @InjectMocks
    private CreateUserUseCase createUserUseCase;

    @Test
    void returnsExistingUserWithoutCallingProvider() {
        when(userRepositoryPort.findById(USER_ID)).thenReturn(Mono.just(USER));

        StepVerifier.create(createUserUseCase.execute(USER_ID))
                .expectNext(new UserCreation(USER, false))
                .verifyComplete();

        verify(userProviderPort, never()).findById(any());
        verify(userRepositoryPort, never()).save(any());
        verify(userEventPublisherPort, never()).publishUserCreated(any());
    }

    @Test
    void fetchesAndSavesUserWhenNotStored() {
        when(userRepositoryPort.findById(USER_ID)).thenReturn(Mono.empty());
        when(userProviderPort.findById(USER_ID)).thenReturn(Mono.just(USER));
        when(userRepositoryPort.save(USER)).thenReturn(Mono.just(USER));
        when(userEventPublisherPort.publishUserCreated(USER)).thenReturn(Mono.empty());

        StepVerifier.create(createUserUseCase.execute(USER_ID))
                .expectNext(new UserCreation(USER, true))
                .verifyComplete();

        verify(userRepositoryPort).save(USER);
        verify(userEventPublisherPort).publishUserCreated(USER);
    }

    @Test
    void returnsCreatedUserWithoutWaitingForTheEvent() {
        when(userRepositoryPort.findById(USER_ID)).thenReturn(Mono.empty());
        when(userProviderPort.findById(USER_ID)).thenReturn(Mono.just(USER));
        when(userRepositoryPort.save(USER)).thenReturn(Mono.just(USER));
        when(userEventPublisherPort.publishUserCreated(USER)).thenReturn(Mono.never());

        StepVerifier.create(createUserUseCase.execute(USER_ID))
                .expectNext(new UserCreation(USER, true))
                .expectComplete()
                .verify(Duration.ofSeconds(1));
    }

    @Test
    void failsWithUserNotFoundWhenProviderHasNoUser() {
        when(userRepositoryPort.findById(USER_ID)).thenReturn(Mono.empty());
        when(userProviderPort.findById(USER_ID)).thenReturn(Mono.empty());

        StepVerifier.create(createUserUseCase.execute(USER_ID))
                .expectError(UserNotFoundException.class)
                .verify();

        verify(userRepositoryPort, never()).save(any());
        verify(userEventPublisherPort, never()).publishUserCreated(any());
    }

    @Test
    void propagatesProviderErrorWithoutSaving() {
        UserProviderException providerError = new UserProviderException("provider down", new RuntimeException());
        when(userRepositoryPort.findById(USER_ID)).thenReturn(Mono.empty());
        when(userProviderPort.findById(USER_ID)).thenReturn(Mono.error(providerError));

        StepVerifier.create(createUserUseCase.execute(USER_ID))
                .expectErrorMatches(error -> error == providerError)
                .verify();

        verify(userRepositoryPort, never()).save(any());
    }

    @Test
    void propagatesPersistenceError() {
        IllegalStateException persistenceError = new IllegalStateException("database down");
        when(userRepositoryPort.findById(USER_ID)).thenReturn(Mono.empty());
        when(userProviderPort.findById(USER_ID)).thenReturn(Mono.just(USER));
        when(userRepositoryPort.save(USER)).thenReturn(Mono.error(persistenceError));

        StepVerifier.create(createUserUseCase.execute(USER_ID))
                .expectErrorMatches(error -> error == persistenceError)
                .verify();

        verify(userEventPublisherPort, never()).publishUserCreated(any());
    }
}
