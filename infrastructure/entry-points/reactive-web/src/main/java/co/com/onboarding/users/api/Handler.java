package co.com.onboarding.users.api;

import co.com.onboarding.users.model.UserCreation;
import co.com.onboarding.users.usecase.CreateUserUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
public class Handler {

    private final CreateUserUseCase createUserUseCase;

    public Mono<ServerResponse> createUser(ServerRequest request) {
        return userId(request)
                .flatMap(createUserUseCase::execute)
                .flatMap(creation -> toResponse(request, creation));
    }

    private static Mono<ServerResponse> toResponse(ServerRequest request, UserCreation creation) {
        ServerResponse.BodyBuilder response = creation.created()
                ? ServerResponse.created(request.uri())
                : ServerResponse.ok();
        return response.bodyValue(creation.user());
    }

    private static Mono<Long> userId(ServerRequest request) {
        return Mono.fromCallable(() -> Long.parseLong(request.pathVariable("id")))
                .filter(id -> id > 0)
                .onErrorResume(NumberFormatException.class, e -> Mono.empty())
                .switchIfEmpty(Mono.error(() -> new ServerWebInputException("User id must be a positive number")));
    }
}
