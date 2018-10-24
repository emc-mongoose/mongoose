# Monitoring API
For real-time monitoring the metrics are exposed in the [Prometheus's](https://github.com/prometheus/client_java) format.
### Configuring port
To configure the port for the server, the parameter `--run-port` is used. By default `--run-port=9999`.
### Output format
Information about new metric starts with
`````
# HELP <metric name>
# TYPE <metric name> gauge
`````
*where `TYPE <metric name> gauge` is the prometheus type of metrics.*

There are 7 metrics: 
- duration, 
- latency, 
- concurrency, 
- successful operation count, 
- faild operation count, 
- transfered size in bytes (BYTES)
- elapsed time.

and 2 Primitive Types: Timing and Rate. Depends on the type of metric, which aggregation types are exported. The table below provides a description:
  
  <table>
    <thead>
        <tr>
            <th>Metric name</th>
            <th>Primitive type</th>
            <th>Aggregation types</th>
        </tr>
    </thead>
    <tbody>
        <tr>
            <td>Duration</td>
            <td rowspan=3>Timing</td>
            <td rowspan=3> <ul><li>count<li>sum<li>mean<li>min<li>max<li>quntile_'value' <a href="https://github.com/emc-mongoose/mongoose/blob/BASE-1271-move-namespace-option/doc/interfaces/api/monitoring/README.md#quantiles"> (configured)<ul> </td>
        </tr>
        <tr>
            <td>Latency</td>
        </tr>
        <tr>
            <td>Concurrency</td>
        </tr>
        <tr>
            <td>Bytes</td>
            <td rowspan=3>Rate</td>
            <td rowspan=3> <ul><li>count<li>sum<li>meanRate<li>lastRate<ul> </td>
        </tr>
         <tr>
            <td>Success</td>
        </tr>
        <tr>
            <td>Fails</td>
        </tr>
        <tr>
            <td>Elapsed time</td>
            <td>Gauge</td>
            <td>value</td>
        </tr>
    </tbody>
</table>

#### Quantiles
To specify the value of the required quantiles, use the `--output-metrics-quantiles` parameter. By default `--output-metrics-quantiles=0.25,0.75`. *This feature affects the output on the server and does not affect the logs and console.*

#### Labels
In braces exported load step parameters:

|Label name|Configured param|Type|
|:---|:---|---|
|`STEP_ID`|load-step-id|string|
|`OP_TYPE`|load-op-type|string, [takes one of these values](https://github.com/emc-mongoose/mongoose/tree/master/doc/usage/load/operations/types#load-operation-types)|
|`CONCURRENCY`|driver-limit-concurrency|integer|
|`NODE_COUNT`|*count of addrs in* load-step-node-addrs|integer|
|`ITEM_DATA_SIZE`|item-data-size|string with the unit suffix (KB, MB, ...)|

#### Example:

````````````````````````````````
# HELP DURATION 
# TYPE DURATION gauge
DURATION_count{STEP_ID="ExposedMetricsTest",OP_TYPE="CREATE",CONCURRENCY="0",NODE_COUNT="1",ITEM_DATA_SIZE="10KB",} 10.0
DURATION_sum{STEP_ID="ExposedMetricsTest",OP_TYPE="CREATE",CONCURRENCY="0",NODE_COUNT="1",ITEM_DATA_SIZE="10KB",} 1.1E7
DURATION_mean{STEP_ID="ExposedMetricsTest",OP_TYPE="CREATE",CONCURRENCY="0",NODE_COUNT="1",ITEM_DATA_SIZE="10KB",} 1100000.0
DURATION_min{STEP_ID="ExposedMetricsTest",OP_TYPE="CREATE",CONCURRENCY="0",NODE_COUNT="1",ITEM_DATA_SIZE="10KB",} 1100000.0
DURATION_max{STEP_ID="ExposedMetricsTest",OP_TYPE="CREATE",CONCURRENCY="0",NODE_COUNT="1",ITEM_DATA_SIZE="10KB",} 1100000.0
# HELP ELAPSED_TIME 
# TYPE ELAPSED_TIME gauge
ELAPSED_TIME_value{STEP_ID="ExposedMetricsTest",OP_TYPE="CREATE",CONCURRENCY="0",NODE_COUNT="1",ITEM_DATA_SIZE="10KB",} 11155.0
...
``````````````````````````````````````````````````
