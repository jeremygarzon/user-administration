package co.com.onboarding.users.api;

import co.com.onboarding.users.api.error.ApiErrorHandler;
import co.com.onboarding.users.model.User;
import co.com.onboarding.users.model.UserCreation;
import co.com.onboarding.users.model.exception.UserNotFoundException;
import co.com.onboarding.users.model.exception.UserProviderException;
import co.com.onboarding.users.usecase.CreateUserUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Mono;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {RouterRest.class, Handler.class, ApiErrorHandler.class})
@WebFluxTest
class RouterRestTest {

    private static final String USER_PATH = "/api/v1/users/{id}";
    private static final User USER = new User(
            1L, "george.bluth@reqres.in", "George", "Bluth", "https://reqres.in/img/faces/1-image.jpg");

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CreateUserUseCase createUserUseCase;

    @Test
    void createUserReturnsCreatedWithLocationWhenUserIsNew() {
        when(createUserUseCase.execute(1L)).thenReturn(Mono.just(new UserCreation(USER, true)));

        webTestClient.put()
                .uri(USER_PATH, 1)
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().location("/api/v1/users/1")
                .expectBody(User.class).isEqualTo(USER);
    }

    @Test
    void createUserReturnsOkWhenUserAlreadyExists() {
        when(createUserUseCase.execute(1L)).thenReturn(Mono.just(new UserCreation(USER, false)));

        webTestClient.put()
                .uri(USER_PATH, 1)
                .exchange()
                .expectStatus().isOk()
                .expectHeader().doesNotExist("Location")
                .expectBody(User.class).isEqualTo(USER);
    }

    @Test
    void createUserReturnsBadRequestForNonNumericId() {
        webTestClient.put()
                .uri(USER_PATH, "abc")
                .exchange()
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.detail").isEqualTo("User id must be a positive number")
                .jsonPath("$.instance").isEqualTo("/api/v1/users/abc");

        verify(createUserUseCase, never()).execute(any());
    }

    @Test
    void createUserReturnsBadRequestForNonPositiveId() {
        webTestClient.put()
                .uri(USER_PATH, 0)
                .exchange()
                .expectStatus().isBadRequest();

        verify(createUserUseCase, never()).execute(any());
    }

    @Test
    void createUserReturnsNotFoundWhenProviderHasNoUser() {
        when(createUserUseCase.execute(23L)).thenReturn(Mono.error(new UserNotFoundException(23L)));

        webTestClient.put()
                .uri(USER_PATH, 23)
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.detail").isEqualTo("User with id 23 does not exist");
    }

    @Test
    void createUserReturnsBadGatewayWhenProviderFails() {
        when(createUserUseCase.execute(1L))
                .thenReturn(Mono.error(new UserProviderException("timeout", new RuntimeException())));

        webTestClient.put()
                .uri(USER_PATH, 1)
                .exchange()
                .expectStatus().isEqualTo(502)
                .expectBody()
                .jsonPath("$.detail").isEqualTo("User provider is not available, try again later");
    }

    @Test
    void createUserHidesInternalDetailsOnUnexpectedErrors() {
        when(createUserUseCase.execute(1L))
                .thenReturn(Mono.error(new IllegalStateException("connection refused to db-host:5432")));

        webTestClient.put()
                .uri(USER_PATH, 1)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.status").isEqualTo(500)
                .jsonPath("$.detail").isEqualTo("Unexpected error processing the request");
    }
}
