package co.com.onboarding.users.api.request;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class RequestDtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void userIdRequestAcceptsPositiveIds() {
        assertThat(validator.validate(new UserIdRequest(1L))).isEmpty();
    }

    @Test
    void userIdRequestRejectsMissingZeroAndNegativeIds() {
        assertThat(messages(validator.validate(new UserIdRequest(null)))).containsExactly("INVALID_USER_ID");
        assertThat(messages(validator.validate(new UserIdRequest(0L)))).containsExactly("INVALID_USER_ID");
        assertThat(messages(validator.validate(new UserIdRequest(-1L)))).containsExactly("INVALID_USER_ID");
    }

    @Test
    void userSearchRequestAcceptsMissingOrNonBlankName() {
        assertThat(validator.validate(new UserSearchRequest(null))).isEmpty();
        assertThat(validator.validate(new UserSearchRequest(" geo "))).isEmpty();
    }

    @Test
    void userSearchRequestRejectsBlankName() {
        assertThat(messages(validator.validate(new UserSearchRequest("  ")))).containsExactly("BLANK_NAME_FILTER");
        assertThat(messages(validator.validate(new UserSearchRequest("")))).containsExactly("BLANK_NAME_FILTER");
    }

    private static <T> Set<String> messages(Set<ConstraintViolation<T>> violations) {
        return violations.stream().map(ConstraintViolation::getMessage).collect(java.util.stream.Collectors.toSet());
    }
}
