package co.com.onboarding.users.redis.template;

import co.com.onboarding.users.model.User;
import co.com.onboarding.users.model.gateways.UserCachePort;
import co.com.onboarding.users.redis.template.helper.ReactiveTemplateAdapterOperations;
import lombok.extern.slf4j.Slf4j;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
public class ReactiveRedisTemplateAdapter extends ReactiveTemplateAdapterOperations<User, String, User>
        implements UserCachePort {

    private static final String KEY_PREFIX = "user:";

    private final Duration ttl;

    public ReactiveRedisTemplateAdapter(
            ReactiveRedisConnectionFactory connectionFactory,
            ObjectMapper mapper,
            @Value("${adapters.redis.user-ttl}") Duration ttl) {
        super(connectionFactory, mapper, user -> user);
        this.ttl = ttl;
    }

    @Override
    public Mono<User> findById(Long id) {
        return super.findById(KEY_PREFIX + id)
                .onErrorResume(error -> {
                    log.warn("Cache lookup failed for user {}, falling back to database: {}", id, error.toString());
                    return Mono.empty();
                });
    }

    @Override
    public Mono<Void> save(User user) {
        return save(KEY_PREFIX + user.id(), user, ttl.toMillis())
                .then()
                .onErrorResume(error -> {
                    log.warn("Could not cache user {}: {}", user.id(), error.toString());
                    return Mono.empty();
                });
    }

    @Override
    protected User toValue(User user) {
        return user;
    }
}
