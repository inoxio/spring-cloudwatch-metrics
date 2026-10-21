package de.inoxio.spring.cloudwatchmetrics;

import static java.util.Objects.requireNonNull;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatterBuilder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ext.javatime.ser.ZonedDateTimeSerializer;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.module.SimpleModule;
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

public class CloudwatchRestDAO implements CloudwatchDAO {

    private static final Logger LOG = LoggerFactory.getLogger(CloudwatchRestDAO.class);

    private final CloudWatchAsyncClient cloudWatchClient;
    private final ObjectMapper objectMapper;
    private final List<Dimension> dimensions = new ArrayList<>();

    private String dashboardName;
    private String metricPrefix;
    private String namespace;

    public CloudwatchRestDAO(final CloudWatchAsyncClient cloudWatchClient,
                             final String namespace,
                             final String metricPrefix,
                             final String dashboardName) {
        this(cloudWatchClient, createObjectMapper());
        this.namespace = requireNonNull(namespace, "Namespace must not be null!");
        this.metricPrefix = requireNonNull(metricPrefix, "MetricPrefix must not be null!");
        this.dashboardName = dashboardName;
    }

    CloudwatchRestDAO(final CloudWatchAsyncClient cloudWatchClient, final ObjectMapper objectMapper) {
        this.cloudWatchClient = requireNonNull(cloudWatchClient, "CloudWatchClient must not be null!");
        this.objectMapper = requireNonNull(objectMapper, "ObjectMapper must not be null!");
    }

    /**
     * Private mapper for the dashboard body, deliberately not exposed as a bean so the consumers jackson setup stays
     * untouched.
     */
    static ObjectMapper createObjectMapper() {
        final var module = new SimpleModule();
        module.addSerializer(ZonedDateTime.class,
                             new ZonedDateTimeSerializer(new DateTimeFormatterBuilder().appendInstant(0).toFormatter()));
        return JsonMapper.builder()
                         .changeDefaultPropertyInclusion(incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL))
                         .addModule(module)
                         .build();
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
        if (metrics == null || metrics.length == 0) {
            throw new IllegalArgumentException("Metrics should at least contain one metric!");
        }

        final var metricDatums = Arrays.stream(metrics)
                .map(metric -> MetricDatum.builder()
                          .metricName(metricPrefix + metric.getName())
                          .unit(StandardUnit.COUNT)
                          .value(metric.getValue())
                          .dimensions(dimensions)
                          .build())
                .toList();

        final var request = PutMetricDataRequest.builder().namespace(namespace).metricData(metricDatums).build();

        LOG.info("Push metrics to cloudwatch: {}", (Object) metrics);
        cloudWatchClient.putMetricData(request)
                        .whenComplete((putMetricDataResponse, throwable) -> checkAndThrowError(throwable));
    }

    public void annotateServerStart() {
        if (dashboardName != null && !dashboardName.isEmpty()) {
            final var dashboardRequest = GetDashboardRequest.builder().dashboardName(dashboardName).build();

            cloudWatchClient.getDashboard(dashboardRequest)
                            .thenApply(GetDashboardResponse::dashboardBody)
                            .whenComplete(this::handleGetDashboard);
        }
    }

    void handleGetDashboard(final String widgets, final Throwable throwable) {
        // log a warning when dashboard was not found
        if (throwable != null && throwable.getCause() instanceof final ResourceNotFoundException cause) {
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
        LOG.info("Dashboard annotated");
        dashboardValidationMessages.forEach(message -> LOG.info("BUT: {}", message.message()));
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
        } catch (final JacksonException e) {
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
