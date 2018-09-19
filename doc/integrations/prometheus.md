# Prometheus Integration

https://prometheus.io/

## Deploy

TODO

## Usage

TODO

## Approach

Mongoose should be configured specifically to make it pushing the metrics into the Prometheus TSDB:

1. Mongoose should be started with additional JVM options:
```
-Dcom.sun.management.jmxremote
-Dcom.sun.management.jmxremote.port=9010
-Dcom.sun.management.jmxremote.rmi.port=9010
-Dcom.sun.management.jmxremote.local.only=false
-Dcom.sun.management.jmxremote.authenticate=false
-Dcom.sun.management.jmxremote.ssl=false
```
2. The correct Prometheus JMX exporter config file should be provided for the Mongoose instance.
The config file example content (obsolete, valid for Mongoose v3.5.1):
```yaml
---
ssl: false
whitelistObjectNames: [ "com.emc.mongoose.api.model.metrics:*" ]
rules:
  - pattern: "com.emc.mongoose.model.metrics<storageDriverConcurrency=(\\d+), storageDriverCount=(\\d+), loadType=(\\w+), stepId=(.+)><>(byte_count|duration_sum|elapsed_time_millis|fail_count|latency_sum|succ_count)"
    attrNameSnakeCase: true
    name: "mongoose_$5"
    type: COUNTER
    labels:
      storage_driver_concurrency: "$1"
      storage_driver_count: "$2"
      load_type: "$3"
      test_step_id: "$4"
  - pattern: "com.emc.mongoose.model.metrics<storageDriverConcurrency=(\\d+), storageDriverCount=(\\d+), loadType=(\\w+), stepId=(.+)><>(byte_rate_last|byte_rate_mean|duration_hi_q|duration_lo_q|duration_max|duration_mean|duration_med|duration_min|fail_rate_last|fail_rate_mean|latency_hi_q|latency_lo_q|latency_max|latency_mean|latency_med|latemcy_min|start_time_millis|succ_rate_last|succ_rate_mean)"
    attrNameSnakeCase: true
    name: "mongoose_$5"
    type: GAUGE
    labels:
      storage_driver_concurrency: "$1"
      storage_driver_count: "$2"
      load_type: "$3"
      test_step_id: "$4"
```
3. Prometheus JMX exporter should be started for the Mongoose instance:
```bash
java -jar jmx_prometheus_httpserver-0.10-jar-with-dependencies.jar 9280 <JMX_EXPORTER_CONFIG_FILE>
```
The jar for the JMX exporter is available at:
https://repo1.maven.org/maven2/io/prometheus/jmx/jmx_prometheus_httpserver/0.10/jmx_prometheus_httpserver-0.10.jar
