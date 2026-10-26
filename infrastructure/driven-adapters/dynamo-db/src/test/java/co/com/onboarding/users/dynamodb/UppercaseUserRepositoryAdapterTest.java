package co.com.onboarding.users.dynamodb;

import co.com.onboarding.users.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.reactivecommons.utils.ObjectMapper;
import reactor.test.StepVerifier;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;
import software.amazon.awssdk.services.dynamodb.model.ResourceNotFoundException;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DynamoDBTemplateAdapterTest {

    private static final User USER = new User(1L, "GEORGE.BLUTH@REQRES.IN", "GEORGE", "BLUTH", "https://reqres.in/img/faces/1-image.jpg");

    @Mock
    private DynamoDbEnhancedAsyncClient client;

    @Mock
    private ObjectMapper mapper;

    @Mock
    private DynamoDbAsyncTable<UserItem> table;

    private DynamoDBTemplateAdapter adapter;

    @BeforeEach
    void setUp() {
        when(client.table(eq("users"), any(TableSchema.class))).thenReturn(table);
        adapter = new DynamoDBTemplateAdapter(client, mapper, "users");
    }

    @Test
    void saveStoresEveryUserAttribute() {
        when(table.putItem(any(UserItem.class))).thenReturn(CompletableFuture.completedFuture(null));

        StepVerifier.create(adapter.save(USER))
                .expectNext(USER)
                .verifyComplete();

        ArgumentCaptor<UserItem> item = ArgumentCaptor.forClass(UserItem.class);
        verify(table).putItem(item.capture());
        assertThat(item.getValue())
                .usingRecursiveComparison()
                .isEqualTo(new UserItem(1L, "GEORGE.BLUTH@REQRES.IN", "GEORGE", "BLUTH",
                        "https://reqres.in/img/faces/1-image.jpg"));
    }

    @Test
    void savePropagatesStorageErrors() {
        when(table.putItem(any(UserItem.class)))
                .thenReturn(CompletableFuture.failedFuture(ResourceNotFoundException.builder().message("no table").build()));

        StepVerifier.create(adapter.save(USER))
                .expectError(ResourceNotFoundException.class)
                .verify();
    }
}
