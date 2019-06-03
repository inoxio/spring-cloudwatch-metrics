package de.inoxio.spring.cloudwatchmetrics;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class CloudwatchConfigTest {

    @Test
    public void shouldCreateCorrectCloudwatchClientBean() {

        // given
        final var appConfig = new CloudwatchConfig();

        // when
        final var cloudWatchClient = appConfig.cloudWatchClient();

        // then
        assertThat(cloudWatchClient).as("Cloud watch client is null.").isNotNull();
    }

    @Test
    public void shouldInitializeJacksonMapper() {

        // given
        final var appConfig = new CloudwatchConfig();

        // when
        final var jackson2ObjectMapperBuilder = appConfig.jacksonBuilder();

        // then
        assertThat(jackson2ObjectMapperBuilder).as("Jackson mapper is not initialized.").isNotNull();
    }
}