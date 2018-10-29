package de.inoxio.spring.cloudwatchmetrics;

import java.io.IOException;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonTokenId;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

public class MetricDTODeserializer extends StdDeserializer<MetricDTO> {

    private static final long serialVersionUID = 2732502719462362601L;

    public MetricDTODeserializer() {
        super(MetricDTO.class);
    }

    @Override
    public MetricDTO deserialize(final JsonParser jp, final DeserializationContext ctxt) throws IOException {

        if (jp.isExpectedStartArrayToken()) {
            return deserializeMetric(jp, ctxt);
        }

        return (MetricDTO) ctxt.handleUnexpectedToken(handledType(), jp);
    }

    private MetricDTO deserializeMetric(final JsonParser jp, final DeserializationContext ctxt) throws IOException {

        final var metric = new MetricDTO();

        while (true) {
            final var currentToken = jp.nextToken();
            switch (currentToken.id()) {
                case JsonTokenId.ID_END_ARRAY:
                    return metric;
                case JsonTokenId.ID_STRING:
                    // this can be: Namespace, MetricName, Dimension1Name, Dimension1Value, Dimension2Name, Dimension2Value...
                    metric.addValues(jp.getText());
                    break;
                case JsonTokenId.ID_START_OBJECT:
                    // this can be {Rendering Properties Object}
                    metric.setProperty(jp.readValueAs(RenderingPropertyDTO.class));
                    break;
                default:
                    return (MetricDTO) ctxt.handleUnexpectedToken(handledType(), jp);
            }
        }
    }
}
