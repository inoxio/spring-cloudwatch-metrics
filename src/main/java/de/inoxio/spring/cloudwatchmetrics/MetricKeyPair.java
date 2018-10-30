package de.inoxio.spring.cloudwatchmetrics;

import java.util.Objects;

public final class MetricKeyPair {

    private final String name;
    private final double value;

    public MetricKeyPair(final String name, final double value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public double getValue() {
        return value;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        final var that = (MetricKeyPair) o;

        return Double.compare(that.value, value) == 0 && Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }

    @Override
    public String toString() {
        return "MetricKeyPair{" + "name='" + name + '\'' + ", value=" + value + '}';
    }

    public static final class MetricKeyPairBuilder {

        private String name;
        private double value;

        private MetricKeyPairBuilder() {
        }

        public static MetricKeyPairBuilder metricKeyPairBuilder() {
            return new MetricKeyPairBuilder();
        }

        public MetricKeyPairBuilder name(final String name) {
            this.name = name;
            return this;
        }

        public MetricKeyPairBuilder value(final double value) {
            this.value = value;
            return this;
        }

        public MetricKeyPair build() {
            return new MetricKeyPair(name, value);
        }
    }
}
