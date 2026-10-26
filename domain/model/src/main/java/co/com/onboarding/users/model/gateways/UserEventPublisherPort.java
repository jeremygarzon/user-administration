package co.com.onboarding.users.model.gateways;

import co.com.onboarding.users.model.User;
import reactor.core.publisher.Mono;

public interface UserEventPublisherPort {

    Mono<Void> publishUserCreated(User user);
}
