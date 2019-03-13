# Content

1. [Introduction](#1-introduction)<br/>
2. [Limitations](#2-limitations)<br/>
3. [Requirements](#3-requirements)<br/>
4. [Approach](#4-approach)<br/>
4.1. [Integrations](#41-integrations)<br/>
4.2. [API](#42-api)<br/>
4.2.1. [Config](#421-config)<br/>
4.2.2. [Run](#422-run)<br/>
4.2.3. [Logs](#423-logs)<br/>
4.2.3.1. [Available log names](#4231-available-log-names)<br/>
4.2.3.2. [Get the log file from the beginning](#4232-get-the-log-file-from-the-beginning)<br/>
4.2.3.3. [Get the specified log file part](#4233-get-the-specified-log-file-part)<br/>
4.2.3.4. [Delete the log file](#4234-delete-the-log-file)<br/>
4.2.4. [Metrics](#424-metrics)<br/>
5. [Configuration](#5-configuration)<br/>
6. [Output](#6-output)<br/>
6.1. [Metrics](#61-metrics)<br/>
6.1.1. [Custom quantiles](#611-custom-quantiles)<br/>
6.1.2. [Labels](#612-labels)<br/>

# 1. Introduction

The specific remote APIs are required to build the full-featured storage performance testing services on top of
Mongoose. The application may be a monitoring system either control UI.

# 2. Limitations

| # | Description |
|:--|:------------|
| 2.1 | Run mode should be "node" instead of default ("interactive"). See the [Configuration](#5-configuration) section for the details

# 3. Requirements

| # | Description |
|:--|-------------|
| 3.1 | A remote API user should be able to fetch aggregated configuration defaults from the Mongoose node
| 3.2 | A remote API user should be able to fetch the aggregated configuration schema from the Mongoose node
| 3.3 | A remote API user should be able to run a new scenario on the Mongoose node
| 3.4 | A remote API user should be able to stop the running scenario on the Mongoose node
| 3.5 | A remote API user should be able to determine if the Mongoose node is running a scenario or not
| 3.6 | A remote API user should be able to identify the scenario running on the Mongoose node
| 3.7 | A remote API user should be able to fetch the log file content from the Mongoose node
| 3.8 | A remote API user should be able to fetch the current metrics in the Prometheus export format from the Mongoose node

# 4. Approach

## 4.1. Integrations

To serve the Remote API the following libraries are used:
* [Jetty](https://www.eclipse.org/jetty/) to serve the HTTP requests
* [Prometheus instrumentation](https://github.com/prometheus/client_java) library to export the metrics

## 4.2. API

> See the full documentation [here](https://app.swaggerhub.com/apis/veronikaKochugova/Mongoose/4.1.1)

### 4.2.1. Config

Get config from node:
```bash
curl GET http://localhost:9999/config
```
> More about configuration [here](../../input/configuration)

Get schema from node:
```bash
curl GET http://localhost:9999/config/schema
```
> The schema relates configuration parameters to the required types.

### 4.2.2. Run

Start a new scenario run:
```bash
curl -v -X POST \
    -F defaults=@base/src/test/robot/api/remote/data/aggregated_defaults.yaml \
    -F scenario=@base/src/test/robot/api/remote/data/scenario_dummy.js \
    http://localhost:9999/run
```

It's possible to omit the `defaults` and `scenario` parts (default ones may be used):
```bash
curl -v -X POST http://localhost:9999/run
```

Also, the partial defaults configuration may be supplied too:
```bash
curl -v -X POST \
    -H "Content-Type:multipart/form-data" \
    -F "defaults=storage:{driver:{type:\"dummy-mock\"}}};type=application/yaml" \
    http://localhost:9999/run
```
> **Note**: use this example above as the most simple way to start via the remote API.

If successful, the response will contain the ETag header with the hexadecimal timestamp (Unix epoch time):
```bash
...
< HTTP/1.1 202 Accepted
< Date: Mon, 26 Nov 2018 18:35:50 GMT
< ETag: 167514e6082
< Content-Length: 0
...
```

This ETag should be considered as a run id and may be used to check the run state (using HEAD/GET request) either stop
it (using DELETE request).

Checking if the given node executes a scenario:
```bash
curl -v -X HEAD http://localhost:9999/run
...
< HTTP/1.1 200 OK
< Date: Mon, 26 Nov 2018 18:40:10 GMT
< ETag: 167514e6082
< Content-Length: 0
...
```

The `If-Match` header with the hexadecimal run id value may be used also:

Checking the run state:
```bash
curl -v -X GET -H "If-Match: 167514e6082" http://localhost:9999/run
...
< HTTP/1.1 200 OK
< Date: Mon, 26 Nov 2018 18:40:10 GMT
< Content-Length: 0
...
```

Stopping the run:
```bash
curl -v -X DELETE -H "If-Match: 167514e6082" http://localhost:9999/run
...
< HTTP/1.1 200 OK
< Date: Mon, 26 Nov 2018 18:41:26 GMT
< Content-Length: 0
```

### 4.2.3. Logs

#### 4.2.3.1. Available Log Names

| Log Name | Purpose |
|:--|:--|
| Cli | Command line arguments dump
| Config | Full load step configuration dump
| Errors | Error messages
| OpTraces | Load operation traces (transfer byte count, latency, duration, etc)
| metrics.File | Load step periodic metrics
| metrics.FileTotal | Load step total metrics log
| metrics.threshold.File | Load step periodic threshold metrics
| metrics.threshold.FileTotal | Load step total threshold metrics log
| Messages | Generic messages
| Scenario | Scenario dump

#### 4.2.3.2. Get The Log File Page From The Beginning

```bash
curl http://localhost:9999/logs/123/Messages
```

#### 4.2.3.3. Get The Specified Log File Part

```bash
curl -H "Range: bytes=100-200" http://localhost:9999/logs/123/Messages
r the type "dummy-mock"
2018-11-27T16:19:34,982 | DEBUG | LinearLoadStepClient | main | com.emc.mongoose.storage.driver.mock.DummyStorageDriverMock@6aecbb8d: shut down
2018-11-27T16:19:34,982 | DEBUG |
```

#### 4.2.3.4. Delete The Log File

```bash
curl -X DELETE http://localhost:9999/logs/123/Messages
```

### 4.2.4 Metrics

For real-time monitoring the metrics are exposed in the [Prometheus's](https://github.com/prometheus/client_java) format.

Example using the command:
```bash
curl http://localhost:9999/metrics
```

->

```
# HELP mongoose_duration 
# TYPE mongoose_duration gauge
mongoose_duration_count{load_step_id="linear_20190304.123915.606",load_op_type="READ",storage_driver_limit_concurrency="1",item_data_size="1MB",start_time="1551703155695",node_list="[]",user_comment="",} 559.0
mongoose_duration_sum{load_step_id="linear_20190304.123915.606",load_op_type="READ",storage_driver_limit_concurrency="1",item_data_size="1MB",start_time="1551703155695",node_list="[]",user_comment="",} 0.083571
mongoose_duration_mean{load_step_id="linear_20190304.123915.606",load_op_type="READ",storage_driver_limit_concurrency="1",item_data_size="1MB",start_time="1551703155695",node_list="[]",user_comment="",} 1.4950089445438282E-4
mongoose_duration_min{load_step_id="linear_20190304.123915.606",load_op_type="READ",storage_driver_limit_concurrency="1",item_data_size="1MB",start_time="1551703155695",node_list="[]",user_comment="",} 2.0E-6
mongoose_duration_quantile_0_25{load_step_id="linear_20190304.123915.606",load_op_type="READ",storage_driver_limit_concurrency="1",item_data_size="1MB",start_time="1551703155695",node_list="[]",user_comment="",} 2.0E-6
mongoose_duration_quantile_0_5{load_step_id="linear_20190304.123915.606",load_op_type="READ",storage_driver_limit_concurrency="1",item_data_size="1MB",start_time="1551703155695",node_list="[]",user_comment="",} 6.0E-6
mongoose_duration_quantile_0_75{load_step_id="linear_20190304.123915.606",load_op_type="READ",storage_driver_limit_concurrency="1",item_data_size="1MB",start_time="1551703155695",node_list="[]",user_comment="",} 9.0E-6
mongoose_duration_max{load_step_id="linear_20190304.123915.606",load_op_type="READ",storage_driver_limit_concurrency="1",item_data_size="1MB",start_time="1551703155695",node_list="[]",user_comment="",} 0.011517
# HELP mongoose_latency 
# TYPE mongoose_latency gauge
mongoose_latency_count{load_step_id="linear_20190304.123915.606",load_op_type="READ",storage_driver_limit_concurrency="1",item_data_size="1MB",start_time="1551703155695",node_list="[]",user_comment="",} 559.0
mongoose_latency_sum{load_step_id="linear_20190304.123915.606",load_op_type="READ",storage_driver_limit_concurrency="1",item_data_size="1MB",start_time="1551703155695",node_list="[]",user_comment="",} 0.029502
mongoose_latency_mean{load_step_id="linear_20190304.123915.606",load_op_type="READ",storage_driver_limit_concurrency="1",item_data_size="1MB",start_time="1551703155695",node_list="[]",user_comment="",} 5.2776386404293386E-5
mongoose_latency_min{load_step_id="linear_20190304.123915.606",load_op_type="READ",storage_driver_limit_concurrency="1",item_data_size="1MB",start_time="1551703155695",node_list="[]",user_comment="",} 1.0E-6
mongoose_latency_quantile_0_25{load_step_id="linear_20190304.123915.606",load_op_type="READ",storage_driver_limit_concurrency="1",item_data_size="1MB",start_time="1551703155695",node_list="[]",user_comment="",} 1.0E-6
mongoose_latency_quantile_0_5{load_step_id="linear_20190304.123915.606",load_op_type="READ",storage_driver_limit_concurrency="1",item_data_size="1MB",start_time="1551703155695",node_list="[]",user_comment="",} 1.0E-6
mongoose_latency_quantile_0_75{load_step_id="linear_20190304.123915.606",load_op_type="READ",storage_driver_limit_concurrency="1",item_data_size="1MB",start_time="1551703155695",node_list="[]",user_comment="",} 5.0E-6
mongoose_latency_max{load_step_id="linear_20190304.123915.606",load_op_type="READ",storage_driver_limit_concurrency="1",item_data_size="1MB",start_time="1551703155695",node_list="[]",user_comment="",} 0.011512
# HELP mongoose_concurrency 
# TYPE mongoose_concurrency gauge
mongoose_concurrency_mean{load_step_id="linear_20190304.123915.606",load_op_type="READ",storage_driver_limit_concurrency="1",item_data_size="1MB",start_time="1551703155695",node_list="[]",user_comment="",} 0.0
mongoose_concurrency_last{load_step_id="linear_20190304.123915.606",load_op_type="READ",storage_driver_limit_concurrency="1",item_data_size="1MB",start_time="1551703155695",node_list="[]",user_comment="",} 0.0
# HELP mongoose_byte 
# TYPE mongoose_byte gauge
mongoose_byte_count{load_step_id="linear_20190304.123915.606",load_op_type="READ",storage_driver_limit_concurrency="1",item_data_size="1MB",start_time="1551703155695",node_list="[]",user_comment="",} 2.628929978368E12
mongoose_byte_rate_mean{load_step_id="linear_20190304.123915.606",load_op_type="READ",storage_driver_limit_concurrency="1",item_data_size="1MB",start_time="1551703155695",node_list="[]",user_comment="",} 8.763099927893334E11
mongoose_byte_rate_last{load_step_id="linear_20190304.123915.606",load_op_type="READ",storage_driver_limit_concurrency="1",item_data_size="1MB",start_time="1551703155695",node_list="[]",user_comment="",} 7.021527038085463E11
# HELP mongoose_success_op 
# TYPE mongoose_success_op gauge
mongoose_success_op_count{load_step_id="linear_20190304.123915.606",load_op_type="READ",storage_driver_limit_concurrency="1",item_data_size="1MB",start_time="1551703155695",node_list="[]",user_comment="",} 2507143.0
mongoose_success_op_rate_mean{load_step_id="linear_20190304.123915.606",load_op_type="READ",storage_driver_limit_concurrency="1",item_data_size="1MB",start_time="1551703155695",node_list="[]",user_comment="",} 835714.3333333334
mongoose_success_op_rate_last{load_step_id="linear_20190304.123915.606",load_op_type="READ",storage_driver_limit_concurrency="1",item_data_size="1MB",start_time="1551703155695",node_list="[]",user_comment="",} 669624.9998174155
# HELP mongoose_failed_op 
# TYPE mongoose_failed_op gauge
mongoose_failed_op_count{load_step_id="linear_20190304.123915.606",load_op_type="READ",storage_driver_limit_concurrency="1",item_data_size="1MB",start_time="1551703155695",node_list="[]",user_comment="",} 0.0
mongoose_failed_op_rate_mean{load_step_id="linear_20190304.123915.606",load_op_type="READ",storage_driver_limit_concurrency="1",item_data_size="1MB",start_time="1551703155695",node_list="[]",user_comment="",} 0.0
mongoose_failed_op_rate_last{load_step_id="linear_20190304.123915.606",load_op_type="READ",storage_driver_limit_concurrency="1",item_data_size="1MB",start_time="1551703155695",node_list="[]",user_comment="",} 0.0
# HELP mongoose_elapsed_time 
# TYPE mongoose_elapsed_time gauge
mongoose_elapsed_time_value{load_step_id="linear_20190304.123915.606",load_op_type="READ",storage_driver_limit_concurrency="1",item_data_size="1MB",start_time="1551703155695",node_list="[]",user_comment="",} 3.778
```

# 5. Configuration

| Option | Type | Default Value | Description
|:--|:--|:--|:--|
| output-metrics-quantiles | List of numbers each in the range (0; 1] | [0.25,0.5,0.75] | The quantile values to calculate and report for the timing metrics (duration/latency)
| run-node | Boolean | `false` | Run in the mode node. Should be enabled to serve the Remote API
| run-port | Integer in the range (0; 65536) | 9999 | The port to listen the Remote API requests

# 6. Output

## 6.1. Metrics

Format of the metric name : `<app name>_<metric name>_<agrigation type>`.
All metric units are using [SI](https://prometheus.io/docs/practices/naming/#base-units).

There metrics being exposed:
* duration
* latency
* concurrency
* successful operation count
* failed operation count
* transferred size in bytes (BYTE)
* elapsed time

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
            <td rowspan=2> <ul><li>count<li>sum<li>mean<li>min<li>max<li>quntile_'value' (<a href="#quantiles">configured</a>)<ul> </td>
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

### 6.1.1. Custom Quantiles
To specify the value of the required quantiles, use the `--output-metrics-quantiles` parameter.
By default `output-metrics-quantiles=[0.25,0.5,0.75]`.

### 6.1.2. Labels
Each metric contains also the following labels/tags:

|Label name|Configured param|Type|
|:---|:---|---|
|`load_step_id`|load-step-id|string|
|`load_op_type`|load-op-type|string, [takes one of these values](doc/usage/load/operations/types#load-operation-types)|
|`storage_driver_limit_concurrency`|storage-driver-limit-concurrency|integer|
|`node_count`|the count of the Mongoose nodes involved into the given load step|integer|
|`item_data_size`|item-data-size|string with the unit suffix (KB, MB, ...)|
