package de.inoxio.spring.cloudwatchmetrics;

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonAnyGetter;
import com.fasterxml.jackson.annotation.JsonAnySetter;

public class BaseDTO {

    private final Map<String, Object> unknownProperties = new HashMap<>();

    @JsonAnySetter
    public void handleUnknownProperties(final String key, final Object value) {
        unknownProperties.put(key, value);
    }

    @JsonAnyGetter
    public Map<String, Object> getUnknownProperties() {
        return unknownProperties;
    }
}
