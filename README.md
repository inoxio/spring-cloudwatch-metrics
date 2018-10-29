# spring-cloudwatch-metrics

A java-spring library to push metrics to cloudwatch

Not yet production ready, because it has some dependency to a `clusterName` you probably don't have!
If you have any questions please open an issue ticket!

## Usage

Add this to your build.gradle.kts
```kotlin
dependencies {
    implementation("de.inoxio:spring-cloudwatch-metrics:0.1.0")
}
```

Inject the CloudwatchDAO from your service as usual:

```jshelllanguage
private final CloudwatchDAO cloudwatchDAO;

@Autowired
public SomeServiceImpl(final CloudwatchDAO cloudwatchDAO) {
    notNull(cloudwatchDAO, "CloudwatchDAO must not be null!");
    this.cloudwatchDAO = cloudwatchDAO;
}

/**
* To push used heap memory and cpu usage to cloudwatch 
* have a method like this in your service that calls
* the CloudwatchDAO. Should be invoked from some scheduler.
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

```

Add the following properties to your project:
```yaml
aws:
  cluster-name: some-cluster-name      # Metrics will be added as a dimension of this 
  namespace: Namespace                 # Namespace where the metrics will be pushed to
  metric-prefix: AppPrefix             # All Metrics will get this prefix. Final name will be AppPrefixHeapMemoryUsed
  report-server-start: true            # Should all graphs with AppPrefixMetrics be annotated
```
