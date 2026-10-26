
package co.com.onboarding.users.r2dbc;

import co.com.onboarding.users.model.User;
import co.com.onboarding.users.model.gateways.UserRepositoryPort;
import co.com.onboarding.users.r2dbc.entity.UserEntity;
import co.com.onboarding.users.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Repository
public class MyReactiveRepositoryAdapter
        extends ReactiveAdapterOperations<
        User,
        UserEntity,
        Long,
        MyReactiveRepository>
        implements UserRepositoryPort {

    private final R2dbcEntityTemplate template;

    public MyReactiveRepositoryAdapter(
            MyReactiveRepository repository,
            ObjectMapper mapper,
            R2dbcEntityTemplate template) {
        super(repository, mapper, entity -> new User(
                entity.getId(),
                entity.getEmail(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getAvatar()
        ));
        this.template = template;
    }

    @Override
    public Flux<User> findByName(String name) {
        return repository.findByFirstNameContainingIgnoreCase(name)
                .map(this::toEntity);
    }

    @Override
    protected UserEntity toData(User user) {
        return new UserEntity(
                user.id(),
                user.email(),
                user.firstName(),
                user.lastName(),
                user.avatar()
        );
    }

    @Override
    protected Mono<UserEntity> saveData(UserEntity data) {
        return template.insert(data)
                .onErrorResume(DuplicateKeyException.class, e -> repository.findById(data.getId()));
    }
}
