package co.com.onboarding.users.model;

import java.util.Locale;

public record User(
        Long id,
        String email,
        String firstName,
        String lastName,
        String avatar
) {

    public User toUpperCase() {
        return new User(id, upper(email), upper(firstName), upper(lastName), avatar);
    }

    private static String upper(String value) {
        return value == null ? null : value.toUpperCase(Locale.ROOT);
    }
}
