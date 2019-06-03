# spring-cloudwatch-metrics

A java-spring library to push metrics to cloudwatch

If you have any questions please open an issue ticket!

## Usage

Add this to your build.gradle.kts
```kotlin
dependencies {
    implementation("de.inoxio:spring-cloudwatch-metrics:1.0.4")
}
```

Inject the CloudwatchDAO from your service as usual:

```java
@Service
public class SomeServiceImpl implements SomeService {
    private final CloudwatchDAO cloudwatchDAO;
    
    @Autowired
    public SomeServiceImpl(final CloudwatchDAO cloudwatchDAO) {
        notNull(cloudwatchDAO, "CloudwatchDAO must not be null!");
        this.cloudwatchDAO = cloudwatchDAO;
    }
    
    /**
    * Optional: If you want to add one or more dimensions to your metrics 
    */
    @PostConstruct
    private void addDimension() {
        cloudwatchDAO.addDimension(dimensionKeyPairBuilder().name("DimensionName")
                                                            .value("DimensionValue")
                                                            .build());
    }
    
    /**
    * To push used heap memory and cpu usage to cloudwatch 
    * have a method like this in your service that calls
    * the CloudwatchDAO. Intended to be invoked by a scheduler.
    */
    public void pushMetrics() {
        final var memoryBean = ManagementFactory.getMemoryMXBean();
        final var operatingSystemBean = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
        
        cloudwatchDAO.pushMetrics(metricKeyPairBuilder().withName("HeapMemoryUsed")
                                                        .withValue(memoryBean.getHeapMemoryUsage().getUsed())
                                                        .build(),
                                  metricKeyPairBuilder().withName("ProcessCpuLoad")
                                                        .withValue(operatingSystemBean.getProcessCpuLoad() * MAX_PERCENT)
                                                        .build());
    }
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

## Release

Change version in `build.gradle.kts`, `README.md` and issue:

```bash
./gradlew bintrayUpload
```
