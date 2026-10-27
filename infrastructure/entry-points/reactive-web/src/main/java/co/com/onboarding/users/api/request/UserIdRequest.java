package co.com.onboarding.users.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import org.springframework.web.reactive.function.server.ServerRequest;

public record UserIdRequest(
        @NotNull(message = "INVALID_USER_ID")
        @Positive(message = "INVALID_USER_ID")
        Long id) {

    private static final String ID_VARIABLE = "id";

    public static UserIdRequest from(ServerRequest request) {
        return new UserIdRequest(parseOrNull(request.pathVariable(ID_VARIABLE)));
    }

    private static Long parseOrNull(String value) {
        try {
            return Long.valueOf(value);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
