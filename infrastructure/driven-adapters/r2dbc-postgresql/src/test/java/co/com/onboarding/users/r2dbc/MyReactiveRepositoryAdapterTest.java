
package co.com.onboarding.users.r2dbc;

import co.com.onboarding.users.model.User;
import co.com.onboarding.users.r2dbc.entity.UserEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.domain.Example;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MyReactiveRepositoryAdapterTest {

    @InjectMocks
    MyReactiveRepositoryAdapter repositoryAdapter;

    @Mock
    MyReactiveRepository repository;

    @Mock
    ObjectMapper mapper;

    @Mock
    R2dbcEntityTemplate template;

    private UserEntity createUserEntity() {
        return new UserEntity(
                1L,
                "test@example.com",
                "Test",
                "User",
                "https://example.com/avatar.png"
        );
    }

    private User createUser() {
        return new User(
                1L,
                "test@example.com",
                "Test",
                "User",
                "https://example.com/avatar.png"
        );
    }

    @Test
    void mustFindValueById() {
        UserEntity entity = createUserEntity();

        when(repository.findById(1L)).thenReturn(Mono.just(entity));

        Mono<User> result = repositoryAdapter.findById(1L);

        StepVerifier.create(result)
                .expectNext(createUser())
                .verifyComplete();
    }

    @Test
    void mustFindAllValues() {
        UserEntity entity = createUserEntity();

        when(repository.findAll()).thenReturn(Flux.just(entity));

        Flux<User> result = repositoryAdapter.findAll();

        StepVerifier.create(result)
                .expectNext(createUser())
                .verifyComplete();
    }

    @Test
    void mustFindByName() {
        when(repository.findByFirstNameContainingIgnoreCase("geo"))
                .thenReturn(Flux.just(createUserEntity()));

        StepVerifier.create(repositoryAdapter.findByName("geo"))
                .expectNext(createUser())
                .verifyComplete();
    }

    @Test
    void mustFindByExample() {
        UserEntity entity = createUserEntity();
        User user = createUser();

        when(repository.findAll(any(Example.class)))
                .thenReturn(Flux.just(entity));

        Flux<User> result = repositoryAdapter.findByExample(user);

        StepVerifier.create(result)
                .expectNext(createUser())
                .verifyComplete();
    }

    @Test
    void mustInsertValue() {
        User user = createUser();
        UserEntity entity = createUserEntity();

        when(template.insert(any(UserEntity.class)))
                .thenReturn(Mono.just(entity));

        Mono<User> result = repositoryAdapter.save(user);

        StepVerifier.create(result)
                .expectNext(createUser())
                .verifyComplete();

        verify(repository, never()).save(any());
    }

    @Test
    void mustReturnStoredValueWhenInsertHitsDuplicateKey() {
        User user = createUser();
        UserEntity entity = createUserEntity();

        when(template.insert(any(UserEntity.class)))
                .thenReturn(Mono.error(new DuplicateKeyException("duplicate key")));
        when(repository.findById(1L)).thenReturn(Mono.just(entity));

        StepVerifier.create(repositoryAdapter.save(user))
                .expectNext(createUser())
                .verifyComplete();
    }
}
