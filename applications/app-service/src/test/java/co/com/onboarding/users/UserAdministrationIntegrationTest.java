package co.com.onboarding.users;

import okhttp3.mockwebserver.Dispatcher;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import org.testcontainers.utility.MountableFile;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbAsyncClient;
import software.amazon.awssdk.services.dynamodb.model.AttributeDefinition;
import software.amazon.awssdk.services.dynamodb.model.AttributeValue;
import software.amazon.awssdk.services.dynamodb.model.BillingMode;
import software.amazon.awssdk.services.dynamodb.model.KeySchemaElement;
import software.amazon.awssdk.services.dynamodb.model.KeyType;
import software.amazon.awssdk.services.dynamodb.model.ScalarAttributeType;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@DirtiesContext
class UserAdministrationIntegrationTest {

    private static final String QUEUE_NAME = "user-created-events";
    private static final String TABLE_NAME = "users";
    private static final Region REGION = Region.US_EAST_1;

    @Container
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer(
            DockerImageName.parse("docker.io/library/postgres:17-alpine").asCompatibleSubstituteFor("postgres"))
            .withCopyFileToContainer(MountableFile.forHostPath("../../deployment/postgres/init.sql"),
                    "/docker-entrypoint-initdb.d/init.sql");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>(
            DockerImageName.parse("docker.io/library/redis:7-alpine"))
            .withExposedPorts(6379);

    @Container
    static final GenericContainer<?> MINISTACK = new GenericContainer<>(
            DockerImageName.parse("docker.io/ministackorg/ministack:latest"))
            .withExposedPorts(4566)
            .waitingFor(Wait.forHttp("/_localstack/health").forStatusCode(200));

    private static final MockWebServer REQRES = startReqResMock();

    static {
        System.setProperty("aws.accessKeyId", "test");
        System.setProperty("aws.secretAccessKey", "test");
    }

    @Value("${local.server.port}")
    private int port;

    private WebTestClient webTestClient;

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("adapters.r2dbc.host", POSTGRES::getHost);
        registry.add("adapters.r2dbc.port", POSTGRES::getFirstMappedPort);
        registry.add("adapters.r2dbc.database", POSTGRES::getDatabaseName);
        registry.add("adapters.r2dbc.username", POSTGRES::getUsername);
        registry.add("adapters.r2dbc.password", POSTGRES::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("adapter.restconsumer.url", () -> REQRES.url("/").toString());
        registry.add("adapter.sqs.endpoint", UserAdministrationIntegrationTest::awsEndpoint);
        registry.add("adapter.sqs.queueUrl", UserAdministrationIntegrationTest::queueUrl);
        registry.add("entrypoint.sqs.endpoint", UserAdministrationIntegrationTest::awsEndpoint);
        registry.add("entrypoint.sqs.queueUrl", UserAdministrationIntegrationTest::queueUrl);
        registry.add("entrypoint.sqs.waitTimeSeconds", () -> 1);
        registry.add("aws.dynamodb.endpoint", UserAdministrationIntegrationTest::awsEndpoint);
    }

    @BeforeAll
    static void createAwsResources() {
        try (SqsAsyncClient sqs = SqsAsyncClient.builder().region(REGION).endpointOverride(URI.create(awsEndpoint())).build();
             DynamoDbAsyncClient dynamoDb = dynamoDbClient()) {
            sqs.createQueue(request -> request.queueName(QUEUE_NAME)).join();
            dynamoDb.createTable(request -> request.tableName(TABLE_NAME)
                    .attributeDefinitions(AttributeDefinition.builder()
                            .attributeName("id").attributeType(ScalarAttributeType.N).build())
                    .keySchema(KeySchemaElement.builder().attributeName("id").keyType(KeyType.HASH).build())
                    .billingMode(BillingMode.PAY_PER_REQUEST)).join();
        }
    }

    @AfterAll
    static void stopReqResMock() throws IOException {
        REQRES.shutdown();
    }

    @BeforeEach
    void setUp() {
        webTestClient = WebTestClient.bindToServer().baseUrl("http://localhost:" + port).build();
    }

    @Test
    void createsUserOnceAndStoresItInUppercaseInDynamoDb() {
        webTestClient.put().uri("/api/v1/users/1")
                .exchange()
                .expectStatus().isCreated()
                .expectHeader().value("Location", location -> assertThat(location).endsWith("/api/v1/users/1"))
                .expectBody()
                .jsonPath("$.firstName").isEqualTo("George");

        webTestClient.put().uri("/api/v1/users/1")
                .exchange()
                .expectStatus().isOk();

        try (DynamoDbAsyncClient dynamoDb = dynamoDbClient()) {
            await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
                Map<String, AttributeValue> item = dynamoDb.getItem(request -> request.tableName(TABLE_NAME)
                        .key(Map.of("id", AttributeValue.fromN("1")))).join().item();
                assertThat(item.get("firstName").s()).isEqualTo("GEORGE");
                assertThat(item.get("email").s()).isEqualTo("GEORGE.BLUTH@REQRES.IN");
            });
        }
    }

    @Test
    void findsUserByIdAndCachesIt() throws Exception {
        webTestClient.put().uri("/api/v1/users/2").exchange().expectStatus().isCreated();

        webTestClient.get().uri("/api/v1/users/2")
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.email").isEqualTo("janet.weaver@reqres.in");

        String cached = REDIS.execInContainer("redis-cli", "GET", "user:2").getStdout();
        assertThat(cached).contains("janet.weaver@reqres.in");

        webTestClient.get().uri(uriBuilder -> uriBuilder.path("/api/v1/users").queryParam("name", "JAN").build())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$[0].firstName").isEqualTo("Janet");
    }

    @Test
    void returnsNotFoundWhenProviderDoesNotHaveTheUser() {
        webTestClient.put().uri("/api/v1/users/23")
                .exchange()
                .expectStatus().isNotFound()
                .expectHeader().contentType(MediaType.APPLICATION_PROBLEM_JSON)
                .expectBody()
                .jsonPath("$.detail").isEqualTo("User with id 23 does not exist");
    }

    private static String awsEndpoint() {
        return "http://" + MINISTACK.getHost() + ":" + MINISTACK.getMappedPort(4566);
    }

    private static String queueUrl() {
        return awsEndpoint() + "/000000000000/" + QUEUE_NAME;
    }

    private static DynamoDbAsyncClient dynamoDbClient() {
        return DynamoDbAsyncClient.builder().region(REGION).endpointOverride(URI.create(awsEndpoint())).build();
    }

    private static MockWebServer startReqResMock() {
        MockWebServer server = new MockWebServer();
        server.setDispatcher(new Dispatcher() {
            @Override
            public MockResponse dispatch(RecordedRequest request) {
                return switch (String.valueOf(request.getPath())) {
                    case "/api/users/1" -> reqResUser(1, "george.bluth@reqres.in", "George", "Bluth");
                    case "/api/users/2" -> reqResUser(2, "janet.weaver@reqres.in", "Janet", "Weaver");
                    default -> new MockResponse().setResponseCode(404).setBody("{}");
                };
            }
        });
        try {
            server.start();
        } catch (IOException e) {
            throw new IllegalStateException("Could not start the ReqRes mock", e);
        }
        return server;
    }

    private static MockResponse reqResUser(int id, String email, String firstName, String lastName) {
        return new MockResponse()
                .setHeader("Content-Type", "application/json")
                .setBody("""
                        {"data": {"id": %d, "email": "%s", "first_name": "%s", "last_name": "%s",
                        "avatar": "https://reqres.in/img/faces/%d-image.jpg"}}
                        """.formatted(id, email, firstName, lastName, id));
    }
}
