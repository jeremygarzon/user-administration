
package co.com.onboarding.users.r2dbc;

import co.com.onboarding.users.model.User;
import co.com.onboarding.users.model.gateways.UserRepositoryPort;
import co.com.onboarding.users.r2dbc.entity.UserEntity;
import co.com.onboarding.users.r2dbc.helper.ReactiveAdapterOperations;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MyReactiveRepositoryAdapter
        extends ReactiveAdapterOperations<
        User,
        UserEntity,
        Long,
        MyReactiveRepository>
        implements UserRepositoryPort {

    public MyReactiveRepositoryAdapter(
            MyReactiveRepository repository,
            ObjectMapper mapper) {
        super(repository, mapper, entity -> new User(
                entity.getId(),
                entity.getEmail(),
                entity.getFirstName(),
                entity.getLastName(),
                entity.getAvatar()
        ));
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
}
