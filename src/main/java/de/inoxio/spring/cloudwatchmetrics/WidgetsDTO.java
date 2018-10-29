package de.inoxio.spring.cloudwatchmetrics;

import java.util.List;

public class WidgetsDTO extends BaseDTO {

    private List<WidgetDTO> widgets;

    public List<WidgetDTO> getWidgets() {
        return widgets;
    }

    public void setWidgets(final List<WidgetDTO> widgets) {
        this.widgets = widgets;
    }
}
