package co.com.onboarding.users.api;

import co.com.onboarding.users.api.error.GlobalExceptionHandler;
import co.com.onboarding.users.api.validation.RequestValidator;
import co.com.onboarding.users.model.User;
import co.com.onboarding.users.model.UserCreation;
import co.com.onboarding.users.model.exception.BusinessException;
import co.com.onboarding.users.model.exception.TechnicalException;
import co.com.onboarding.users.usecase.CreateUserUseCase;
import co.com.onboarding.users.usecase.FindUsersUseCase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webflux.test.autoconfigure.WebFluxTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

import static co.com.onboarding.users.model.exception.BusinessErrorMessage.USER_NOT_FOUND;
import static co.com.onboarding.users.model.exception.TechnicalErrorMessage.USER_PROVIDER_UNAVAILABLE;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ContextConfiguration(classes = {RouterRest.class, Handler.class, GlobalExceptionHandler.class, RequestValidator.class})
@WebFluxTest
class RouterRestTest {

    private static final String USERS_PATH = "/api/v1/users";
    private static final String USER_PATH = USERS_PATH + "/{id}";
    private static final User USER = new User(
            1L, "george.bluth@reqres.in", "George", "Bluth", "https://reqres.in/img/faces/1-image.jpg");

    @Autowired
    private WebTestClient webTestClient;

    @MockitoBean
    private CreateUserUseCase createUserUseCase;

    @MockitoBean
    private FindUsersUseCase findUsersUseCase;

    @Test
    void createUserReturnsCreatedWithLocationWhenUserIsNew() {
        when(createUserUseCase.execute(1L)).thenReturn(Mono.just(new UserCreation(USER, true)));

        createUser("{\"id\":1}")
                .expectStatus().isCreated()
                .expectHeader().location("/api/v1/users/1")
                .expectBody(User.class).isEqualTo(USER);
    }

    @Test
    void createUserReturnsOkWhenUserAlreadyExists() {
        when(createUserUseCase.execute(1L)).thenReturn(Mono.just(new UserCreation(USER, false)));

        createUser("{\"id\":1}")
                .expectStatus().isOk()
                .expectHeader().doesNotExist("Location")
                .expectBody(User.class).isEqualTo(USER);
    }

    @Test
    void createUserReturnsBadRequestForNonPositiveId() {
        createUser("{\"id\":0}")
                .expectStatus().isBadRequest()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(400)
                .jsonPath("$.code").isEqualTo("INVALID_USER_ID")
                .jsonPath("$.detail").isEqualTo("User id must be a positive number")
                .jsonPath("$.instance").isEqualTo("/api/v1/users");

        verify(createUserUseCase, never()).execute(any());
    }

    @Test
    void createUserReturnsBadRequestWhenIdIsMissing() {
        createUser("{}")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_USER_ID");

        verify(createUserUseCase, never()).execute(any());
    }

    @Test
    void createUserReturnsBadRequestForMalformedBody() {
        createUser("{\"id\":\"abc\"}")
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_REQUEST_BODY");

        verify(createUserUseCase, never()).execute(any());
    }

    @Test
    void createUserReturnsBadRequestWithoutBody() {
        webTestClient.post()
                .uri(USERS_PATH)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_REQUEST_BODY");

        verify(createUserUseCase, never()).execute(any());
    }

    @Test
    void getUserReturnsBadRequestForNegativeId() {
        webTestClient.get()
                .uri(USER_PATH, -5)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("INVALID_USER_ID");

        verify(findUsersUseCase, never()).findById(any());
    }

    @Test
    void createUserReturnsNotFoundWhenProviderHasNoUser() {
        when(createUserUseCase.execute(23L)).thenReturn(Mono.error(new BusinessException(USER_NOT_FOUND)));

        createUser("{\"id\":23}")
                .expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.code").isEqualTo("USER_NOT_FOUND")
                .jsonPath("$.detail").isEqualTo("User does not exist")
                .jsonPath("$.instance").isEqualTo("/api/v1/users");
    }

    @Test
    void createUserReturnsBadGatewayWhenProviderFails() {
        when(createUserUseCase.execute(1L))
                .thenReturn(Mono.error(new TechnicalException(USER_PROVIDER_UNAVAILABLE, new RuntimeException())));

        createUser("{\"id\":1}")
                .expectStatus().isEqualTo(502)
                .expectBody()
                .jsonPath("$.code").isEqualTo("USER_PROVIDER_UNAVAILABLE")
                .jsonPath("$.detail").isEqualTo("User provider is not available, try again later");
    }

    @Test
    void createUserHidesInternalDetailsOnUnexpectedErrors() {
        when(createUserUseCase.execute(1L))
                .thenReturn(Mono.error(new IllegalStateException("connection refused to db-host:5432")));

        createUser("{\"id\":1}")
                .expectStatus().is5xxServerError()
                .expectBody()
                .jsonPath("$.status").isEqualTo(500)
                .jsonPath("$.code").isEqualTo("UNEXPECTED_ERROR")
                .jsonPath("$.detail").isEqualTo("Unexpected error processing the request");
    }

    @Test
    void unknownRouteReturnsProblemDetail() {
        webTestClient.get()
                .uri("/api/v1/unknown")
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.status").isEqualTo(404)
                .jsonPath("$.instance").isEqualTo("/api/v1/unknown");
    }

    @Test
    void getUserReturnsStoredUser() {
        when(findUsersUseCase.findById(1L)).thenReturn(Mono.just(USER));

        webTestClient.get()
                .uri(USER_PATH, 1)
                .exchange()
                .expectStatus().isOk()
                .expectBody(User.class).isEqualTo(USER);
    }

    @Test
    void getUserReturnsNotFoundMessageWhenUserDoesNotExist() {
        when(findUsersUseCase.findById(99L)).thenReturn(Mono.error(new BusinessException(USER_NOT_FOUND)));

        webTestClient.get()
                .uri(USER_PATH, 99)
                .exchange()
                .expectStatus().isNotFound()
                .expectBody()
                .jsonPath("$.detail").isEqualTo("User does not exist");
    }

    @Test
    void listUsersReturnsAllUsersWhenNoFilterIsGiven() {
        when(findUsersUseCase.findAll()).thenReturn(Flux.just(USER));

        webTestClient.get()
                .uri(USERS_PATH)
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(User.class).isEqualTo(List.of(USER));

        verify(findUsersUseCase, never()).findByName(any());
    }

    @Test
    void listUsersFiltersByName() {
        when(findUsersUseCase.findByName("geo")).thenReturn(Flux.just(USER));

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(USERS_PATH).queryParam("name", "geo").build())
                .exchange()
                .expectStatus().isOk()
                .expectBodyList(User.class).isEqualTo(List.of(USER));

        verify(findUsersUseCase, never()).findAll();
    }

    @Test
    void listUsersReturnsEmptyArrayWhenNothingMatches() {
        when(findUsersUseCase.findByName("nobody")).thenReturn(Flux.empty());

        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(USERS_PATH).queryParam("name", "nobody").build())
                .exchange()
                .expectStatus().isOk()
                .expectBody().json("[]");
    }

    @Test
    void listUsersReturnsBadRequestForBlankName() {
        webTestClient.get()
                .uri(uriBuilder -> uriBuilder.path(USERS_PATH).queryParam("name", " ").build())
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody()
                .jsonPath("$.code").isEqualTo("BLANK_NAME_FILTER")
                .jsonPath("$.detail").isEqualTo("Query parameter 'name' must not be blank");

        verify(findUsersUseCase, never()).findByName(any());
    }

    @Test
    void listUsersReturnsProblemDetailWhenDatabaseFails() {
        when(findUsersUseCase.findAll()).thenReturn(Flux.error(new IllegalStateException("db down")));

        webTestClient.get()
                .uri(USERS_PATH)
                .exchange()
                .expectStatus().is5xxServerError()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.detail").isEqualTo("Unexpected error processing the request");
    }

    private WebTestClient.ResponseSpec createUser(String body) {
        return webTestClient.post()
                .uri(USERS_PATH)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(body)
                .exchange();
    }
}
