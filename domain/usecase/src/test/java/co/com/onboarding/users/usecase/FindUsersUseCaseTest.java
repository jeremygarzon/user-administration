package co.com.onboarding.users.usecase;

import co.com.onboarding.users.model.User;
import co.com.onboarding.users.model.exception.BusinessException;
import co.com.onboarding.users.model.gateways.UserCachePort;
import co.com.onboarding.users.model.gateways.UserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FindUsersUseCaseTest {

    private static final User GEORGE = new User(1L, "george.bluth@reqres.in", "George", "Bluth", null);
    private static final User JANET = new User(2L, "janet.weaver@reqres.in", "Janet", "Weaver", null);

    @Mock
    private UserRepositoryPort userRepositoryPort;

    @Mock
    private UserCachePort userCachePort;

    @InjectMocks
    private FindUsersUseCase findUsersUseCase;

    @Test
    void findByIdReturnsCachedUserWithoutQueryingRepository() {
        when(userCachePort.findById(1L)).thenReturn(Mono.just(GEORGE));

        StepVerifier.create(findUsersUseCase.findById(1L))
                .expectNext(GEORGE)
                .verifyComplete();

        verify(userRepositoryPort, never()).findById(any());
        verify(userCachePort, never()).save(any());
    }

    @Test
    void findByIdQueriesRepositoryAndCachesUserOnCacheMiss() {
        when(userCachePort.findById(1L)).thenReturn(Mono.empty());
        when(userRepositoryPort.findById(1L)).thenReturn(Mono.just(GEORGE));
        when(userCachePort.save(GEORGE)).thenReturn(Mono.empty());

        StepVerifier.create(findUsersUseCase.findById(1L))
                .expectNext(GEORGE)
                .verifyComplete();

        verify(userCachePort).save(GEORGE);
    }

    @Test
    void findByIdFailsWithUserNotFoundAndCachesNothingWhenNotStored() {
        when(userCachePort.findById(99L)).thenReturn(Mono.empty());
        when(userRepositoryPort.findById(99L)).thenReturn(Mono.empty());

        StepVerifier.create(findUsersUseCase.findById(99L))
                .expectError(BusinessException.class)
                .verify();

        verify(userCachePort, never()).save(any());
    }

    @Test
    void findAllReturnsEveryStoredUser() {
        when(userRepositoryPort.findAll()).thenReturn(Flux.just(GEORGE, JANET));

        StepVerifier.create(findUsersUseCase.findAll())
                .expectNext(GEORGE, JANET)
                .verifyComplete();
    }

    @Test
    void findByNameSearchesWithTrimmedName() {
        when(userRepositoryPort.findByName("geo")).thenReturn(Flux.just(GEORGE));

        StepVerifier.create(findUsersUseCase.findByName("  geo "))
                .expectNext(GEORGE)
                .verifyComplete();
    }
}
