package de.inoxio.spring.cloudwatchmetrics;

import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ser.std.StdSerializer;

public class MetricDTOSerializer extends StdSerializer<MetricDTO> {

    public MetricDTOSerializer() {
        super(MetricDTO.class);
    }

    @Override
    public void serialize(final MetricDTO metric, final JsonGenerator gen, final SerializationContext ctxt) {

        if (metric == null) {
            return;
        }
        gen.writeStartArray();
        for (final var value : metric.getValues()) {
            gen.writeString(value);
        }
        if (metric.getProperty() != null) {
            gen.writePOJO(metric.getProperty());
        }
        gen.writeEndArray();
    }
}
