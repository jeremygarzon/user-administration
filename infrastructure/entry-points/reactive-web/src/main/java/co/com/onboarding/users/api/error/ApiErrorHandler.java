package co.com.onboarding.users.api.error;

import co.com.onboarding.users.model.exception.UserNotFoundException;
import co.com.onboarding.users.model.exception.UserProviderException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.HandlerFilterFunction;
import org.springframework.web.reactive.function.server.HandlerFunction;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.net.URI;

@Slf4j
@Component
public class ApiErrorHandler implements HandlerFilterFunction<ServerResponse, ServerResponse> {

    @Override
    public Mono<ServerResponse> filter(ServerRequest request, HandlerFunction<ServerResponse> next) {
        return next.handle(request)
                .onErrorResume(error -> toErrorResponse(request, error));
    }

    private static Mono<ServerResponse> toErrorResponse(ServerRequest request, Throwable error) {
        ProblemDetail problem = toProblem(error);
        problem.setInstance(URI.create(request.path()));
        logError(request, problem, error);
        return ServerResponse.status(problem.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyValue(problem);
    }

    private static ProblemDetail toProblem(Throwable error) {
        if (error instanceof UserNotFoundException) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, error.getMessage());
        }
        if (error instanceof ResponseStatusException statusException) {
            return statusException.getBody();
        }
        if (error instanceof UserProviderException) {
            return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_GATEWAY,
                    "User provider is not available, try again later");
        }
        return ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR,
                "Unexpected error processing the request");
    }

    private static void logError(ServerRequest request, ProblemDetail problem, Throwable error) {
        if (problem.getStatus() >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            log.error("{} {} failed with status {}", request.method(), request.path(), problem.getStatus(), error);
        } else {
            log.warn("{} {} rejected with status {}: {}",
                    request.method(), request.path(), problem.getStatus(), problem.getDetail());
        }
    }
}
