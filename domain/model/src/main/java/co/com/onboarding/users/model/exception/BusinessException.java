package co.com.onboarding.users.model.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final BusinessErrorMessage errorMessage;

    public BusinessException(BusinessErrorMessage errorMessage) {
        super(errorMessage.getMessage());
        this.errorMessage = errorMessage;
    }
}
