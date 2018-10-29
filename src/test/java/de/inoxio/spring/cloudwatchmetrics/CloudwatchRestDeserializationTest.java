package de.inoxio.spring.cloudwatchmetrics;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.fasterxml.jackson.databind.ObjectMapper;


@RunWith(SpringRunner.class)
@ContextConfiguration(classes = CloudwatchConfig.class, loader = AnnotationConfigContextLoader.class)
@JsonTest
public class CloudwatchRestDeserializationTest {

    @Autowired
    private ObjectMapper mapper;

    @Test
    public void shouldTransformSimpleMetric() throws IOException {

        // given
        //language=JSON
        final var json = "{\"metrics\":[[\"StringA\",\"StringB\"]]}";

        // when
        final var property = mapper.readValue(json, PropertyDTO.class);
        final var serializedJson = mapper.writeValueAsString(property);

        // then
        assertThat(serializedJson, is(json));
    }

    @Test
    public void shouldTransformMetricWithRenderingProperty() throws IOException {

        // given
        //language=JSON
        final var json = "{\"metrics\":[[\"StringA\",\"StringB\",{\"label\":\"some label\"}]]}";

        // when
        final var property = mapper.readValue(json, PropertyDTO.class);
        final var serializedJson = mapper.writeValueAsString(property);

        // then
        assertThat(serializedJson, is(json));
    }

    @Test
    public void shouldTransformMetricWithDateAnnotation() throws IOException {

        // given
        //language=JSON
        final var json = "{\"annotations\":{\"vertical\":[{\"label\":\"ServerStart\",\"value\":\"2018-09-24T00:03:25Z\"}]}}";

        // when
        final var property = mapper.readValue(json, PropertyDTO.class);
        final var serializedJson = mapper.writeValueAsString(property);

        // then
        assertThat(serializedJson, is(json));
    }

    @Test
    public void shouldTransformMultipleMetrics() throws IOException {

        // given
        //language=JSON
        final var json = "{\"metrics\":[[\"StringA\",\"StringB\"],[\"StringC\",\"StringD\",\"StringE\",\"StringF\"]]}";

        // when
        final var property = mapper.readValue(json, PropertyDTO.class);
        final var serializedJson = mapper.writeValueAsString(property);

        // then
        assertThat(serializedJson, is(json));
    }

    @Test
    public void shouldTransformMultipleMetricsWithRenderingProperty() throws IOException {

        // given
        //language=JSON
        final var json = "{\"metrics\":[[\"StringA\",\"StringB\",{\"label\":\"some label\"}],[\"StringC\",\"StringD\",\"StringE\",\"StringF\",{\"color\":\"some color\",\"label\":\"some other label\"}]]}";

        // when
        final var property = mapper.readValue(json, PropertyDTO.class);
        final var serializedJson = mapper.writeValueAsString(property);

        // then
        assertThat(serializedJson, is(json));
    }

    @Test
    public void shouldTransformRealWorldExample() throws IOException {

        // given
        //language=JSON
        final var json = "{\"widgets\":[{\"properties\":{\"metrics\":[[{\"expression\":\"m2-m1\",\"visible\":false,\"color\":\"#1f77b4\",\"id\":\"e1\",\"label\":\"HeapMemory: Free\"}],[\"Namespace\",\"HeapMemoryUsed\",\"ClusterName\",\"test-cluster\",{\"color\":\"#1f77b4\",\"id\":\"m1\",\"label\":\"HeapMemory: Used\"}],[\".\",\"HeapMemoryCommitted\",\".\",\".\",{\"color\":\"#1f77b4\",\"id\":\"m2\",\"label\":\"HeapMemory: Available\"}],[{\"expression\":\"m3-m4\",\"visible\":false,\"color\":\"#2ca02c\",\"id\":\"e2\",\"label\":\"NonHeapMemory: Free\"}],[\"Namespace\",\"NonHeapMemoryUsed\",\"ClusterName\",\"test-cluster\",{\"color\":\"#2ca02c\",\"id\":\"m4\",\"label\":\"NonHeapMemory: Used\"}],[\".\",\"NonHeapMemoryCommitted\",\".\",\".\",{\"color\":\"#2ca02c\",\"id\":\"m3\",\"label\":\"NonHeapMemory: Available\"}],[\".\",\"ProcessCpuLoad\",\".\",\".\",{\"yAxis\":\"right\",\"color\":\"#e377c2\",\"id\":\"m5\",\"label\":\"Cpu\"}]],\"view\":\"timeSeries\",\"yAxis\":{\"left\":{\"label\":\"MB\",\"min\":0,\"showUnits\":false},\"right\":{\"label\":\"%\",\"min\":0,\"max\":100,\"showUnits\":false}},\"stacked\":false,\"region\":\"eu-central-1\",\"title\":\"Monitoring\"},\"x\":12,\"width\":12,\"y\":27,\"type\":\"metric\",\"height\":8}]}";

        // when
        final var widgets = mapper.readValue(json, WidgetsDTO.class);
        final var serializedJson = mapper.writeValueAsString(widgets);

        // then
        assertThat(serializedJson, is(json));
    }
}