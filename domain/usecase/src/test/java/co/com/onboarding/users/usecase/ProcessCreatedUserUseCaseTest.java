package co.com.onboarding.users.usecase;

import co.com.onboarding.users.model.User;
import co.com.onboarding.users.model.gateways.UppercaseUserRepositoryPort;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessCreatedUserUseCaseTest {

    @Mock
    private UppercaseUserRepositoryPort uppercaseUserRepositoryPort;

    @InjectMocks
    private ProcessCreatedUserUseCase processCreatedUserUseCase;

    @Test
    void storesUserWithUppercasePersonalData() {
        User user = new User(1L, "george.bluth@reqres.in", "George", "Bluth", "https://reqres.in/img/faces/1-image.jpg");
        User expected = new User(1L, "GEORGE.BLUTH@REQRES.IN", "GEORGE", "BLUTH", "https://reqres.in/img/faces/1-image.jpg");
        when(uppercaseUserRepositoryPort.save(any())).thenAnswer(invocation -> Mono.just(invocation.getArgument(0)));

        StepVerifier.create(processCreatedUserUseCase.execute(user))
                .verifyComplete();

        verify(uppercaseUserRepositoryPort).save(expected);
    }

    @Test
    void propagatesStorageErrorsSoTheMessageIsRetried() {
        User user = new User(1L, "george.bluth@reqres.in", "George", "Bluth", null);
        when(uppercaseUserRepositoryPort.save(any())).thenReturn(Mono.error(new IllegalStateException("table missing")));

        StepVerifier.create(processCreatedUserUseCase.execute(user))
                .expectError(IllegalStateException.class)
                .verify();
    }
}
