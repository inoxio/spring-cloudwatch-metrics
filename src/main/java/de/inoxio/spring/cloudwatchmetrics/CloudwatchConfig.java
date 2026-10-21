package de.inoxio.spring.cloudwatchmetrics;

import static software.amazon.awssdk.regions.Region.EU_CENTRAL_1;

import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;

import software.amazon.awssdk.services.cloudwatch.CloudWatchAsyncClient;

@AutoConfiguration
public class CloudwatchConfig {

    @Bean
    @ConditionalOnMissingBean
    public CloudWatchAsyncClient cloudWatchClient() {
        return CloudWatchAsyncClient.builder().region(EU_CENTRAL_1).build();
    }

    @Bean(initMethod = "annotateServerStart")
    @ConditionalOnMissingBean(CloudwatchDAO.class)
    public CloudwatchRestDAO cloudwatchDAO(final CloudWatchAsyncClient cloudWatchClient, final Environment environment) {
        return new CloudwatchRestDAO(cloudWatchClient,
                                     environment.getRequiredProperty("aws.namespace"),
                                     environment.getRequiredProperty("aws.metric-prefix"),
                                     environment.getProperty("aws.dashboard-name"));
    }
}
