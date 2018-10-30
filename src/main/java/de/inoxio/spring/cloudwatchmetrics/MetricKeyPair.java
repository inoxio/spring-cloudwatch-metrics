package de.inoxio.spring.cloudwatchmetrics;

public class MetricKeyPair {

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
