package co.com.onboarding.users.dynamodb;

import co.com.onboarding.users.dynamodb.helper.TemplateAdapterOperations;
import co.com.onboarding.users.model.User;
import co.com.onboarding.users.model.gateways.UppercaseUserRepositoryPort;
import org.reactivecommons.utils.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;
import software.amazon.awssdk.enhanced.dynamodb.DynamoDbEnhancedAsyncClient;

@Repository
public class DynamoDBTemplateAdapter extends TemplateAdapterOperations<User, Long, UserItem>
        implements UppercaseUserRepositoryPort {

    public DynamoDBTemplateAdapter(
            DynamoDbEnhancedAsyncClient client,
            ObjectMapper mapper,
            @Value("${aws.dynamodb.users-table}") String tableName) {
        super(client, mapper, item -> new User(
                item.getId(),
                item.getEmail(),
                item.getFirstName(),
                item.getLastName(),
                item.getAvatar()
        ), tableName);
    }

    @Override
    protected UserItem toEntity(User user) {
        return new UserItem(
                user.id(),
                user.email(),
                user.firstName(),
                user.lastName(),
                user.avatar()
        );
    }
}
