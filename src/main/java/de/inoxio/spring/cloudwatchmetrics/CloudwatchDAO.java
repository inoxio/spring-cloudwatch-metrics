package de.inoxio.spring.cloudwatchmetrics;

public interface CloudwatchDAO {

    void pushMetrics(MetricKeyPair... metrics);
}
