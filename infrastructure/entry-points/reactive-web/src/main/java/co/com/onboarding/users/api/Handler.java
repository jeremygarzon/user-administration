package co.com.onboarding.users.api;

import co.com.onboarding.users.api.request.CreateUserRequest;
import co.com.onboarding.users.api.request.UserIdRequest;
import co.com.onboarding.users.api.request.UserSearchRequest;
import co.com.onboarding.users.api.validation.RequestValidator;
import co.com.onboarding.users.model.User;
import co.com.onboarding.users.model.UserCreation;
import co.com.onboarding.users.model.exception.BusinessException;
import co.com.onboarding.users.usecase.CreateUserUseCase;
import co.com.onboarding.users.usecase.FindUsersUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import static co.com.onboarding.users.model.exception.BusinessErrorMessage.INVALID_REQUEST_BODY;

@Component
@RequiredArgsConstructor
public class Handler {

    private final CreateUserUseCase createUserUseCase;
    private final FindUsersUseCase findUsersUseCase;
    private final RequestValidator requestValidator;

    public Mono<ServerResponse> createUser(ServerRequest request) {
        return request.bodyToMono(CreateUserRequest.class)
                .onErrorMap(ServerWebInputException.class, e -> new BusinessException(INVALID_REQUEST_BODY))
                .switchIfEmpty(Mono.error(() -> new BusinessException(INVALID_REQUEST_BODY)))
                .flatMap(requestValidator::validate)
                .map(CreateUserRequest::id)
                .flatMap(createUserUseCase::execute)
                .flatMap(creation -> toResponse(request, creation));
    }

    public Mono<ServerResponse> getUser(ServerRequest request) {
        return userId(request)
                .flatMap(findUsersUseCase::findById)
                .flatMap(user -> ServerResponse.ok().bodyValue(user));
    }

    public Mono<ServerResponse> listUsers(ServerRequest request) {
        return requestValidator.validate(UserSearchRequest.from(request))
                .flatMapMany(this::findUsers)
                .collectList()
                .flatMap(users -> ServerResponse.ok().bodyValue(users));
    }

    private Flux<User> findUsers(UserSearchRequest search) {
        return search.hasName()
                ? findUsersUseCase.findByName(search.name())
                : findUsersUseCase.findAll();
    }

    private Mono<Long> userId(ServerRequest request) {
        return requestValidator.validate(UserIdRequest.from(request))
                .map(UserIdRequest::id);
    }

    private static Mono<ServerResponse> toResponse(ServerRequest request, UserCreation creation) {
        ServerResponse.BodyBuilder response = creation.created()
                ? ServerResponse.created(request.uriBuilder().path("/{id}").build(creation.user().id()))
                : ServerResponse.ok();
        return response.bodyValue(creation.user());
    }
}
