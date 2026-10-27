package co.com.onboarding.users.redis;

import co.com.onboarding.users.model.User;
import co.com.onboarding.users.model.gateways.UserCachePort;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.serializer.JacksonJsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@Component
public class UserCacheAdapter implements UserCachePort {

    private static final String KEY_PREFIX = "user:";
    private static final String CACHE_READ_FAILED = "Cache lookup failed, falling back to database";
    private static final String CACHE_WRITE_FAILED = "Could not cache user";

    private final ReactiveRedisTemplate<String, User> template;
    private final Duration ttl;

    public UserCacheAdapter(
            ReactiveRedisConnectionFactory connectionFactory,
            @Value("${adapters.redis.user-ttl}") Duration ttl) {
        RedisSerializationContext<String, User> serialization = RedisSerializationContext
                .<String, User>newSerializationContext(RedisSerializer.string())
                .value(new JacksonJsonRedisSerializer<>(User.class))
                .build();
        this.template = new ReactiveRedisTemplate<>(connectionFactory, serialization);
        this.ttl = ttl;
    }

    @Override
    public Mono<User> findById(Long id) {
        return template.opsForValue().get(KEY_PREFIX + id)
                .onErrorResume(error -> {
                    log.warn(CACHE_READ_FAILED, kv("userId", id), kv("error", error.toString()));
                    return Mono.empty();
                });
    }

    @Override
    public Mono<Void> save(User user) {
        return template.opsForValue().set(KEY_PREFIX + user.id(), user, ttl)
                .then()
                .onErrorResume(error -> {
                    log.warn(CACHE_WRITE_FAILED, kv("userId", user.id()), kv("error", error.toString()));
                    return Mono.empty();
                });
    }
}
