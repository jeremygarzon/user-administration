package co.com.onboarding.users.model.gateways;

import co.com.onboarding.users.model.User;
import reactor.core.publisher.Mono;

public interface UserCachePort {

    Mono<User> findById(Long id);

    Mono<Void> save(User user);
}
