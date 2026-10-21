package de.inoxio.spring.cloudwatchmetrics;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

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
    void shouldFailWithoutRequiredProperties() {
        new ApplicationContextRunner().withConfiguration(AutoConfigurations.of(CloudwatchConfig.class))
                                      .run(context -> assertThat(context).hasFailed());
    }
}
