package de.inoxio.spring.cloudwatchmetrics;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;

@JsonDeserialize(using = MetricDTODeserializer.class)
@JsonSerialize(using = MetricDTOSerializer.class)
public class MetricDTO extends BaseDTO {

    private List<String> values = new ArrayList<>();
    private RenderingPropertyDTO property;

    // [Namespace, MetricName, Dimension1Name, Dimension1Value, Dimension2Name, Dimension2Value... {Rendering Properties Object}]
    public String getMetricName() {
        return (values.size() > 1) ? values.get(1) : "";
    }

    public List<String> getValues() {
        return values;
    }

    public void addValues(final String value) {
        this.values.add(value);
    }

    public RenderingPropertyDTO getProperty() {
        return property;
    }

    public void setProperty(final RenderingPropertyDTO property) {
        this.property = property;
    }


    public static final class MetricBuilder {

        private List<String> values = new ArrayList<>();

        private MetricBuilder() {
        }

        public static MetricBuilder metricBuilder() {
            return new MetricBuilder();
        }

        public MetricBuilder values(final List<String> values) {
            this.values = values;
            return this;
        }

        public MetricDTO build() {
            final var metricDTO = new MetricDTO();
            metricDTO.values = this.values;
            return metricDTO;
        }
    }
}
