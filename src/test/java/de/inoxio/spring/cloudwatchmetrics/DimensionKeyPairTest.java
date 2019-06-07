package de.inoxio.spring.cloudwatchmetrics;

import static org.assertj.core.api.Assertions.assertThat;

import static de.inoxio.spring.cloudwatchmetrics.DimensionKeyPair.DimensionKeyPairBuilder.dimensionKeyPairBuilder;

import org.junit.Test;

import nl.jqno.equalsverifier.EqualsVerifier;

public class DimensionKeyPairTest {

    @Test
    public void shouldTestEqualsAndHashcode() {
        EqualsVerifier.forClass(DimensionKeyPair.class).verify();
    }

    @Test
    public void shouldConvertObjectToString() {

        // given
        final var dimensionKeyPair = dimensionKeyPairBuilder().name("name").value("value").build();

        // when
        final var toString = dimensionKeyPair.toString();

        // then
        assertThat(toString).as("String of converted object is not correct.")
                            .isEqualTo("DimensionKeyPair{name='name', value='value'}");
    }

}