# spring-cloudwatch-metrics

A java-spring library to push metrics to cloudwatch

If you have any questions please open an issue ticket!

## Requirements

* Java 21+
* Spring Boot 4.x (Jackson 3)

## Usage

Add this to your build.gradle.kts
```kotlin
dependencies {
    implementation("de.inoxio:spring-cloudwatch-metrics:3.0.0")
}
```

Add the following properties to your project:
```yaml
aws:
  namespace: Namespace                 # Namespace where the metrics will be pushed to
  metric-prefix: AppPrefix             # All Metrics will get this prefix. Final name will be AppPrefixHeapMemoryUsed
  dashboard-name: some-dashboard-name  # Optional: Set it to the dashboard name you want graphs to be annotated on 
                                       #           server start. It will add a vertical annotation to all graphs with
                                       #           metrics that start with 'metric-prefix'
```

The library registers itself via Spring Boot auto-configuration, no component scan is needed. Inject the
`CloudwatchDAO` into your service:

```java
import static de.inoxio.spring.cloudwatchmetrics.DimensionKeyPair.DimensionKeyPairBuilder.dimensionKeyPairBuilder;
import static de.inoxio.spring.cloudwatchmetrics.MetricKeyPair.MetricKeyPairBuilder.metricKeyPairBuilder;

import java.lang.management.ManagementFactory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.sun.management.OperatingSystemMXBean;

import de.inoxio.spring.cloudwatchmetrics.CloudwatchDAO;

@Service
public class MetricsService {

    private static final int MAX_PERCENT = 100;

    private final CloudwatchDAO cloudwatchDAO;

    public MetricsService(final CloudwatchDAO cloudwatchDAO) {
        this.cloudwatchDAO = cloudwatchDAO;
        // Optional: add one or more dimensions to all metrics
        cloudwatchDAO.addDimension(dimensionKeyPairBuilder().name("DimensionName")
                                                            .value("DimensionValue")
                                                            .build());
    }

    /**
     * Pushes used heap memory and cpu usage to cloudwatch every minute.
     * Requires @EnableScheduling on one of your configuration classes.
     */
    @Scheduled(fixedRate = 60_000)
    public void pushMetrics() {
        final var memoryBean = ManagementFactory.getMemoryMXBean();
        final var operatingSystemBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();

        cloudwatchDAO.pushMetrics(metricKeyPairBuilder().name("HeapMemoryUsed")
                                                        .value(memoryBean.getHeapMemoryUsage().getUsed())
                                                        .build(),
                                  metricKeyPairBuilder().name("ProcessCpuLoad")
                                                        .value(operatingSystemBean.getProcessCpuLoad() * MAX_PERCENT)
                                                        .build());
    }
}
```

All metrics are pushed with the unit `Count`.

### Custom CloudWatch client

By default a `CloudWatchAsyncClient` for region `eu-central-1` is created. To use another region or custom
credentials, define your own bean, the auto-configured one backs off:

```java
@Bean
public CloudWatchAsyncClient cloudWatchClient() {
    return CloudWatchAsyncClient.builder().region(Region.EU_WEST_1).build();
}
```

You can also replace the `CloudwatchDAO` completely by defining your own bean of that type.

## Migration from 2.x to 3.0.0

* Requires Spring Boot 4 and Jackson 3 (`tools.jackson`).
* The library no longer registers a global `Jackson2ObjectMapperBuilder` bean, your application's Jackson
  configuration stays untouched.
* `CloudwatchConfig` is now an auto-configuration. If you imported or component-scanned it explicitly, remove that.
* The public constructor of `CloudwatchRestDAO` changed to
  `(CloudWatchAsyncClient, String namespace, String metricPrefix, String dashboardName)`.

## Dependencies

At runtime this project only depends on:
* AWS SDK `cloudwatch` (part of the API, the `CloudWatchAsyncClient` bean can be overridden)
* Jackson 3 `jackson-databind` (used internally, no `ObjectMapper` bean is registered)
* `slf4j-api`

Spring Boot is not pulled in, the consuming application brings its own (Spring Boot 4.x).

## Release

Change version in `build.gradle.kts`, `README.md` and issue:

```bash
./gradlew clean build publish jreleaserFullRelease
```

Publishing to Maven Central (Central Portal), signing and the GitHub release are done by
[JReleaser](https://jreleaser.org). It expects the following secrets in `~/.jreleaser/config.properties`
or as environment variables:

```properties
JRELEASER_MAVENCENTRAL_USERNAME=<Central Portal user token name>
JRELEASER_MAVENCENTRAL_PASSWORD=<Central Portal user token password>
JRELEASER_GPG_PUBLIC_KEY=<armored public key>
JRELEASER_GPG_SECRET_KEY=<armored secret key>
JRELEASER_GPG_PASSPHRASE=<passphrase>
JRELEASER_GITHUB_TOKEN=<GitHub token>
```

Append `--dryrun` to check the configuration without uploading anything.

## Check Renovate

https://developer.mend.io/github/inoxio/spring-cloudwatch-metrics
