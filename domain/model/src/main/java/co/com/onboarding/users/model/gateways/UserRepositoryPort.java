package co.com.onboarding.users.model.gateways;

import co.com.onboarding.users.model.User;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface UserRepositoryPort {

    Mono<User> findById(Long id);

    Mono<User> save(User user);

    Flux<User> findAll();
}
