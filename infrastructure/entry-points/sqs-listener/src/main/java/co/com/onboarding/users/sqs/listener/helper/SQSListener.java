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

@Slf4j
@Builder
public class SQSListener {
    private static final Duration MIN_POLL_BACKOFF = Duration.ofSeconds(1);
    private static final Duration MAX_POLL_BACKOFF = Duration.ofSeconds(30);

    private final SqsAsyncClient client;
    private final SQSProperties properties;
    private final Function<Message, Mono<Void>> processor;
    private final Disposable.Composite subscriptions = Disposables.composite();
    private String operation;

    public SQSListener start() {
        this.operation = "MessageFrom:" + properties.queueUrl();
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
                        .doBeforeRetry(signal -> log.warn("Error polling sqs queue {}, retry attempt {}: {}",
                                properties.queueUrl(), signal.totalRetries() + 1, signal.failure().toString())));
    }

    private Flux<Void> listen() {
        return getMessages()
                .flatMap(message -> processor.apply(message)
                        .name("async_operation")
                        .tag("operation", operation)
                        .metrics()
                        .then(confirm(message))
                        .onErrorResume(error -> {
                            log.error("Error processing sqs message {}, it will be redelivered",
                                    message.messageId(), error);
                            return Mono.empty();
                        }),
                        properties.maxNumberOfMessages());
    }

    private Mono<Void> confirm(Message message) {
        return Mono.fromCallable(() -> getDeleteMessageRequest(message.receiptHandle()))
                .flatMap(request -> Mono.fromFuture(client.deleteMessage(request)))
                .then();
    }

    private Flux<Message> getMessages() {
        return Mono.fromCallable(this::getReceiveMessageRequest)
                .flatMap(request -> Mono.fromFuture(client.receiveMessage(request)))
                .doOnNext(response -> log.debug("{} received messages from sqs", response.messages().size()))
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
