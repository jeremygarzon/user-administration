package co.com.onboarding.users.api.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record CreateUserRequest(
        @NotNull(message = "INVALID_USER_ID")
        @Positive(message = "INVALID_USER_ID")
        Long id) {
}
