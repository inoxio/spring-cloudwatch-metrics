package de.inoxio.spring.cloudwatchmetrics;

public class WidgetDTO extends BaseDTO {

    private PropertyDTO properties;

    public PropertyDTO getProperties() {
        return properties;
    }

    public void setProperties(final PropertyDTO properties) {
        this.properties = properties;
    }


    public static final class WidgetBuilder {

        private PropertyDTO properties;

        private WidgetBuilder() {
        }

        public static WidgetBuilder widgetBuilder() {
            return new WidgetBuilder();
        }

        public WidgetBuilder properties(final PropertyDTO properties) {
            this.properties = properties;
            return this;
        }

        public WidgetDTO build() {
            final var widgetDTO = new WidgetDTO();
            widgetDTO.setProperties(properties);
            return widgetDTO;
        }
    }
}
