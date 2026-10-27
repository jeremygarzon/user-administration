package co.com.onboarding.users.model.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BusinessErrorMessage {

    USER_NOT_FOUND("User does not exist"),
    INVALID_USER_ID("User id must be a positive number"),
    INVALID_REQUEST_BODY("Request body is missing or malformed"),
    BLANK_NAME_FILTER("Query parameter 'name' must not be blank");

    private final String message;
}
