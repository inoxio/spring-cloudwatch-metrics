package de.inoxio.spring.cloudwatchmetrics;

import static org.assertj.core.api.Assertions.assertThat;

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
        final var metricKeyPair = metricKeyPairBuilder().name("name").value(1.0).build();

        // when
        final var toString = metricKeyPair.toString();

        // then
        assertThat(toString).as("String of converted object is not correct.")
                            .isEqualTo("MetricKeyPair{name='name', value=1.0}");
    }

}