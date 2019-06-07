package de.inoxio.spring.cloudwatchmetrics;

import static java.lang.String.format;

import static org.springframework.util.Assert.notEmpty;
import static org.springframework.util.Assert.notNull;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Repository;

import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.model.DashboardValidationMessage;
import software.amazon.awssdk.services.cloudwatch.model.Dimension;
import software.amazon.awssdk.services.cloudwatch.model.GetDashboardRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetDashboardResponse;
import software.amazon.awssdk.services.cloudwatch.model.MetricDatum;
import software.amazon.awssdk.services.cloudwatch.model.PutDashboardRequest;
import software.amazon.awssdk.services.cloudwatch.model.PutDashboardResponse;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.ResourceNotFoundException;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

@Repository
public class CloudwatchRestDAO implements CloudwatchDAO {

    private static final Logger LOG = LoggerFactory.getLogger(CloudwatchRestDAO.class);

    private final CloudWatchAsyncClient cloudWatchClient;
    private final ObjectMapper objectMapper;
    private final List<Dimension> dimensions = new ArrayList<>();

    @Value(value = "${aws.dashboard-name:#{null}}")
    private String dashboardName;
    @Value(value = "${aws.metric-prefix}")
    private String metricPrefix;
    @Value(value = "${aws.namespace}")
    private String namespace;

    @Autowired
    public CloudwatchRestDAO(final CloudWatchAsyncClient cloudWatchClient, final ObjectMapper objectMapper) {
        notNull(cloudWatchClient, "CloudWatchClient must not be null!");
        this.cloudWatchClient = cloudWatchClient;
        notNull(objectMapper, "ObjectMapper must not be null!");
        this.objectMapper = objectMapper;
    }

    @Override
    public void addDimension(final DimensionKeyPair... dimensionKeyPairs) {
        Arrays.stream(dimensionKeyPairs)
              .map(dimensionKeyPair -> Dimension.builder()
                                                .name(dimensionKeyPair.getName())
                                                .value(dimensionKeyPair.getValue())
                                                .build())
              .forEach(dimensions::add);
    }

    @Override
    public void pushMetrics(final MetricKeyPair... metrics) {
        notEmpty(metrics, "Metrics should at least contain one metric!");

        final var logMessage = new StringBuilder("Push metrics to cloudwatch:");

        final var metricDatums = Arrays.stream(metrics).map(metric -> {
            logMessage.append(format("%n%25s = %,13.1f", metric.getName(), metric.getValue()));
            return MetricDatum.builder()
                              .metricName(metricPrefix + metric.getName())
                              .unit(StandardUnit.COUNT)
                              .value(metric.getValue())
                              .dimensions(dimensions)
                              .build();
        }).collect(Collectors.toList());

        final var request = PutMetricDataRequest.builder().namespace(namespace).metricData(metricDatums).build();

        logInfoMessage(logMessage.toString());

        cloudWatchClient.putMetricData(request)
                        .whenComplete((putMetricDataResponse, throwable) -> checkAndThrowError(throwable));
    }

    @PostConstruct
    void annotateServerStart() {
        if (dashboardName != null && !dashboardName.isEmpty()) {
            final var dashboardRequest = GetDashboardRequest.builder().dashboardName(dashboardName).build();

            cloudWatchClient.getDashboard(dashboardRequest)
                            .thenApply(GetDashboardResponse::dashboardBody)
                            .whenComplete(this::handleGetDashboard);
        }
    }

    void handleGetDashboard(final String widgets, final Throwable throwable) {
        // log a warning when dashboard was not found
        if (throwable != null && throwable.getCause() instanceof ResourceNotFoundException) {
            final var cause = (ResourceNotFoundException) throwable.getCause();
            LOG.warn(cause.awsErrorDetails().errorMessage());
            return;
        }
        checkAndThrowError(throwable);

        final var annotatedWidgets = annotateWidgets(widgets);
        if (annotatedWidgets != null && annotatedWidgets.length() != widgets.length()) {
            updateChangedDashboard(annotatedWidgets);
        }
    }

    void updateChangedDashboard(final String annotatedWidgets) {
        final var putDashboardRequest = PutDashboardRequest.builder().dashboardName(dashboardName)
                                                           .dashboardBody(annotatedWidgets)
                                                           .build();

        cloudWatchClient.putDashboard(putDashboardRequest)
                        .thenApply(PutDashboardResponse::dashboardValidationMessages)
                        .whenComplete(this::handlePutDashboard);
    }

    void handlePutDashboard(final List<DashboardValidationMessage> dashboardValidationMessages, final Throwable throwable) {
        checkAndThrowError(throwable);
        logInfoMessage("Dashboard annotated");
        dashboardValidationMessages.forEach(validationMessage -> logInfoMessage("BUT: " + validationMessage.message()));
    }

    void logInfoMessage(final String message) {
        LOG.info(message);
    }

    String annotateWidgets(final String json) {
        try {
            final var widgets = objectMapper.readValue(json, WidgetsDTO.class);

            if (widgets == null) {
                return json;
            } else {
                widgets.getWidgets().stream().filter(filterWidgets()).forEach(this::addAnnotationToWidget);
            }

            return objectMapper.writeValueAsString(widgets);
        } catch (final IOException e) {
            LOG.error("Unable to transform JSON to PoJo", e);
            return null;
        }
    }

    void addAnnotationToWidget(final WidgetDTO widget) {
        final var annotation = new AnnotationDTO();
        annotation.setLabel(metricPrefix + " Start");
        annotation.setValue(ZonedDateTime.now());
        widget.getProperties().createOrGetAnnotations().createOrGetVertical().add(annotation);
    }

    Predicate<WidgetDTO> filterWidgets() {
        return widget -> widget.getProperties()
                               .getMetrics()
                               .stream()
                               .anyMatch(metric -> metric.getMetricName().startsWith(metricPrefix));
    }

    void checkAndThrowError(final Throwable throwable) {
        if (throwable != null && throwable.getCause() != null) {
            LOG.error("Unable to send request to cloudwatch!", throwable.getCause());
            throw new RuntimeException(throwable.getCause());
        }
    }

    void setDashboardName(final String dashboardName) {
        this.dashboardName = dashboardName;
    }

    void setMetricPrefix(final String metricPrefix) {
        this.metricPrefix = metricPrefix;
    }

    void setNamespace(final String namespace) {
        this.namespace = namespace;
    }
}
