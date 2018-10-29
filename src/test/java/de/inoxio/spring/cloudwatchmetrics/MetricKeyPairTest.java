package de.inoxio.spring.cloudwatchmetrics;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

import static de.inoxio.spring.cloudwatchmetrics.MetricKeyPair.MetricKeyPairBuilder.metricKeyPairBuilder;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class MetricKeyPairTest {

    @Test
    public void shouldTestEqualsAndHashcode() {
        EqualsVerifier.forClass(MetricKeyPair.class).verify();
    }

    @Test
    public void shouldConvertObjectToString() {

        // given
        final var metricKeyPair = metricKeyPairBuilder().withName("name").withValue(1.0).build();

        // when
        final var toString = metricKeyPair.toString();

        // then
        assertThat(toString, is("MetricKeyPair{name='name', value=1.0}"));
    }

}