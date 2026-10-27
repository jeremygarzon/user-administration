package co.com.onboarding.users.api.error;

import co.com.onboarding.users.model.exception.BusinessErrorMessage;
import co.com.onboarding.users.model.exception.BusinessException;
import co.com.onboarding.users.model.exception.TechnicalErrorMessage;
import co.com.onboarding.users.model.exception.TechnicalException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.webflux.autoconfigure.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.webflux.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponse;
import org.springframework.web.reactive.function.server.RouterFunction;
import org.springframework.web.reactive.function.server.RouterFunctions;
import org.springframework.web.reactive.function.server.ServerRequest;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.net.URI;

import static net.logstash.logback.argument.StructuredArguments.kv;
import static org.springframework.web.reactive.function.server.RequestPredicates.all;

@Slf4j
@Component
@Order(-2)
public class GlobalExceptionHandler extends AbstractErrorWebExceptionHandler {

    private static final String CODE_PROPERTY = "code";
    private static final String UNEXPECTED_ERROR_CODE = "UNEXPECTED_ERROR";
    private static final String UNEXPECTED_ERROR_MESSAGE = "Unexpected error processing the request";
    private static final String REQUEST_REJECTED = "Request rejected";
    private static final String REQUEST_FAILED = "Request failed";

    public GlobalExceptionHandler(ErrorAttributes errorAttributes,
                                  ApplicationContext applicationContext,
                                  ServerCodecConfigurer serverCodecConfigurer) {
        super(errorAttributes, new WebProperties.Resources(), applicationContext);
        setMessageWriters(serverCodecConfigurer.getWriters());
        setMessageReaders(serverCodecConfigurer.getReaders());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(all(), this::renderError);
    }

    private Mono<ServerResponse> renderError(ServerRequest request) {
        Throwable error = getError(request);
        ProblemDetail problem = toProblem(error);
        problem.setInstance(URI.create(request.path()));
        logError(request, problem, error);
        return ServerResponse.status(problem.getStatus())
                .contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .bodyValue(problem);
    }

    private static ProblemDetail toProblem(Throwable error) {
        if (error instanceof BusinessException exception) {
            return businessProblem(exception.getErrorMessage());
        }
        if (error instanceof ConstraintViolationException exception) {
            return businessProblem(firstViolation(exception));
        }
        if (error instanceof TechnicalException exception) {
            TechnicalErrorMessage errorMessage = exception.getErrorMessage();
            return problem(statusOf(errorMessage), errorMessage.name(), errorMessage.getMessage());
        }
        if (error instanceof ErrorResponse response) {
            return response.getBody();
        }
        return problem(HttpStatus.INTERNAL_SERVER_ERROR, UNEXPECTED_ERROR_CODE, UNEXPECTED_ERROR_MESSAGE);
    }

    private static ProblemDetail businessProblem(BusinessErrorMessage errorMessage) {
        return problem(statusOf(errorMessage), errorMessage.name(), errorMessage.getMessage());
    }

    private static BusinessErrorMessage firstViolation(ConstraintViolationException exception) {
        String code = exception.getConstraintViolations().iterator().next().getMessage();
        return BusinessErrorMessage.valueOf(code);
    }

    private static HttpStatus statusOf(BusinessErrorMessage errorMessage) {
        return switch (errorMessage) {
            case USER_NOT_FOUND -> HttpStatus.NOT_FOUND;
            case INVALID_USER_ID, INVALID_REQUEST_BODY, BLANK_NAME_FILTER -> HttpStatus.BAD_REQUEST;
        };
    }

    private static HttpStatus statusOf(TechnicalErrorMessage errorMessage) {
        return switch (errorMessage) {
            case USER_PROVIDER_UNAVAILABLE -> HttpStatus.BAD_GATEWAY;
        };
    }

    private static ProblemDetail problem(HttpStatus status, String code, String message) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, message);
        problem.setProperty(CODE_PROPERTY, code);
        return problem;
    }

    private static void logError(ServerRequest request, ProblemDetail problem, Throwable error) {
        if (problem.getStatus() >= HttpStatus.INTERNAL_SERVER_ERROR.value()) {
            log.error(REQUEST_FAILED, kv("method", request.method().name()), kv("path", request.path()),
                    kv("status", problem.getStatus()), error);
        } else {
            log.warn(REQUEST_REJECTED, kv("method", request.method().name()), kv("path", request.path()),
                    kv("status", problem.getStatus()), kv("detail", problem.getDetail()));
        }
    }
}
