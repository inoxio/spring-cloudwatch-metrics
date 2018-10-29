package de.inoxio.spring.cloudwatchmetrics;

import static com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL;
import static com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS;

import static software.amazon.awssdk.regions.Region.EU_CENTRAL_1;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatterBuilder;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.datatype.jsr310.deser.InstantDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.ZonedDateTimeSerializer;

import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

@Configuration
public class CloudwatchConfig {

    @Bean
    public CloudWatchAsyncClient cloudWatchClient() {
        return CloudWatchAsyncClient.builder().region(EU_CENTRAL_1).build();
    }

    @Bean
    public Jackson2ObjectMapperBuilder jacksonBuilder() {
        final var module = new JavaTimeModule();
        module.addDeserializer(ZonedDateTime.class, InstantDeserializer.ZONED_DATE_TIME);
        module.addSerializer(ZonedDateTime.class,
                             new ZonedDateTimeSerializer(new DateTimeFormatterBuilder().appendInstant(0)
                                                                                       .toFormatter()));

        return Jackson2ObjectMapperBuilder.json()
                                          .serializationInclusion(NON_NULL)
                                          .modules(module)
                                          .featuresToDisable(WRITE_DATES_AS_TIMESTAMPS);

    }
}