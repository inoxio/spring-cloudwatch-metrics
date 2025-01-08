package de.inoxio.spring.cloudwatchmetrics;

import static de.inoxio.spring.cloudwatchmetrics.AnnotationDTO.AnnotationBuilder.annotionBuilder;
import static de.inoxio.spring.cloudwatchmetrics.AnnotationsDTO.AnnotationsBuilder.annotationsBuilder;
import static de.inoxio.spring.cloudwatchmetrics.DimensionKeyPair.DimensionKeyPairBuilder.dimensionKeyPairBuilder;
import static de.inoxio.spring.cloudwatchmetrics.MetricDTO.MetricBuilder.metricBuilder;
import static de.inoxio.spring.cloudwatchmetrics.MetricKeyPair.MetricKeyPairBuilder.metricKeyPairBuilder;
import static de.inoxio.spring.cloudwatchmetrics.PropertyDTO.PropertyBuilder.propertyBuilder;
import static de.inoxio.spring.cloudwatchmetrics.WidgetDTO.WidgetBuilder.widgetBuilder;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.assertj.core.api.ThrowableAssert;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
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

class CloudwatchRestDAOTest {

    @Test
    void shouldPutMetricsToCloudwatch() {

        // given
        final var cloudWatchClient = mock(CloudWatchAsyncClient.class);
        given(cloudWatchClient.putMetricData(anyPutMetricDataRequest()))
                .willReturn(CompletableFuture.completedFuture(null));
        final var cloudwatchRestDAO = new CloudwatchRestDAO(cloudWatchClient, mock(ObjectMapper.class));
        cloudwatchRestDAO.setMetricPrefix("somePrefix");
        cloudwatchRestDAO.setNamespace("someNamespace");

        // when
        cloudwatchRestDAO.pushMetrics(metricKeyPairBuilder().name("someMetric").value(10).build());

        // then
        final var captor = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        then(cloudWatchClient).should().putMetricData(captor.capture());

        final var request = captor.getValue();
        assertThat(request.namespace()).as("Request does not contain the correct namespace").isEqualTo("someNamespace");

        final var metricData = request.metricData();
        assertThat(metricData).as("Size of metric is not one.").hasSize(1);
        assertThat(metricData).as("Metric name is not correct")
                .extracting(MetricDatum::metricName).containsExactly("somePrefixsomeMetric");
        assertThat(metricData).as("Metric has incorrect value.")
                .extracting(MetricDatum::value).containsExactly(10.0d);
        assertThat(metricData).as("Unit type of metric is incorrect.")
                .extracting(MetricDatum::unit).containsExactly(StandardUnit.COUNT);
        assertThat(metricData).as("Size of metric dimensions is incorrect.")
                .flatExtracting(MetricDatum::dimensions).hasSize(0);
    }

    @Test
    void shouldPutMetricsToCloudwatchWithDimension() {

        // given
        final var cloudWatchClient = mock(CloudWatchAsyncClient.class);
        given(cloudWatchClient.putMetricData(anyPutMetricDataRequest())).willReturn(CompletableFuture.completedFuture(
                null));
        final var cloudwatchRestDAO = new CloudwatchRestDAO(cloudWatchClient, mock(ObjectMapper.class));
        cloudwatchRestDAO.setMetricPrefix("somePrefix");
        cloudwatchRestDAO.setNamespace("someNamespace");
        cloudwatchRestDAO.addDimension(dimensionKeyPairBuilder().name("someDimension").value("dimensionValue").build());

        // when
        cloudwatchRestDAO.pushMetrics(metricKeyPairBuilder().name("someMetric").value(10).build());

        // then
        final var captor = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        then(cloudWatchClient).should().putMetricData(captor.capture());

        final var request = captor.getValue();
        assertThat(request.namespace()).as("Request does not contain the correct namespace").isEqualTo("someNamespace");

        final var metricData = request.metricData();
        assertThat(metricData).as("Size of metric is not one.").hasSize(1);
        assertThat(metricData).as("Metric name is not correct")
                .extracting(MetricDatum::metricName).containsExactly("somePrefixsomeMetric");
        assertThat(metricData).as("Metric has incorrect value.")
                .extracting(MetricDatum::value).containsExactly(10.0d);
        assertThat(metricData).as("Unit type of metric is incorrect.")
                .extracting(MetricDatum::unit).containsExactly(StandardUnit.COUNT);
        assertThat(metricData).as("Dimonsion name and value is incorrect.")
                .flatExtracting(MetricDatum::dimensions)
                .extracting(Dimension::name, Dimension::value)
                .containsExactly(tuple("someDimension", "dimensionValue"));
    }

    @Test
    void shouldGetDashboardWhenCreatingAnnotations() {

        // given
        final var cloudWatchClient = mock(CloudWatchAsyncClient.class);
        given(cloudWatchClient.getDashboard(anyGetDashboardRequest())).willReturn(CompletableFuture.completedFuture(
                GetDashboardResponse.builder().build()));

        final var cloudwatchRestDAO = spy(new CloudwatchRestDAO(cloudWatchClient, mock(ObjectMapper.class)));
        cloudwatchRestDAO.setDashboardName("someDashboardName");
        willDoNothing().given(cloudwatchRestDAO).handleGetDashboard(null, null);

        // when
        cloudwatchRestDAO.annotateServerStart();

        // then
        final var captor = ArgumentCaptor.forClass(GetDashboardRequest.class);
        then(cloudWatchClient).should().getDashboard(captor.capture());
        assertThat(captor.getValue().dashboardName()).as("Deshboard name is incorrect.").isEqualTo("someDashboardName");

        then(cloudwatchRestDAO).should().handleGetDashboard(any(), any());
    }

    @Test
    void shouldNotGetDashboardWhenNoNameSet() {

        // given
        final var cloudWatchClient = mock(CloudWatchAsyncClient.class);
        final var cloudwatchRestDAO = new CloudwatchRestDAO(cloudWatchClient, mock(ObjectMapper.class));

        // when
        cloudwatchRestDAO.annotateServerStart();

        // then
        then(cloudWatchClient).shouldHaveNoInteractions();
    }

    @Test
    void shouldDoNothingOnGetDashboardIfNonExisting() {

        // given
        final var cloudwatchRestDAO = spy(new CloudwatchRestDAO(mock(CloudWatchAsyncClient.class),
                                                                mock(ObjectMapper.class)));
        final var resourceNotFoundException = ResourceNotFoundException.builder()
                                                                       .awsErrorDetails(AwsErrorDetails.builder()
                                                                                                       .build())
                                                                       .build();

        // when
        cloudwatchRestDAO.handleGetDashboard(null, new CompletionException(resourceNotFoundException));

        // then
        then(cloudwatchRestDAO).should(times(0)).checkAndThrowError(any());
        then(cloudwatchRestDAO).should(times(0)).annotateWidgets(any());
        then(cloudwatchRestDAO).should(times(0)).updateChangedDashboard(any());
    }

    @Test
    void shouldThrowExceptionOnGetDashboardIfError() {

        // given
        final var cloudwatchRestDAO = spy(new CloudwatchRestDAO(mock(CloudWatchAsyncClient.class),
                                                                mock(ObjectMapper.class)));
        final var testException = new Throwable("Test exception. IGNORE!");
        willThrow(new RuntimeException(testException)).given(cloudwatchRestDAO).checkAndThrowError(testException);

        // when
        final ThrowableAssert.ThrowingCallable callable = () -> cloudwatchRestDAO.handleGetDashboard(null, testException);

        // then
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(callable);
    }

    @Test
    void shouldNotUpdateDashboardIfWidgetIsUnchanged() {

        // given
        final var cloudwatchRestDAO = spy(new CloudwatchRestDAO(mock(CloudWatchAsyncClient.class),
                                                                mock(ObjectMapper.class)));
        willReturn("some widgets").given(cloudwatchRestDAO).annotateWidgets("some widgets");

        // when
        cloudwatchRestDAO.handleGetDashboard("some widgets", null);

        // then
        then(cloudwatchRestDAO).should(times(0)).updateChangedDashboard(any());
    }

    @Test
    void shouldNotUpdateDashboardIfParsedWidgetIsNull() {

        // given
        final var cloudwatchRestDAO = spy(new CloudwatchRestDAO(mock(CloudWatchAsyncClient.class),
                                                                mock(ObjectMapper.class)));
        willReturn(null).given(cloudwatchRestDAO).annotateWidgets("some widgets");

        // when
        cloudwatchRestDAO.handleGetDashboard("some widgets", null);

        // then
        then(cloudwatchRestDAO).should(times(0)).updateChangedDashboard(any());
    }

    @Test
    void shouldUpdateDashboardIfWidgetIsChanged() {

        // given
        final var cloudwatchRestDAO = spy(new CloudwatchRestDAO(mock(CloudWatchAsyncClient.class),
                                                                mock(ObjectMapper.class)));
        willReturn("some other widget").given(cloudwatchRestDAO).annotateWidgets("some widgets");
        willDoNothing().given(cloudwatchRestDAO).updateChangedDashboard("some other widget");

        // when
        cloudwatchRestDAO.handleGetDashboard("some widgets", null);

        // then
        final var captor = ArgumentCaptor.forClass(String.class);
        then(cloudwatchRestDAO).should().updateChangedDashboard(captor.capture());
        assertThat(captor.getValue()).as("Dashboard was not updated.").isEqualTo("some other widget");
    }

    @Test
    void shouldThrowRuntimeExceptionIfGiven() {

        // given
        final var cloudwatchRestDAO = new CloudwatchRestDAO(mock(CloudWatchAsyncClient.class),
                                                            mock(ObjectMapper.class));

        // when
        final ThrowableAssert.ThrowingCallable callable = () -> cloudwatchRestDAO.checkAndThrowError(new CompletionException(new Exception("Test exception. IGNORE!")));

        // then
        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(callable);
    }

    @Test
    void shouldNotThrowRuntimeExceptionIfNonGiven() {

        // given
        final var cloudwatchRestDAO = new CloudwatchRestDAO(mock(CloudWatchAsyncClient.class),
                                                            mock(ObjectMapper.class));

        // when
        cloudwatchRestDAO.checkAndThrowError(null);

        // then
        //ok
    }

    @Test
    void shouldNotThrowExceptionIfNoCauseIsGiven() {

        // given
        final var cloudwatchRestDAO = new CloudwatchRestDAO(mock(CloudWatchAsyncClient.class),
                                                            mock(ObjectMapper.class));

        // when
        cloudwatchRestDAO.checkAndThrowError(new Exception("Test exception. IGNORE!"));

        // then
    }

    @Test
    void shouldUpdateDashboard() {

        // given
        final var cloudWatchClient = mock(CloudWatchAsyncClient.class);
        final var dashboardResponse = PutDashboardResponse.builder()
                                                          .dashboardValidationMessages(DashboardValidationMessage.builder()
                                                                                                                 .build())
                                                          .build();
        given(cloudWatchClient.putDashboard(anyPutDashboardRequest())).willReturn(CompletableFuture.completedFuture(
                dashboardResponse));

        final var cloudwatchRestDAO = spy(new CloudwatchRestDAO(cloudWatchClient, mock(ObjectMapper.class)));
        cloudwatchRestDAO.setDashboardName("someDashboardName");
        willDoNothing().given(cloudwatchRestDAO)
                       .handlePutDashboard(dashboardResponse.dashboardValidationMessages(), null);

        // when
        cloudwatchRestDAO.updateChangedDashboard("some widget");

        // then
        final var captor = ArgumentCaptor.forClass(PutDashboardRequest.class);
        then(cloudWatchClient).should().putDashboard(captor.capture());
        assertThat(captor.getValue().dashboardName()).as("Dashboard name is wrong.").isEqualTo("someDashboardName");

        then(cloudwatchRestDAO).should().handlePutDashboard(any(), any());

    }

    @Test
    void shouldThrowExceptionOnPutDashboardIfError() {

        // given
        final var cloudwatchRestDAO = spy(new CloudwatchRestDAO(mock(CloudWatchAsyncClient.class),
                                                                mock(ObjectMapper.class)));
        final var testException = new CompletionException(new Exception("Test exception. IGNORE!"));
        willThrow(new RuntimeException()).given(cloudwatchRestDAO).checkAndThrowError(testException);

        // when
        final ThrowableAssert.ThrowingCallable callable = () -> cloudwatchRestDAO.handlePutDashboard(List.of(), testException);

        assertThatExceptionOfType(RuntimeException.class)
                .isThrownBy(callable);
    }

    @Test
    void shouldAddNewAnnotation() {

        // given
        final var cloudwatchRestDAO = new CloudwatchRestDAO(mock(CloudWatchAsyncClient.class),
                                                            mock(ObjectMapper.class));
        cloudwatchRestDAO.setMetricPrefix("somePrefix");

        final var widget = widgetBuilder().properties(propertyBuilder().build()).build();

        // when
        cloudwatchRestDAO.addAnnotationToWidget(widget);

        // then
        final var verticalAnnotations = widget.getProperties().getAnnotations().getVertical();
        assertThat(verticalAnnotations)
                .as("Vertival annotations label is incorrect.")
                .extracting(AnnotationDTO::getLabel)
                .containsExactly("somePrefix Start");
    }

    @Test
    void shouldAddAnotherAnnotation() {

        // given
        final var cloudwatchRestDAO = new CloudwatchRestDAO(mock(CloudWatchAsyncClient.class),
                                                            mock(ObjectMapper.class));
        cloudwatchRestDAO.setMetricPrefix("somePrefix");

        final var annotation = annotionBuilder().label("some Label").value(ZonedDateTime.now()).build();

        final var widget =
         widgetBuilder().properties(propertyBuilder().annotations(annotationsBuilder().vertical(new ArrayList<>(
                List.of(annotation))).build()).build()).build();

        // when
        cloudwatchRestDAO.addAnnotationToWidget(widget);

        // then
        final var verticalAnnotations = widget.getProperties().getAnnotations().getVertical();
        assertThat(verticalAnnotations).as("Size of vertical annotations is incorrect.").hasSize(2);
        assertThat(verticalAnnotations.get(1).getLabel()).as("Label of vertical annotations is incorrect.")
                                                         .isEqualTo("somePrefix Start");
    }

    @Test
    void shouldFilterWidgetWithNoMetrics() {

        // given
        final var cloudwatchRestDAO = new CloudwatchRestDAO(mock(CloudWatchAsyncClient.class),
                                                            mock(ObjectMapper.class));
        cloudwatchRestDAO.setMetricPrefix("somePrefix");

        final var widget = widgetBuilder().properties(propertyBuilder().metrics(List.of(metricBuilder().build()))
                                                                       .build()).build();

        // when
        final var test = cloudwatchRestDAO.filterWidgets().test(widget);

        // then
        assertThat(test).as("Widget with no metrics was not filtered.").isEqualTo(false);
    }

    @Test
    void shouldFilterWidgetWithOtherMetrics() {

        // given
        final var cloudwatchRestDAO = new CloudwatchRestDAO(mock(CloudWatchAsyncClient.class),
                                                            mock(ObjectMapper.class));
        cloudwatchRestDAO.setMetricPrefix("somePrefix");

        final var values = List.of("some Namespace", "some MetricName");
        final var widget = widgetBuilder().properties(propertyBuilder().metrics(List.of(metricBuilder().values(values)
                                                                                                       .build()))
                                                                       .build()).build();

        // when
        final var test = cloudwatchRestDAO.filterWidgets().test(widget);

        // then
        assertThat(test).as("Widget with no metrics was not filtered.").isEqualTo(false);
    }

    @Test
    void shouldFilterWidgetWithPrefixedMetricName() {

        // given
        final var cloudwatchRestDAO = new CloudwatchRestDAO(mock(CloudWatchAsyncClient.class),
                                                            mock(ObjectMapper.class));
        cloudwatchRestDAO.setMetricPrefix("somePrefix");

        final var values = List.of("some Namespace", "somePrefixMetricName");
        final var widget = widgetBuilder().properties(propertyBuilder().metrics(List.of(metricBuilder().values(values)
                                                                                                       .build()))
                                                                       .build()).build();

        // when
        final var test = cloudwatchRestDAO.filterWidgets().test(widget);

        // then
        assertThat(test).as("Widget with prefixed metric name was not filtered.").isEqualTo(true);
    }

    @Test
    void shouldReturnOriginalJsonWhenJsonIsInvalid() throws IOException {

        // given
        final var objectMapper = mock(ObjectMapper.class);
        final var cloudwatchRestDAO = new CloudwatchRestDAO(mock(CloudWatchAsyncClient.class), objectMapper);
        final var json = "{\"test\":[\"test\"]}";
        willReturn(null).given(objectMapper).readValue(json, WidgetsDTO.class);

        // when
        final var actualJson = cloudwatchRestDAO.annotateWidgets(json);

        // then
        assertThat(actualJson).as("Returned json is not the original (invalid) json.").isEqualTo(json);
    }

    private PutDashboardRequest anyPutDashboardRequest() {
        return any(PutDashboardRequest.class);
    }

    private GetDashboardRequest anyGetDashboardRequest() {
        return any(GetDashboardRequest.class);
    }

    private PutMetricDataRequest anyPutMetricDataRequest() {
        return any(PutMetricDataRequest.class);
    }
}
