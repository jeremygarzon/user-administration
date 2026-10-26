package co.com.onboarding.users.r2dbc;

import co.com.onboarding.users.model.User;
import co.com.onboarding.users.r2dbc.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserRepositoryAdapterTest {

    private static final User USER = new User(1L, "test@example.com", "Test", "User", "https://example.com/avatar.png");

    @Mock
    private UserReactiveRepository repository;

    @Mock
    private R2dbcEntityTemplate template;

    @InjectMocks
    private UserRepositoryAdapter adapter;

    private static UserEntity entity() {
        return new UserEntity(1L, "test@example.com", "Test", "User", "https://example.com/avatar.png");
    }

    @Test
    void findByIdMapsEntityToUser() {
        when(repository.findById(1L)).thenReturn(Mono.just(entity()));

        StepVerifier.create(adapter.findById(1L))
                .expectNext(USER)
                .verifyComplete();
    }

    @Test
    void findAllMapsEveryEntity() {
        when(repository.findAll()).thenReturn(Flux.just(entity()));

        StepVerifier.create(adapter.findAll())
                .expectNext(USER)
                .verifyComplete();
    }

    @Test
    void findByNameSearchesByFirstName() {
        when(repository.findByFirstNameContainingIgnoreCase("tes")).thenReturn(Flux.just(entity()));

        StepVerifier.create(adapter.findByName("tes"))
                .expectNext(USER)
                .verifyComplete();
    }

    @Test
    void saveInsertsInsteadOfUpdating() {
        when(template.insert(any(UserEntity.class))).thenReturn(Mono.just(entity()));

        StepVerifier.create(adapter.save(USER))
                .expectNext(USER)
                .verifyComplete();

        verify(repository, never()).save(any());
    }

    @Test
    void saveReturnsStoredUserWhenInsertHitsDuplicateKey() {
        when(template.insert(any(UserEntity.class))).thenReturn(Mono.error(new DuplicateKeyException("duplicate key")));
        when(repository.findById(1L)).thenReturn(Mono.just(entity()));

        StepVerifier.create(adapter.save(USER))
                .expectNext(USER)
                .verifyComplete();
    }
}
