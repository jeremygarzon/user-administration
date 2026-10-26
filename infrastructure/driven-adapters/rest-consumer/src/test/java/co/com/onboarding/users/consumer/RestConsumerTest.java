package co.com.onboarding.users.consumer;

import co.com.onboarding.users.model.User;
import co.com.onboarding.users.model.exception.UserProviderException;
import io.github.resilience4j.circuitbreaker.CallNotPermittedException;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.test.StepVerifier;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class RestConsumerTest {

    private static final String REQRES_USER_BODY = """
            {
              "data": {
                "id": 1,
                "email": "george.bluth@reqres.in",
                "first_name": "George",
                "last_name": "Bluth",
                "avatar": "https://reqres.in/img/faces/1-image.jpg"
              }
            }
            """;

    private static MockWebServer mockBackEnd;

    private CircuitBreakerRegistry circuitBreakerRegistry;
    private RestConsumer restConsumer;

    @BeforeAll
    static void startServer() throws IOException {
        mockBackEnd = new MockWebServer();
        mockBackEnd.start();
    }

    @AfterAll
    static void tearDown() throws IOException {
        mockBackEnd.shutdown();
    }

    @BeforeEach
    void setUp() {
        var webClient = WebClient.builder().baseUrl(mockBackEnd.url("/").toString()).build();
        circuitBreakerRegistry = CircuitBreakerRegistry.ofDefaults();
        restConsumer = new RestConsumer(webClient, circuitBreakerRegistry);
    }

    @Test
    @DisplayName("findById maps the ReqRes response to the domain User.")
    void findByIdMapsResponseToUser() throws InterruptedException {
        enqueueResponse(HttpStatus.OK, REQRES_USER_BODY);

        StepVerifier.create(restConsumer.findById(1L))
                .expectNext(new User(
                        1L,
                        "george.bluth@reqres.in",
                        "George",
                        "Bluth",
                        "https://reqres.in/img/faces/1-image.jpg"))
                .verifyComplete();

        RecordedRequest request = mockBackEnd.takeRequest(1, TimeUnit.SECONDS);
        assertThat(request).isNotNull();
        assertThat(request.getPath()).isEqualTo("/api/users/1");
    }

    @Test
    @DisplayName("findById completes empty when ReqRes does not find the user.")
    void findByIdReturnsEmptyOnNotFound() {
        enqueueResponse(HttpStatus.NOT_FOUND, "{}");

        StepVerifier.create(restConsumer.findById(23L))
                .verifyComplete();

        assertThat(circuitBreakerRegistry.circuitBreaker("reqres").getMetrics().getNumberOfFailedCalls())
                .isZero();
    }

    @Test
    @DisplayName("findById wraps provider failures in UserProviderException keeping the cause.")
    void findByIdWrapsServerErrors() {
        enqueueResponse(HttpStatus.INTERNAL_SERVER_ERROR, "{}");

        StepVerifier.create(restConsumer.findById(1L))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(UserProviderException.class)
                        .hasCauseInstanceOf(WebClientResponseException.InternalServerError.class))
                .verify();
    }

    @Test
    @DisplayName("findById fails fast without calling ReqRes when the circuit is open.")
    void findByIdFailsFastWhenCircuitIsOpen() {
        circuitBreakerRegistry.circuitBreaker("reqres").transitionToOpenState();
        int requestsBefore = mockBackEnd.getRequestCount();

        StepVerifier.create(restConsumer.findById(1L))
                .expectErrorSatisfies(error -> assertThat(error)
                        .isInstanceOf(UserProviderException.class)
                        .hasCauseInstanceOf(CallNotPermittedException.class))
                .verify();

        assertThat(mockBackEnd.getRequestCount()).isEqualTo(requestsBefore);
    }

    private static void enqueueResponse(HttpStatus status, String body) {
        mockBackEnd.enqueue(new MockResponse()
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setResponseCode(status.value())
                .setBody(body));
    }
}
