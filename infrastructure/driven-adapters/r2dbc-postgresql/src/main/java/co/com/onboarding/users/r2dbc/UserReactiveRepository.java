package co.com.onboarding.users.r2dbc;

import co.com.onboarding.users.r2dbc.entity.UserEntity;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import reactor.core.publisher.Flux;

public interface UserReactiveRepository extends ReactiveCrudRepository<UserEntity, Long> {

    Flux<UserEntity> findByFirstNameContainingIgnoreCase(String firstName);
}
