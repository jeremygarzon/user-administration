package co.com.onboarding.users.sqs.listener;

import co.com.onboarding.users.model.User;
import co.com.onboarding.users.usecase.ProcessCreatedUserUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import software.amazon.awssdk.services.sqs.model.Message;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SQSProcessorTest {

    @Mock
    private ProcessCreatedUserUseCase processCreatedUserUseCase;

    private SQSProcessor processor;

    @BeforeEach
    void setUp() {
        processor = new SQSProcessor(processCreatedUserUseCase, JsonMapper.builder().build());
    }

    @Test
    void processesTheUserCarriedByTheMessage() {
        User user = new User(1L, "george.bluth@reqres.in", "George", "Bluth", null);
        when(processCreatedUserUseCase.execute(user)).thenReturn(Mono.empty());
        Message message = Message.builder()
                .body("{\"id\":1,\"email\":\"george.bluth@reqres.in\",\"firstName\":\"George\",\"lastName\":\"Bluth\"}")
                .build();

        StepVerifier.create(processor.apply(message))
                .verifyComplete();

        verify(processCreatedUserUseCase).execute(user);
    }

    @Test
    void failsOnMalformedMessageSoItIsNotDeleted() {
        Message message = Message.builder().body("not json").build();

        StepVerifier.create(processor.apply(message))
                .expectError(JacksonException.class)
                .verify();

        verify(processCreatedUserUseCase, never()).execute(any());
    }
}
