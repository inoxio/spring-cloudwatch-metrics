package de.inoxio.spring.cloudwatchmetrics;

public class DimensionKeyPair {

    private final String name;
    private final String value;

    public DimensionKeyPair(final String name, final String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
        return value;
    }

    public static final class DimensionKeyPairBuilder {

        private String name;
        private String value;

        private DimensionKeyPairBuilder() {
        }

        public static DimensionKeyPairBuilder dimensionKeyPairBuilder() {
            return new DimensionKeyPairBuilder();
        }

        public DimensionKeyPairBuilder name(final String name) {
            this.name = name;
            return this;
        }

        public DimensionKeyPairBuilder value(final String value) {
            this.value = value;
            return this;
        }

        public DimensionKeyPair build() {
            return new DimensionKeyPair(name, value);
        }
    }
}
