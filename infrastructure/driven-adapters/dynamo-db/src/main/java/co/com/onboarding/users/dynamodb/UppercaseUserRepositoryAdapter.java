package co.com.onboarding.users.dynamodb;

import co.com.onboarding.users.model.User;
import co.com.onboarding.users.model.gateways.UppercaseUserRepositoryPort;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbAsyncTable;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;
import software.amazon.awssdk.enhanced.dynamodb.TableSchema;

@Repository
public class UppercaseUserRepositoryAdapter implements UppercaseUserRepositoryPort {

    private final DynamoDbAsyncTable<UserItem> table;

    public UppercaseUserRepositoryAdapter(
            DynamoDbEnhancedAsyncClient client,
            @Value("${aws.dynamodb.users-table}") String tableName) {
        this.table = client.table(tableName, TableSchema.fromBean(UserItem.class));
    }

    @Override
    public Mono<User> save(User user) {
        return Mono.fromFuture(() -> table.putItem(toItem(user)))
                .thenReturn(user);
    }

    private static UserItem toItem(User user) {
        return new UserItem(user.id(), user.email(), user.firstName(), user.lastName(), user.avatar());
    }
}
