package co.com.onboarding.users.model.exception;

import lombok.Getter;

@Getter
public class TechnicalException extends RuntimeException {

    private final TechnicalErrorMessage errorMessage;

    public TechnicalException(TechnicalErrorMessage errorMessage, Throwable cause) {
        super(errorMessage.getMessage(), cause);
        this.errorMessage = errorMessage;
    }
}
