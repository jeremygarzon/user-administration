package co.com.onboarding.users.api.request;

import jakarta.validation.constraints.Pattern;
import org.springframework.web.reactive.function.server.ServerRequest;

public record UserSearchRequest(

        @Pattern(regexp = ".*\\S.*", message = "BLANK_NAME_FILTER")
        String name) {

    private static final String NAME_PARAM = "name";

    public static UserSearchRequest from(ServerRequest request) {
        return new UserSearchRequest(request.queryParam(NAME_PARAM).orElse(null));
    }

    public boolean hasName() {
        return name != null;
    }
}
