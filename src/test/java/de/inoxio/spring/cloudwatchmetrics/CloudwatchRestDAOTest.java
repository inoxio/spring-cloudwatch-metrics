package de.inoxio.spring.cloudwatchmetrics;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willReturn;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;

import static de.inoxio.spring.cloudwatchmetrics.AnnotationDTO.AnnotationBuilder.annotionBuilder;
import static de.inoxio.spring.cloudwatchmetrics.AnnotationsDTO.AnnotationsBuilder.annotationsBuilder;
import static de.inoxio.spring.cloudwatchmetrics.MetricDTO.MetricBuilder.metricBuilder;
import static de.inoxio.spring.cloudwatchmetrics.MetricKeyPair.MetricKeyPairBuilder.metricKeyPairBuilder;
import static de.inoxio.spring.cloudwatchmetrics.PropertyDTO.PropertyBuilder.propertyBuilder;
import static de.inoxio.spring.cloudwatchmetrics.WidgetDTO.WidgetBuilder.widgetBuilder;

import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import com.fasterxml.jackson.databind.ObjectMapper;

import software.amazon.awssdk.awscore.exception.AwsErrorDetails;
import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;
import software.amazon.awssdk.services.cloudwatch.model.DashboardValidationMessage;
import software.amazon.awssdk.services.cloudwatch.model.GetDashboardRequest;
import software.amazon.awssdk.services.cloudwatch.model.GetDashboardResponse;
import software.amazon.awssdk.services.cloudwatch.model.PutDashboardRequest;
import software.amazon.awssdk.services.cloudwatch.model.PutDashboardResponse;
import software.amazon.awssdk.services.cloudwatch.model.PutMetricDataRequest;
import software.amazon.awssdk.services.cloudwatch.model.ResourceNotFoundException;
import software.amazon.awssdk.services.cloudwatch.model.StandardUnit;

public class CloudwatchRestDAOTest {

    @Test
    public void shouldputMetricsToCloudwatch() {

        // given
        final var cloudWatchClient = mock(CloudWatchAsyncClient.class);
        given(cloudWatchClient.putMetricData(anyPutMetricDataRequest())).willReturn(CompletableFuture.completedFuture(
                null));
        final var cloudwatchRestDAO = new CloudwatchRestDAO(cloudWatchClient, mock(ObjectMapper.class));
        cloudwatchRestDAO.setClusterName("someClusterName");
        cloudwatchRestDAO.setMetricPrefix("somePrefix");
        cloudwatchRestDAO.setNamespace("someNamespace");

        // when
        cloudwatchRestDAO.pushMetrics(metricKeyPairBuilder().withName("someMetric").withValue(10).build());

        // then
        final var captor = ArgumentCaptor.forClass(PutMetricDataRequest.class);
        then(cloudWatchClient).should().putMetricData(captor.capture());

        final var request = captor.getValue();
        assertThat(request.namespace(), is("someNamespace"));

        final var metricData = request.metricData();
        assertThat(metricData, hasSize(1));
        assertThat(metricData.get(0).metricName(), is("somePrefixsomeMetric"));
        assertThat(metricData.get(0).value(), is(10.0d));
        assertThat(metricData.get(0).unit(), is(StandardUnit.COUNT));

        final var dimensions = metricData.get(0).dimensions();
        assertThat(dimensions, hasSize(1));
        assertThat(dimensions.get(0).name(), is("ClusterName"));
        assertThat(dimensions.get(0).value(), is("someClusterName"));
    }

    @Test
    public void shouldGetDashboardWhenCreatingAnnotations() {

        // given
        final var cloudWatchClient = mock(CloudWatchAsyncClient.class);
        given(cloudWatchClient.getDashboard(anyGetDashboardRequest())).willReturn(CompletableFuture.completedFuture(
                GetDashboardResponse.builder().build()));

        final var cloudwatchRestDAO = spy(new CloudwatchRestDAO(cloudWatchClient, mock(ObjectMapper.class)));
        cloudwatchRestDAO.setClusterName("someClusterName");
        cloudwatchRestDAO.setReportServerStart(true);
        willDoNothing().given(cloudwatchRestDAO).handleGetDashboard(null, null);

        // when
        cloudwatchRestDAO.annotateServerStart();

        // then
        final var captor = ArgumentCaptor.forClass(GetDashboardRequest.class);
        then(cloudWatchClient).should().getDashboard(captor.capture());
        assertThat(captor.getValue().dashboardName(), is("someClusterName"));

        then(cloudwatchRestDAO).should().handleGetDashboard(any(), any());
    }

    @Test
    public void shouldDoNothingOnGetDashboardIfNonExisting() {

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

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionOnGetDashboardIfError() {

        // given
        final var cloudwatchRestDAO = spy(new CloudwatchRestDAO(mock(CloudWatchAsyncClient.class),
                                                                mock(ObjectMapper.class)));
        final var testException = new Throwable("Test exception. IGNORE!");
        willThrow(new RuntimeException(testException)).given(cloudwatchRestDAO).checkAndThrowError(testException);

        // when
        cloudwatchRestDAO.handleGetDashboard(null, testException);
    }

    @Test
    public void shouldNotUpdateDashboardIfWidgetIsUnchanged() {

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
    public void shouldNotUpdateDashboardIfParsedWidgetIsNull() {

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
    public void shouldUpdateDashboardIfWidgetIsChanged() {

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
        assertThat(captor.getValue(), is("some other widget"));
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowRuntimeExceptionIfGiven() {

        // given
        final var cloudwatchRestDAO = new CloudwatchRestDAO(mock(CloudWatchAsyncClient.class),
                                                            mock(ObjectMapper.class));

        // when
        cloudwatchRestDAO.checkAndThrowError(new CompletionException(new Exception("Test exception. IGNORE!")));
    }

    @Test
    public void shouldNotThrowRuntimeExceptionIfNonGiven() {

        // given
        final var cloudwatchRestDAO = new CloudwatchRestDAO(mock(CloudWatchAsyncClient.class),
                                                            mock(ObjectMapper.class));

        // when
        cloudwatchRestDAO.checkAndThrowError(null);

        // then
        //ok
    }

    @Test
    public void shouldNotThrowExceptionIfNoCauseIsGiven() {

        // given
        final var cloudwatchRestDAO = new CloudwatchRestDAO(mock(CloudWatchAsyncClient.class),
                                                            mock(ObjectMapper.class));

        // when
        cloudwatchRestDAO.checkAndThrowError(new Exception("Test exception. IGNORE!"));

        // then
    }

    @Test
    public void shouldUpdateDashboard() {

        // given
        final var cloudWatchClient = mock(CloudWatchAsyncClient.class);
        final var dashboardResponse = PutDashboardResponse.builder()
                                                          .dashboardValidationMessages(DashboardValidationMessage.builder()
                                                                                                                 .build())
                                                          .build();
        given(cloudWatchClient.putDashboard(anyPutDashboardRequest())).willReturn(CompletableFuture.completedFuture(
                dashboardResponse));

        final var cloudwatchRestDAO = spy(new CloudwatchRestDAO(cloudWatchClient, mock(ObjectMapper.class)));
        cloudwatchRestDAO.setClusterName("someClusterName");
        willDoNothing().given(cloudwatchRestDAO)
                       .handlePutDashboard(dashboardResponse.dashboardValidationMessages(), null);

        // when
        cloudwatchRestDAO.updateChangedDashboard("some widget");

        // then
        final var captor = ArgumentCaptor.forClass(PutDashboardRequest.class);
        then(cloudWatchClient).should().putDashboard(captor.capture());
        assertThat(captor.getValue().dashboardName(), is("someClusterName"));

        then(cloudwatchRestDAO).should().handlePutDashboard(any(), any());

    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionOnPutDashboardIfError() {

        // given
        final var cloudwatchRestDAO = spy(new CloudwatchRestDAO(mock(CloudWatchAsyncClient.class),
                                                                mock(ObjectMapper.class)));
        final var testException = new CompletionException(new Exception("Test exception. IGNORE!"));
        willThrow(new RuntimeException()).given(cloudwatchRestDAO).checkAndThrowError(testException);

        // when
        cloudwatchRestDAO.handlePutDashboard(null, testException);
    }

    @Test
    public void shouldLogMessageOnPutDashboard() {

        // given
        final var cloudwatchRestDAO = spy(new CloudwatchRestDAO(mock(CloudWatchAsyncClient.class),
                                                                mock(ObjectMapper.class)));
        willDoNothing().given(cloudwatchRestDAO).logInfoMessage(any());

        // when
        cloudwatchRestDAO.handlePutDashboard(Collections.emptyList(), null);

        // then
        final var captor = ArgumentCaptor.forClass(String.class);
        then(cloudwatchRestDAO).should().logInfoMessage(captor.capture());
        assertThat(captor.getValue(), is("Dashboard annotated"));
    }

    @Test
    public void shouldAlsoLogValidationWarningsOnPutDashboard() {

        // given
        final var cloudwatchRestDAO = spy(new CloudwatchRestDAO(mock(CloudWatchAsyncClient.class),
                                                                mock(ObjectMapper.class)));
        willDoNothing().given(cloudwatchRestDAO).logInfoMessage(any());

        // when
        cloudwatchRestDAO.handlePutDashboard(List.of(DashboardValidationMessage.builder()
                                                                               .message("some validation")
                                                                               .build()), null);

        // then
        final var captor = ArgumentCaptor.forClass(String.class);
        then(cloudwatchRestDAO).should(times(2)).logInfoMessage(captor.capture());
        assertThat(captor.getAllValues(), hasItems("Dashboard annotated", "BUT: some validation"));
    }

    @Test
    public void shouldAddNewAnnotation() {

        // given
        final var cloudwatchRestDAO = new CloudwatchRestDAO(mock(CloudWatchAsyncClient.class),
                                                            mock(ObjectMapper.class));
        cloudwatchRestDAO.setMetricPrefix("somePrefix");

        final var widget = widgetBuilder().properties(propertyBuilder().build()).build();

        // when
        cloudwatchRestDAO.addAnnotationToWidget(widget);

        // then
        final var verticalAnnotations = widget.getProperties().getAnnotations().getVertical();
        assertThat(verticalAnnotations, hasSize(1));
        assertThat(verticalAnnotations.get(0).getLabel(), is("somePrefix Start"));
    }

    @Test
    public void shouldAddAnotherAnnotation() {

        // given
        final var cloudwatchRestDAO = new CloudwatchRestDAO(mock(CloudWatchAsyncClient.class),
                                                            mock(ObjectMapper.class));
        cloudwatchRestDAO.setMetricPrefix("somePrefix");

        final var annotation = annotionBuilder().label("some Label").value(ZonedDateTime.now()).build();

        final var widget = widgetBuilder().properties(propertyBuilder().annotations(annotationsBuilder().vertical(new ArrayList<>(
                List.of(annotation))).build()).build()).build();

        // when
        cloudwatchRestDAO.addAnnotationToWidget(widget);

        // then
        final var verticalAnnotations = widget.getProperties().getAnnotations().getVertical();
        assertThat(verticalAnnotations, hasSize(2));
        assertThat(verticalAnnotations.get(1).getLabel(), is("somePrefix Start"));
    }

    @Test
    public void shouldFilterWidgetWithNoMetrics() {

        // given
        final var cloudwatchRestDAO = new CloudwatchRestDAO(mock(CloudWatchAsyncClient.class),
                                                            mock(ObjectMapper.class));
        cloudwatchRestDAO.setMetricPrefix("somePrefix");

        final var widget = widgetBuilder().properties(propertyBuilder().metrics(List.of(metricBuilder().build()))
                                                                       .build()).build();

        // when
        final var test = cloudwatchRestDAO.filterWidgets().test(widget);

        // then
        assertThat(test, is(false));
    }

    @Test
    public void shouldFilterWidgetWithOtherMetrics() {

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
        assertThat(test, is(false));
    }

    @Test
    public void shouldFilterWidgetWithPrefixedMetricName() {

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
        assertThat(test, is(true));
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
