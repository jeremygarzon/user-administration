package co.com.onboarding.users.consumer.config;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

class RestConsumerConfigTest {

    private MockWebServer server;

    @BeforeEach
    void setUp() throws IOException {
        server = new MockWebServer();
        server.start();
        server.enqueue(new MockResponse().setBody("{}"));
    }

    @AfterEach
    void tearDown() throws IOException {
        server.shutdown();
    }

    @Test
    void sendsApiKeyHeaderWhenConfigured() throws InterruptedException {
        RecordedRequest request = get(new RestConsumerConfig(server.url("/").toString(), 1000, "secret-key"));

        assertThat(request.getHeader("x-api-key")).isEqualTo("secret-key");
        assertThat(request.getHeader(HttpHeaders.CONTENT_TYPE)).isNull();
    }

    @Test
    void omitsApiKeyHeaderWhenNotConfigured() throws InterruptedException {
        RecordedRequest request = get(new RestConsumerConfig(server.url("/").toString(), 1000, ""));

        assertThat(request.getHeader("x-api-key")).isNull();
    }

    private RecordedRequest get(RestConsumerConfig config) throws InterruptedException {
        WebClient client = config.getWebClient(WebClient.builder());
        client.get().uri("/api/users/1").retrieve().toBodilessEntity().block();
        return server.takeRequest(1, TimeUnit.SECONDS);
    }
}
