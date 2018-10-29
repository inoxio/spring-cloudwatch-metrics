package de.inoxio.spring.cloudwatchmetrics;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import org.junit.Test;

public class CloudwatchConfigTest {

    @Test
    public void shouldCreateCorrectCloudwatchClientBean() {

        // given
        final var appConfig = new CloudwatchConfig();

        // when
        final var cloudWatchClient = appConfig.cloudWatchClient();

        // then
        assertThat(cloudWatchClient, is(not(nullValue())));
    }

}