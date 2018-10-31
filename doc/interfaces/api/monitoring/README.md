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

and 3 Primitive Types: Timing, Rate, Concurrency. Depends on the type of metric, which aggregation types are exported. The table below provides a description:
  
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
            <td rowspan=2>Timing</td>
            <td rowspan=2> <ul><li>count<li>sum<li>mean<li>min<li>max<li>quntile_'value' <a href="https://github.com/emc-mongoose/mongoose/blob/BASE-1271-move-namespace-option/doc/interfaces/api/monitoring/README.md#quantiles"> (configured)<ul> </td>
        </tr>
        <tr>
            <td>Latency</td>
        </tr>
        <tr>
            <td>Concurrency</td>
            <td>Concurrency</td>
            <td><ul><li>mean<li>last</td>
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
|`load_step_id`|load-step-id|string|
|`load_op_type`|load-op-type|string, [takes one of these values](https://github.com/emc-mongoose/mongoose/tree/master/doc/usage/load/operations/types#load-operation-types)|
|`storage_driver_limit_concurrency`|storage-driver-limit-concurrency|integer|
|`node_count`|*count of addrs in* load-step-node-addrs|integer|
|`item_data_sizw`|item-data-size|string with the unit suffix (KB, MB, ...)|

### Usage

To view the metrics you need to start Mongoose and go to the `localhost:<run-port>`.

#### Example:

````````````````````````````````
# HELP duration
# TYPE duration gauge
duration_count{load_step_id="ExposedMetricsTest",load_op_type="CREATE",storage_driver_limit_concurrency="0",node_count="1",item_data_size="10KB",} 10.0
duration_sum{load_step_id="ExposedMetricsTest",load_op_type="CREATE",storage_driver_limit_concurrency="0",node_count="1",item_data_size="10KB",} 1.1E7
duration_mean{load_step_id="ExposedMetricsTest",load_op_type="CREATE",storage_driver_limit_concurrency="0",node_count="1",item_data_size="10KB",} 1100000.0
duration_min{load_step_id="ExposedMetricsTest",load_op_type="CREATE",storage_driver_limit_concurrency="0",node_count="1",item_data_size="10KB",} 1100000.0
duration_quantile_0.25{load_step_id="ExposedMetricsTest",load_op_type="CREATE",storage_driver_limit_concurrency="0",node_count="1",item_data_size="10KB",} 1100000.0
duration_quantile_0.5{load_step_id="ExposedMetricsTest",load_op_type="CREATE",storage_driver_limit_concurrency="0",node_count="1",item_data_size="10KB",} 1100000.0
duration_quantile_0.75{load_step_id="ExposedMetricsTest",load_op_type="CREATE",storage_driver_limit_concurrency="0",node_count="1",item_data_size="10KB",} 1100000.0
duration_max{load_step_id="ExposedMetricsTest",load_op_type="CREATE",storage_driver_limit_concurrency="0",node_count="1",item_data_size="10KB",} 1100000.0
# HELP latency
# TYPE latency gauge
latency_count{load_step_id="ExposedMetricsTest",load_op_type="CREATE",storage_driver_limit_concurrency="0",node_count="1",item_data_size="10KB",} 10.0
latency_sum{load_step_id="ExposedMetricsTest",load_op_type="CREATE",storage_driver_limit_concurrency="0",node_count="1",item_data_size="10KB",} 1.0E7
latency_mean{load_step_id="ExposedMetricsTest",load_op_type="CREATE",storage_driver_limit_concurrency="0",node_count="1",item_data_size="10KB",} 1000000.0
latency_min{load_step_id="ExposedMetricsTest",load_op_type="CREATE",storage_driver_limit_concurrency="0",node_count="1",item_data_size="10KB",} 1000000.0
latency_quantile_0.25{load_step_id="ExposedMetricsTest",load_op_type="CREATE",storage_driver_limit_concurrency="0",node_count="1",item_data_size="10KB",} 1000000.0
latency_quantile_0.5{load_step_id="ExposedMetricsTest",load_op_type="CREATE",storage_driver_limit_concurrency="0",node_count="1",item_data_size="10KB",} 1000000.0
latency_quantile_0.75{load_step_id="ExposedMetricsTest",load_op_type="CREATE",storage_driver_limit_concurrency="0",node_count="1",item_data_size="10KB",} 1000000.0
latency_max{load_step_id="ExposedMetricsTest",load_op_type="CREATE",storage_driver_limit_concurrency="0",node_count="1",item_data_size="10KB",} 1000000.0
# HELP concurrency
# TYPE concurrency gauge
concurrency_mean{load_step_id="ExposedMetricsTest",load_op_type="CREATE",storage_driver_limit_concurrency="0",node_count="1",item_data_size="10KB",} 1.0
concurrency_last{load_step_id="ExposedMetricsTest",load_op_type="CREATE",storage_driver_limit_concurrency="0",node_count="1",item_data_size="10KB",} 1.0
# HELP transferred_bytes
# TYPE transferred_bytes gauge
transferred_bytes_count{load_step_id="ExposedMetricsTest",load_op_type="CREATE",storage_driver_limit_concurrency="0",node_count="1",item_data_size="10KB",} 102400.0
transferred_bytes_rate_mean{load_step_id="ExposedMetricsTest",load_op_type="CREATE",storage_driver_limit_concurrency="0",node_count="1",item_data_size="10KB",} 11377.777777777777
transferred_bytes_rate_last{load_step_id="ExposedMetricsTest",load_op_type="CREATE",storage_driver_limit_concurrency="0",node_count="1",item_data_size="10KB",} 10240.0
# HELP operations_successful
# TYPE operations_successful gauge
operations_successful_count{load_step_id="ExposedMetricsTest",load_op_type="CREATE",storage_driver_limit_concurrency="0",node_count="1",item_data_size="10KB",} 10.0
operations_successful_rate_mean{load_step_id="ExposedMetricsTest",load_op_type="CREATE",storage_driver_limit_concurrency="0",node_count="1",item_data_size="10KB",} 1.1111111111111112
operations_successful_rate_last{load_step_id="ExposedMetricsTest",load_op_type="CREATE",storage_driver_limit_concurrency="0",node_count="1",item_data_size="10KB",} 1.0
# HELP operations_failed
# TYPE operations_failed gauge
operations_failed_count{load_step_id="ExposedMetricsTest",load_op_type="CREATE",storage_driver_limit_concurrency="0",node_count="1",item_data_size="10KB",} 10.0
operations_failed_rate_mean{load_step_id="ExposedMetricsTest",load_op_type="CREATE",storage_driver_limit_concurrency="0",node_count="1",item_data_size="10KB",} 1.1111111111111112
operations_failed_rate_last{load_step_id="ExposedMetricsTest",load_op_type="CREATE",storage_driver_limit_concurrency="0",node_count="1",item_data_size="10KB",} 1.0
# HELP elapsed_time
# TYPE elapsed_time gauge
elapsed_time_value{load_step_id="ExposedMetricsTest",load_op_type="CREATE",storage_driver_limit_concurrency="0",node_count="1",item_data_size="10KB",} 11544.0
``````````````````````````````````````````````````
