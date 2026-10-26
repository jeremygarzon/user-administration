package co.com.onboarding.users.model;

public record User(
        Long id,
        String email,
        String firstName,
        String lastName,
        String avatar
) {
}
