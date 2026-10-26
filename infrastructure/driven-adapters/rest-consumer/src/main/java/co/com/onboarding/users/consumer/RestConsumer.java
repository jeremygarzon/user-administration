
package co.com.onboarding.users.consumer;

import co.com.onboarding.users.model.User;
import co.com.onboarding.users.model.gateways.UserProviderPort;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Service
@RequiredArgsConstructor
public class RestConsumer implements UserProviderPort {

    private final WebClient client;

    @Override
    @CircuitBreaker(name = "reqres")
    public Mono<User> findById(Long id) {
        return client
                .get()
                .uri("/api/users/{id}", id)
                .retrieve()
                .bodyToMono(ReqResUserResponse.class)
                .map(response -> response.data())
                .map(data -> new User(
                        data.id(),
                        data.email(),
                        data.firstName(),
                        data.lastName(),
                        data.avatar()
                ));
    }
}
