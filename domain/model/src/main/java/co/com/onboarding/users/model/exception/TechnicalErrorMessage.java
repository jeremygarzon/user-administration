package co.com.onboarding.users.model.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum TechnicalErrorMessage {

    USER_PROVIDER_UNAVAILABLE("User provider is not available, try again later");

    private final String message;
}
