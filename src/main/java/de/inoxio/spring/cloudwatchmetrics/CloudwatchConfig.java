package de.inoxio.spring.cloudwatchmetrics;

import static software.amazon.awssdk.regions.Region.EU_CENTRAL_1;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

@AutoConfiguration
public class CloudwatchConfig {

    static final String DEFAULT_NAMESPACE = "Default";
    static final String DEFAULT_METRIC_PREFIX = "";

    private static final Logger LOG = LoggerFactory.getLogger(CloudwatchConfig.class);

    @Bean
    @ConditionalOnMissingBean
    public CloudWatchAsyncClient cloudWatchClient() {
        return CloudWatchAsyncClient.builder().region(EU_CENTRAL_1).build();
    }

    @Bean(initMethod = "annotateServerStart")
    @ConditionalOnMissingBean(CloudwatchDAO.class)
    public CloudwatchRestDAO cloudwatchDAO(final CloudWatchAsyncClient cloudWatchClient, final Environment environment) {
        return new CloudwatchRestDAO(cloudWatchClient,
                                     getPropertyOrDefault(environment, "aws.namespace", DEFAULT_NAMESPACE),
                                     getPropertyOrDefault(environment, "aws.metric-prefix", DEFAULT_METRIC_PREFIX),
                                     environment.getProperty("aws.dashboard-name"));
    }

    private static String getPropertyOrDefault(final Environment environment,
                                               final String key,
                                               final String defaultValue) {
        final var value = environment.getProperty(key);
        if (value != null) {
            return value;
        }
        LOG.info("Property '{}' is not set, using default '{}'. Set the property to override it.", key, defaultValue);
        return defaultValue;
    }
}
