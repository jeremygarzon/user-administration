package co.com.onboarding.users.metrics.aws;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import software.amazon.awssdk.metrics.MetricCollection;
import software.amazon.awssdk.metrics.MetricPublisher;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Component
@RequiredArgsConstructor
public class MicrometerMetricPublisher implements MetricPublisher {
    private final ExecutorService service = Executors.newFixedThreadPool(10);
    private final MeterRegistry registry;

    @Override
    public void publish(MetricCollection metricCollection) {
        service.submit(() -> {
            List<Tag> tags = buildTags(metricCollection);
            metricCollection.stream().forEach(record -> {
                if (record.value() instanceof Duration duration) {
                    registry.timer(record.metric().name(), tags).record(duration);
                } else if (record.value() instanceof Integer count) {
                    registry.counter(record.metric().name(), tags).increment(count);
                }
            });
        });
    }

    @Override
    public void close() {
        service.shutdown();
    }

    private List<Tag> buildTags(MetricCollection metricCollection) {
        return metricCollection.stream()
                .filter(record -> record.value() instanceof String || record.value() instanceof Boolean)
                .map(record -> Tag.of(record.metric().name(), record.value().toString()))
                .toList();
    }
}
