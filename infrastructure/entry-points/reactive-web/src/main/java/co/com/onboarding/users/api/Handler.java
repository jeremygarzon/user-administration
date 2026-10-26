package co.com.onboarding.users.api;

import co.com.onboarding.users.model.User;
import co.com.onboarding.users.model.UserCreation;
import co.com.onboarding.users.usecase.CreateUserUseCase;
import co.com.onboarding.users.usecase.FindUsersUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Optional;

@Component
@RequiredArgsConstructor
public class Handler {

    private final CreateUserUseCase createUserUseCase;
    private final FindUsersUseCase findUsersUseCase;

    public Mono<ServerResponse> createUser(ServerRequest request) {
        return userId(request)
                .flatMap(createUserUseCase::execute)
                .flatMap(creation -> toResponse(request, creation));
    }

    public Mono<ServerResponse> getUser(ServerRequest request) {
        return userId(request)
                .flatMap(findUsersUseCase::findById)
                .flatMap(user -> ServerResponse.ok().bodyValue(user));
    }

    public Mono<ServerResponse> listUsers(ServerRequest request) {
        Optional<String> name = request.queryParam("name");
        if (name.isPresent() && name.get().isBlank()) {
            return Mono.error(new ServerWebInputException("Query parameter 'name' must not be blank"));
        }
        Flux<User> users = name
                .map(findUsersUseCase::findByName)
                .orElseGet(findUsersUseCase::findAll);
        return users.collectList()
                .flatMap(list -> ServerResponse.ok().bodyValue(list));
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
