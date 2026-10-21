package de.inoxio.spring.cloudwatchmetrics;

import tools.jackson.core.JsonParser;
import tools.jackson.core.JsonTokenId;
import tools.jackson.databind.DeserializationContext;
import tools.jackson.databind.deser.std.StdDeserializer;

public class MetricDTODeserializer extends StdDeserializer<MetricDTO> {

    public MetricDTODeserializer() {
        super(MetricDTO.class);
    }

    @Override
    public MetricDTO deserialize(final JsonParser jp, final DeserializationContext ctxt) {

        if (jp.isExpectedStartArrayToken()) {
            return deserializeMetric(jp, ctxt);
        }

        return (MetricDTO) ctxt.handleUnexpectedToken(handledType(), jp);
    }

    private MetricDTO deserializeMetric(final JsonParser jp, final DeserializationContext ctxt) {

        final var metric = new MetricDTO();

        while (true) {
            final var currentToken = jp.nextToken();
            switch (currentToken.id()) {
                case JsonTokenId.ID_END_ARRAY:
                    return metric;
                case JsonTokenId.ID_STRING:
                    // this can be: Namespace, MetricName, Dimension1Name, Dimension1Value, Dimension2Name, Dimension2Value...
                    metric.addValues(jp.getString());
                    break;
                case JsonTokenId.ID_START_OBJECT:
                    // this can be {Rendering Properties Object}
                    metric.setProperty(ctxt.readValue(jp, RenderingPropertyDTO.class));
                    break;
                default:
                    return (MetricDTO) ctxt.handleUnexpectedToken(handledType(), jp);
            }
        }
    }
}
