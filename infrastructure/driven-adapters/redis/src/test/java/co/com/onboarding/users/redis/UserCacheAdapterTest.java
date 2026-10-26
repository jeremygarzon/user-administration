package co.com.onboarding.users.redis;

import co.com.onboarding.users.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import reactor.test.StepVerifier;

import java.time.Duration;

class UserCacheAdapterTest {

    private static final int UNREACHABLE_PORT = 1;

    private LettuceConnectionFactory connectionFactory;
    private UserCacheAdapter adapter;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory(new RedisStandaloneConfiguration("localhost", UNREACHABLE_PORT));
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();
        adapter = new UserCacheAdapter(connectionFactory, Duration.ofMinutes(10));
    }

    @AfterEach
    void tearDown() {
        connectionFactory.destroy();
    }

    @Test
    void findByIdBehavesAsCacheMissWhenRedisIsUnavailable() {
        StepVerifier.create(adapter.findById(1L))
                .verifyComplete();
    }

    @Test
    void saveCompletesWithoutErrorWhenRedisIsUnavailable() {
        User user = new User(1L, "george.bluth@reqres.in", "George", "Bluth", null);

        StepVerifier.create(adapter.save(user))
                .verifyComplete();
    }
}
