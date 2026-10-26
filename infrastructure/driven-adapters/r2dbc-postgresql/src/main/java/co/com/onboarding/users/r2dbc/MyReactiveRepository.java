
package co.com.onboarding.users.r2dbc;

import co.com.onboarding.users.r2dbc.entity.UserEntity;
import org.springframework.data.repository.query.ReactiveQueryByExampleExecutor;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface MyReactiveRepository
        extends ReactiveCrudRepository<UserEntity, Long>,
        ReactiveQueryByExampleExecutor<UserEntity> {

    Flux<UserEntity> findByFirstNameContainingIgnoreCase(String firstName);
}