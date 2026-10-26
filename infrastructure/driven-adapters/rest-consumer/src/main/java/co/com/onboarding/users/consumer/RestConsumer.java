
package co.com.onboarding.users.consumer;

import co.com.onboarding.users.model.User;
import co.com.onboarding.users.model.exception.UserProviderException;
import co.com.onboarding.users.model.gateways.UserProviderPort;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

@Service
public class RestConsumer implements UserProviderPort {

    private static final String CIRCUIT_BREAKER_NAME = "reqres";

    private final WebClient client;
    private final CircuitBreaker circuitBreaker;

    public RestConsumer(WebClient client, CircuitBreakerRegistry circuitBreakerRegistry) {
        this.client = client;
        this.circuitBreaker = circuitBreakerRegistry.circuitBreaker(CIRCUIT_BREAKER_NAME);
    }

    @Override
    public Mono<User> findById(Long id) {
        return client
                .get()
                .uri("/api/users/{id}", id)
                .retrieve()
                .bodyToMono(ReqResUserResponse.class)
                .map(ReqResUserResponse::data)
                .map(data -> new User(
                        data.id(),
                        data.email(),
                        data.firstName(),
                        data.lastName(),
                        data.avatar()
                ))
                .onErrorResume(WebClientResponseException.NotFound.class, e -> Mono.empty())
                .transformDeferred(CircuitBreakerOperator.of(circuitBreaker))
                .onErrorMap(e -> new UserProviderException("Could not fetch user " + id + " from provider", e));
    }
}
