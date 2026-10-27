package co.com.onboarding.users.usecase;

import co.com.onboarding.users.model.User;
import co.com.onboarding.users.model.UserCreation;
import co.com.onboarding.users.model.exception.BusinessException;
import co.com.onboarding.users.model.exception.TechnicalException;
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
import reactor.util.context.Context;

import java.util.concurrent.atomic.AtomicReference;

import static co.com.onboarding.users.model.exception.BusinessErrorMessage.USER_NOT_FOUND;
import static co.com.onboarding.users.model.exception.TechnicalErrorMessage.USER_PROVIDER_UNAVAILABLE;
import static org.assertj.core.api.Assertions.assertThat;
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
    void failsWithUserNotFoundWhenProviderHasNoUser() {
        when(userRepositoryPort.findById(USER_ID)).thenReturn(Mono.empty());
        when(userProviderPort.findById(USER_ID)).thenReturn(Mono.empty());

        StepVerifier.create(createUserUseCase.execute(USER_ID))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOfSatisfying(BusinessException.class,
                                exception -> assertThat(exception.getErrorMessage()).isEqualTo(USER_NOT_FOUND)))
                .verify();

        verify(userRepositoryPort, never()).save(any());
        verify(userEventPublisherPort, never()).publishUserCreated(any());
    }

    @Test
    void propagatesProviderErrorWithoutSaving() {
        TechnicalException providerError = new TechnicalException(USER_PROVIDER_UNAVAILABLE, new RuntimeException());
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

    @Test
    void publishesEventWithTheCallerReactorContext() {
        AtomicReference<String> tracedValue = new AtomicReference<>();
        when(userRepositoryPort.findById(USER_ID)).thenReturn(Mono.empty());
        when(userProviderPort.findById(USER_ID)).thenReturn(Mono.just(USER));
        when(userRepositoryPort.save(USER)).thenReturn(Mono.just(USER));
        when(userEventPublisherPort.publishUserCreated(USER)).thenReturn(Mono.deferContextual(context -> {
            tracedValue.set(context.getOrDefault("trace", "missing"));
            return Mono.empty();
        }));

        StepVerifier.create(createUserUseCase.execute(USER_ID).contextWrite(Context.of("trace", "abc")))
                .expectNextCount(1)
                .verifyComplete();

        assertThat(tracedValue.get()).isEqualTo("abc");
    }
}
