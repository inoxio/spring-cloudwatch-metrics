package de.inoxio.spring.cloudwatchmetrics;

import java.time.ZonedDateTime;

public class AnnotationDTO extends BaseDTO {

    private String label;
    private ZonedDateTime value;

    public String getLabel() {
        return label;
    }

    public void setLabel(final String label) {
        this.label = label;
    }

    public ZonedDateTime getValue() {
        return value;
    }

    public void setValue(final ZonedDateTime value) {
        this.value = value;
    }

    public static final class AnnotationBuilder {

        private String label;
        private ZonedDateTime value;

        private AnnotationBuilder() {
        }

        public static AnnotationBuilder annotionBuilder() {
            return new AnnotationBuilder();
        }

        public AnnotationBuilder label(final String label) {
            this.label = label;
            return this;
        }

        public AnnotationBuilder value(final ZonedDateTime value) {
            this.value = value;
            return this;
        }

        public AnnotationDTO build() {
            final var annotationDTO = new AnnotationDTO();
            annotationDTO.setLabel(label);
            annotationDTO.setValue(value);
            return annotationDTO;
        }
    }
}
