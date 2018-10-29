package de.inoxio.spring.cloudwatchmetrics;

import java.util.List;

public class PropertyDTO extends BaseDTO {

    private List<MetricDTO> metrics;
    private AnnotationsDTO annotations;

    public List<MetricDTO> getMetrics() {
        return metrics;
    }

    public void setMetrics(final List<MetricDTO> metrics) {
        this.metrics = metrics;
    }

    public AnnotationsDTO getAnnotations() {
        return annotations;
    }

    public void setAnnotations(final AnnotationsDTO annotations) {
        this.annotations = annotations;
    }

    public AnnotationsDTO createOrGetAnnotations() {
        if (annotations == null) {
            annotations = new AnnotationsDTO();
        }
        return annotations;
    }

    public static final class PropertyBuilder {

        private List<MetricDTO> metrics;
        private AnnotationsDTO annotations;

        private PropertyBuilder() {
        }

        public static PropertyBuilder propertyBuilder() {
            return new PropertyBuilder();
        }

        public PropertyBuilder metrics(final List<MetricDTO> metrics) {
            this.metrics = metrics;
            return this;
        }

        public PropertyBuilder annotations(final AnnotationsDTO annotations) {
            this.annotations = annotations;
            return this;
        }

        public PropertyDTO build() {
            final var propertyDTO = new PropertyDTO();
            propertyDTO.setMetrics(metrics);
            propertyDTO.setAnnotations(annotations);
            return propertyDTO;
        }
    }
}
