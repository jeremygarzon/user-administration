package co.com.onboarding.users.sqs.listener.helper;

import co.com.onboarding.users.sqs.listener.config.SQSProperties;
import lombok.Builder;
import lombok.extern.slf4j.Slf4j;
import reactor.core.Disposable;
import reactor.core.Disposables;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;
import reactor.util.retry.Retry;
import software.amazon.awssdk.services.sqs.SqsAsyncClient;
import software.amazon.awssdk.services.sqs.model.DeleteMessageRequest;
import software.amazon.awssdk.services.sqs.model.Message;
import software.amazon.awssdk.services.sqs.model.ReceiveMessageRequest;

import java.time.Duration;
import java.util.function.Function;

import static net.logstash.logback.argument.StructuredArguments.kv;

@Slf4j
@Builder
public class SQSListener {
    private static final Duration MIN_POLL_BACKOFF = Duration.ofSeconds(1);
    private static final Duration MAX_POLL_BACKOFF = Duration.ofSeconds(30);
    private static final String METRIC_NAME = "async_operation";
    private static final String POLL_FAILED = "Error polling sqs queue, retrying";
    private static final String MESSAGE_FAILED = "Error processing sqs message, it will be redelivered";

    private final SqsAsyncClient client;
    private final SQSProperties properties;
    private final Function<Message, Mono<Void>> processor;
    private final Disposable.Composite subscriptions = Disposables.composite();

    public SQSListener start() {
        Scheduler scheduler = Schedulers.newParallel("sqs-pool", properties.numberOfThreads());
        subscriptions.add(scheduler);
        for (var i = 0; i < properties.numberOfThreads(); i++) {
            subscriptions.add(listenRetryRepeat()
                    .subscribeOn(scheduler)
                    .subscribe());
        }
        return this;
    }

    public void stop() {
        subscriptions.dispose();
    }

    private Flux<Void> listenRetryRepeat() {
        return listen()
                .repeat()
                .retryWhen(Retry.backoff(Long.MAX_VALUE, MIN_POLL_BACKOFF)
                        .maxBackoff(MAX_POLL_BACKOFF)
                        .doBeforeRetry(signal -> log.warn(POLL_FAILED, kv("queueUrl", properties.queueUrl()),
                                kv("attempt", signal.totalRetries() + 1), kv("error", signal.failure().toString()))));
    }

    private Flux<Void> listen() {
        return getMessages()
                .flatMap(message -> processor.apply(message)
                        .name(METRIC_NAME)
                        .tag("operation", "MessageFrom:" + properties.queueUrl())
                        .metrics()
                        .then(confirm(message))
                        .onErrorResume(error -> {
                            log.error(MESSAGE_FAILED, kv("messageId", message.messageId()), error);
                            return Mono.empty();
                        }),
                        properties.maxNumberOfMessages());
    }

    private Mono<Void> confirm(Message message) {
        return Mono.fromFuture(() -> client.deleteMessage(getDeleteMessageRequest(message.receiptHandle())))
                .then();
    }

    private Flux<Message> getMessages() {
        return Mono.fromFuture(() -> client.receiveMessage(getReceiveMessageRequest()))
                .flatMapMany(response -> Flux.fromIterable(response.messages()));
    }

    private ReceiveMessageRequest getReceiveMessageRequest() {
        return ReceiveMessageRequest.builder()
                .queueUrl(properties.queueUrl())
                .maxNumberOfMessages(properties.maxNumberOfMessages())
                .waitTimeSeconds(properties.waitTimeSeconds())
                .visibilityTimeout(properties.visibilityTimeoutSeconds())
                .build();
    }

    private DeleteMessageRequest getDeleteMessageRequest(String receiptHandle) {
        return DeleteMessageRequest.builder()
                .queueUrl(properties.queueUrl())
                .receiptHandle(receiptHandle)
                .build();
    }
}
