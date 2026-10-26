package co.com.onboarding.users.r2dbc;

import co.com.onboarding.users.model.User;
import co.com.onboarding.users.model.gateways.UserRepositoryPort;
import co.com.onboarding.users.r2dbc.entity.UserEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
@RequiredArgsConstructor
public class UserRepositoryAdapter implements UserRepositoryPort {

    private final UserReactiveRepository repository;
    private final R2dbcEntityTemplate template;

    @Override
    public Mono<User> findById(Long id) {
        return repository.findById(id)
                .map(UserRepositoryAdapter::toUser);
    }

    @Override
    public Flux<User> findAll() {
        return repository.findAll()
                .map(UserRepositoryAdapter::toUser);
    }

    @Override
    public Flux<User> findByName(String name) {
        return repository.findByFirstNameContainingIgnoreCase(name)
                .map(UserRepositoryAdapter::toUser);
    }

    @Override
    public Mono<User> save(User user) {
        return template.insert(toEntity(user))
                .onErrorResume(DuplicateKeyException.class, e -> repository.findById(user.id()))
                .map(UserRepositoryAdapter::toUser);
    }

    private static UserEntity toEntity(User user) {
        return new UserEntity(user.id(), user.email(), user.firstName(), user.lastName(), user.avatar());
    }

    private static User toUser(UserEntity entity) {
        return new User(entity.getId(), entity.getEmail(), entity.getFirstName(), entity.getLastName(), entity.getAvatar());
    }
}
