package co.com.onboarding.users.sqs.listener.config;

import co.com.onboarding.users.sqs.listener.helper.SQSListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.metrics.MetricPublisher;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.Message;

import java.net.URI;
import java.util.function.Function;

@Configuration
public class SQSConfig {

    @Bean(destroyMethod = "stop")
    public SQSListener sqsListener(SqsAsyncClient client, SQSProperties properties, Function<Message, Mono<Void>> fn) {
        return SQSListener.builder()
                .client(client)
                .properties(properties)
                .processor(fn)
                .build()
                .start();
    }

    @Bean
    @ConditionalOnMissingBean(SqsAsyncClient.class)
    public SqsAsyncClient configSqs(SQSProperties properties, MetricPublisher publisher) {
        return SqsAsyncClient.builder()
                .endpointOverride(resolveEndpoint(properties))
                .region(Region.of(properties.region()))
                .overrideConfiguration(o -> o.addMetricPublisher(publisher))
                .credentialsProvider(DefaultCredentialsProvider.builder().build())
                .build();
    }

    protected URI resolveEndpoint(SQSProperties properties) {
        if (properties.endpoint() != null && !properties.endpoint().isBlank()) {
            return URI.create(properties.endpoint());
        }
        return null;
    }
}
