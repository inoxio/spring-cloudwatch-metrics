package de.inoxio.spring.cloudwatchmetrics;

import java.util.Objects;

public final class DimensionKeyPair {

    private final String name;
    private final String value;

    private DimensionKeyPair(final String name, final String value) {
        this.name = name;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public String getValue() {
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

        final var that = (DimensionKeyPair) o;

        return Objects.equals(name, that.name) && Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, value);
    }

    @Override
    public String toString() {
        return "DimensionKeyPair{" + "name='" + name + '\'' + ", value='" + value + '\'' + '}';
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
