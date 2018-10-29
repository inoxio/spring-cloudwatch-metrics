package de.inoxio.spring.cloudwatchmetrics;

import java.util.ArrayList;
import java.util.List;

public class AnnotationsDTO extends BaseDTO {

    private List<AnnotationDTO> vertical;

    public List<AnnotationDTO> getVertical() {
        return vertical;
    }

    public void setVertical(final List<AnnotationDTO> vertical) {
        this.vertical = vertical;
    }

    public List<AnnotationDTO> createOrGetVertical() {
        if (vertical == null) {
            vertical = new ArrayList<>();
        }
        return vertical;
    }

    public static final class AnnotationsBuilder {

        private List<AnnotationDTO> vertical;

        private AnnotationsBuilder() {
        }

        public static AnnotationsBuilder annotationsBuilder() {
            return new AnnotationsBuilder();
        }

        public AnnotationsBuilder vertical(final List<AnnotationDTO> vertical) {
            this.vertical = vertical;
            return this;
        }

        public AnnotationsDTO build() {
            final var annotationsDTO = new AnnotationsDTO();
            annotationsDTO.setVertical(vertical);
            return annotationsDTO;
        }
    }
}
