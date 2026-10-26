package co.com.onboarding.users.redis.template;

import co.com.onboarding.users.model.User;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.reactivecommons.utils.ObjectMapperImp;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import reactor.test.StepVerifier;

import java.time.Duration;

class ReactiveRedisTemplateAdapterTest {

    private static final int UNREACHABLE_PORT = 1;

    private LettuceConnectionFactory connectionFactory;
    private ReactiveRedisTemplateAdapter adapter;

    @BeforeEach
    void setUp() {
        connectionFactory = new LettuceConnectionFactory(new RedisStandaloneConfiguration("localhost", UNREACHABLE_PORT));
        connectionFactory.afterPropertiesSet();
        connectionFactory.start();
        adapter = new ReactiveRedisTemplateAdapter(connectionFactory, new ObjectMapperImp(), Duration.ofMinutes(10));
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
