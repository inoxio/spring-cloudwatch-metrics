package de.inoxio.spring.cloudwatchmetrics;

import static de.inoxio.spring.cloudwatchmetrics.MetricKeyPair.MetricKeyPairBuilder.metricKeyPairBuilder;
import static java.util.concurrent.CompletableFuture.completedFuture;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataResponse;

@ExtendWith(OutputCaptureExtension.class)
class CloudwatchConfigTest {

    private final ApplicationContextRunner contextRunner =
            new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(CloudwatchConfig.class))
                                          .withPropertyValues("aws.namespace=someNamespace",
                                                              "aws.metric-prefix=somePrefix");

    @Test
    void shouldAutoConfigureCloudwatchDAO() {
        contextRunner.run(context -> {
            assertThat(context).hasSingleBean(CloudWatchAsyncClient.class);
            assertThat(context).hasSingleBean(CloudwatchDAO.class);
        });
    }

    @Test
    void shouldBackOffWhenClientIsProvided() {
        final var client = mock(CloudWatchAsyncClient.class);
        contextRunner.withBean(CloudWatchAsyncClient.class, () -> client).run(context -> {
            assertThat(context).getBean(CloudWatchAsyncClient.class).isSameAs(client);
            assertThat(context).hasSingleBean(CloudwatchDAO.class);
        });
    }

    @Test
    void shouldUseConfiguredPropertiesWithoutLoggingDefaults(final CapturedOutput output) {
        final var client = mockClient();
        contextRunner.withBean(CloudWatchAsyncClient.class, () -> client).run(context -> {
            context.getBean(CloudwatchDAO.class).pushMetrics(metricKeyPairBuilder().name("Metric").value(1).build());

            final var request = capturePutMetricDataRequest(client);
            assertThat(request.namespace()).isEqualTo("someNamespace");
            assertThat(request.metricData()).singleElement().extracting("metricName").isEqualTo("somePrefixMetric");
            assertThat(output).doesNotContain("using default");
        });
    }

    @Test
    void shouldUseAndLogDefaultsWithoutProperties(final CapturedOutput output) {
        final var client = mockClient();
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(CloudwatchConfig.class))
                                      .withBean(CloudWatchAsyncClient.class, () -> client)
                                      .run(context -> {
                                          context.getBean(CloudwatchDAO.class)
                                                 .pushMetrics(metricKeyPairBuilder().name("Metric").value(1).build());

                                          final var request = capturePutMetricDataRequest(client);
                                          assertThat(request.namespace()).isEqualTo("Default");
                                          assertThat(request.metricData()).singleElement()
                                                                          .extracting("metricName")
                                                                          .isEqualTo("Metric");
                                          assertThat(output).contains(
                                                  "Property 'aws.namespace' is not set, using default 'Default'",
                                                  "Property 'aws.metric-prefix' is not set, using default ''");
                                      });
    }

    private static CloudWatchAsyncClient mockClient() {
        final var client = mock(CloudWatchAsyncClient.class);
        when(client.putMetricData(any(PutMetricDataRequest.class))).thenReturn(completedFuture(PutMetricDataResponse.builder()
                                                                                                                    .build()));
        return client;
    }

    private static PutMetricDataRequest capturePutMetricDataRequest(final CloudWatchAsyncClient client) {
        final var captor = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        verify(client).putMetricData(captor.capture());
        return captor.getValue();
    }
}
