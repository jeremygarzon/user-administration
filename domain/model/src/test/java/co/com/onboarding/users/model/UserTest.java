package co.com.onboarding.users.model;

import org.junit.jupiter.api.Test;

import java.util.Locale;

import static org.assertj.core.api.Assertions.assertThat;

class UserTest {

    @Test
    void toUpperCaseKeepsIdAndAvatarUnchanged() {
        User user = new User(7L, "michael.lawson@reqres.in", "Michael", "Lawson", "https://reqres.in/img/faces/7-image.jpg");

        assertThat(user.toUpperCase())
                .isEqualTo(new User(7L, "MICHAEL.LAWSON@REQRES.IN", "MICHAEL", "LAWSON",
                        "https://reqres.in/img/faces/7-image.jpg"));
    }

    @Test
    void toUpperCaseToleratesMissingValues() {
        User user = new User(7L, null, "Michael", null, null);

        assertThat(user.toUpperCase()).isEqualTo(new User(7L, null, "MICHAEL", null, null));
    }

    @Test
    void toUpperCaseIsLocaleIndependent() {
        Locale defaultLocale = Locale.getDefault();
        Locale.setDefault(Locale.forLanguageTag("tr"));
        try {
            User user = new User(1L, "info@reqres.in", "ivan", "ilic", null);

            assertThat(user.toUpperCase().firstName()).isEqualTo("IVAN");
        } finally {
            Locale.setDefault(defaultLocale);
        }
    }
}
