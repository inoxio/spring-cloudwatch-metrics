package de.inoxio.spring.cloudwatchmetrics;

public interface CloudwatchDAO {

    void addDimension(DimensionKeyPair... dimensions);

    void pushMetrics(MetricKeyPair... metrics);
}
