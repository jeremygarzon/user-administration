package co.com.onboarding.users.model.gateways;

import co.com.onboarding.users.model.User;
import reactor.core.publisher.Mono;

public interface UppercaseUserRepositoryPort {

    Mono<User> save(User user);
}
