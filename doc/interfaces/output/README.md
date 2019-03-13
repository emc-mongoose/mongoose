# Contents

1. [General](#1-general)<br/>
&nbsp;&nbsp;1.1. [Logging Subsystem](#11-logging-subsystem)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;1.1.1. [Load Step Id](#111-load-step-id)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;1.1.2. [Console](#112-console)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;1.1.3. [Files](#113-files)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;1.1.4. [Log configuration](#114-log-configuration)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;1.1.4.1. [Docker Custom Logging](#1141-docker-custom-logging)<br/> 
&nbsp;&nbsp;1.2. [Categories](#12-categories)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;1.2.1. [CLI Arguments](#121-cli-arguments)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;1.2.2. [Configuration Dump](#122-configuration-dump)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;1.2.3. [Scenario Dump](#123-scenario-dump)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;1.2.4. [3rd Party Log Messages](#124-3rd-party-log-messages)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;1.2.5. [Error Messages](#125-error-messages)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;1.2.6. [General Messages](#126-general-messages)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;1.2.7. [Item List Files](#127-item-list-files)<br/>
2. [Metrics](#2-metrics)<br/>
&nbsp;&nbsp;2.1. [Load Average](#21-load-average)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;2.1.1. [Console](#211-console)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;2.1.1.1. [Table Fields Description](#2111-table-fields-description)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;2.1.2. [File](#212-files)<br/>
&nbsp;&nbsp;2.2. [Load Step Summary](#22-load-step-summary)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;2.2.1. [Console](#221-console)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;2.2.2. [File](#222-files)<br/>
&nbsp;&nbsp;2.3. [Operation Traces](#23-operation-traces)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;2.3.1. [Console](#231-console)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;2.3.2. [File](#232-files)<br/>
&nbsp;&nbsp;2.4. [Threshold](#24-threshold)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;2.4.1. [Console](#241-console)<br/>
&nbsp;&nbsp;&nbsp;&nbsp;2.4.2. [File](#242-files)<br/>

# 1. General

## 1.1. Logging Subsystem

Mongoose uses the [Apache Log4J2](http://logging.apache.org/log4j/2.x/) library to handle the
overwhelming majority of its output due to
[high performance capabilities](http://logging.apache.org/log4j/2.x/performance.html).

The Log4J2 configuration is bundled into the `mongoose.jar` resources as `log4j2.yaml`. Also the
`log4j2.component.properties` file bundled into that jar to specify the additional options tuning the logging subsystem
performance.

### 1.1.1 Load Step Id

The load step id is used to differentiate the log messages and metrics. If step id is not set, it's generated
automatically using the following pattern: `<STEP_TYPE>-<yyyyMMdd.HHmmss.SSS>` There's also the special prefix "none-"
which used instead of a step type if the message is logged out of any load step context.

### 1.1.2 Console

Console output is slightly colored by default for better readability.
To disable the console output coloring set the `output-color` configuration option to "false".

### 1.1.3. Files

The most of log messages are written to the output files using dynamic output file path:
`<MONGOOSE_DIR>/log/<STEP_ID>/...` where "STEP_ID" may change during runtime.

### 1.1.4. Log configuration

In order to configure logging, can be used custom log4j2 config. This may be necessary to disable warnings or logging certain files.
Use parameter `Dlog4j.configurationFile` for this:
```bash
java -Dlog4j.configurationFile=/path/to/custom/config/log4j2.yaml -jar mongoose-<VERSION>.jar
```
An example of a default configuration [here](base/src/main/resources/log4j2.yaml).

##### 1.1.4.1. Docker Custom Logging

The custom docker image should be built with the modified *entrypoint* for custom logging configuration. The additional property
`-Dlog4j.configurationFile=/path/in/container/custom-log4j2.yaml` should be added:

```bash
#!/bin/sh
umask 0000
export JAVA_HOME=/opt/mongoose
export PATH=/usr/local/sbin:/usr/local/bin:/usr/sbin:/usr/bin:/sbin:/bin:${JAVA_HOME}/bin
java -Dlog4j.configurationFile=/path/in/container/custom-log4j2.yaml -jar /opt/mongoose/mongoose.jar "$@"
```

> default [entrypoint](docker/entrypoint.sh) 

and run container with following command:

```bash
docker run \
    -ti -v /path/on/host/custom-log4j2config:/path/in/container/custom-log4j2.yaml \
    -v /path/on/host/custom-entrypoint.sh:/path/in/container/custom-entrypoint.sh \
    --entrypoint /path/in/container/custom-entrypoint.sh \
    --network host \
    emcmongoose/mongoose[-<TYPE>] [\
    <ARGS>]
```


## 1.2. Categories

### 1.2.1. CLI Arguments

The CLI arguments are logged once per run into the `cli.args.log` log
file.

### 1.2.2. Configuration Dump

The configuration (defaults after the CLI arguments applied) is logged
once per run into the `config.yaml` log file.

### 1.2.3. Scenario Dump

The scenario used for the test is logged once per run into the
`scenario.txt` log file.

### 1.2.4. 3rd Party Log Messages

The log messages from the 3rd party components (dependency libraries) are intercepted and logged
into the `3rdparty.log` file with the *dynamic path*. Logging level: "INFO".

### 1.2.5. Error Messages

* Error messages with level "INFO" or higher are displayed on the console with orange color.
* Error messages with level "DEBUG" or higher are written to the file `errors.log` with the
  *dynamic path*

### 1.2.6. General Messages

* Info messages with level "INFO" or higher are displayed on the console with grey color.
* Info messages with level "DEBUG" or higher are written to the file `messages.log` with the
  *dynamic path*

### 1.2.7. Item List Files

To persist the info about the items processed by a load step the items output file should be used.

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --load-op-limit-count=1000 --item-output-file=items.csv
```
In the example above the info about 1000 items processed by the load step will be persisted in the
`items.csv` output file.

The items list file may be useful if it's needed to perform another load step using these items:
```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --read --item-input-file=items.csv
```

Items list file contains the CSV records each occupying a line.
Each record contains comma-separated values which are:
1. Full Item Path
2. Data ring buffer offset (hexadecimal)
3. Size (decimal)
4. A pair of [layer/mask](../design/data_reentrancy.md) values separated with "/" character

# 2. Metrics

## 2.1. Load Average

Load average metrics records are thought to be produced periodically to monitor the load step state
in the nearly real time mode. The producing period is configurable (`output-metrics-average-period`)
and the default value is "10s" (10 seconds). Setting this period to 0 will disable the load average
metrics output at all.

### 2.1.1. Console

The average metrics are displayed in the console in the form of ASCII table for better readability.
The header is displayed each 20 rows by default. Use the
`output-metrics-average-table-header-period` configuration parameter to set the custom value.

Table output example:
```
------------------------------------------------------------------------------------------------------------------------
 Step Id  | Timestamp  |  Op  |     Concurrency     |       Count       | Step  |   Last Rate    |  Mean    |   Mean
 (last 10 |            | type |---------------------|-------------------| Time  |----------------| Latency  | Duration
 symbols) |yyMMddHHmmss|      | Current  |   Mean   |   Success  |Failed|  [s]  | [op/s] |[MB/s] |  [us]    |   [us]
----------|------------|------|----------|----------|------------|------|-------|--------|-------|----------|-----------
1881901842|170824183431|CREATE|         0|0.0       |           0|     0|0.011  |0.0     |0.0    |         0|          0
1881901842|170824183441|CREATE|        29|0.7765    |          23|     0|10.033 |1.060711|106.071|    571575|    2437084
1881901842|170824183451|CREATE|        62|42.805    |          98|     0|20.022 |5.304414|530.441|    400584|    1913174
1881901842|170824183501|CREATE|        84|70.379    |         160|     0|30.031 |5.898923|589.892|    533951|    2306146
1881901842|170824183511|CREATE|       423|220.20    |         203|     0|40.033 |4.353079|435.307|    520783|    2337984
1881901842|170824183521|CREATE|       435|430.03    |         233|     0|50.033 |3.720354|372.035|    532562|    3428059
1881901842|170824183531|CREATE|       417|433.73    |         477|     0|60.033 |18.34146|1834.14|    620557|   11863247
1881901842|170824183541|CREATE|       459|456.62    |         581|     0|70.033 |12.48209|1248.20|    614247|   13671047
1881901842|170824183551|CREATE|       467|466.64    |         747|     0|80.032 |14.98322|1498.32|    657974|   14921506
1881901842|170824183601|CREATE|       481|475.94    |         940|     0|90.033 |17.02162|1702.16|    694843|   16494300
1881901842|170824183611|CREATE|       493|485.12    |        1066|     0|100.033|14.30267|1430.26|    795762|   16876532
1881901842|170824183621|CREATE|       510|495.63    |        1225|     0|110.03 |15.00818|1500.81|    775649|   17780188
1881901842|170824183631|CREATE|       523|509.71    |        1406|     0|120.034|17.27814|1727.81|    757483|   18318582
1881901842|170824183641|CREATE|       528|523.41    |        1545|     0|130.034|14.63165|1463.16|    749247|   18427227
1881901842|170824183651|CREATE|       543|531.80    |        1716|     0|140.036|15.76763|1576.76|    735830|   18708690
1881901842|170824183701|CREATE|       550|545.41    |        1864|     0|150.035|14.85324|1485.32|    740029|   19060149
1881901842|170824183711|CREATE|       569|551.75    |        2030|     0|160.035|15.74449|1574.44|    735227|   19254647
1881901842|170824183721|CREATE|       582|568.95    |        2165|     0|170.035|13.86718|1386.71|    733804|   19512344
1881901842|170824183731|CREATE|       585|577.05    |        2270|     0|180.039|11.65720|1165.72|    754406|   19747777
```

#### 2.1.1.1. Table Fields Description

Field Name            | Description
----------------------|------------
Step Name             | The configured step name. Automatic value is used if not configured obviously. Note that only last 10 characters are displayed in the table.
Timestamp             | The datetime of the record output in the "yyMMddHHmmss" format (year is specified by the 2 last digits)
Op Type               | Load operation type. Colored for readability.
Concurrency / Current | The current summary concurrency level (count of concurrently executed load operations)
Concurrency / Mean    | The mean summary concurrency level for the last *period* (10s by default)
Count / Success       | The count of the items processed sucessfully.
Count / Failed        | The count of the items processed with a failure.
Step Time [s]         | The test step elapsed time in seconds. Note that the step elapsing more than 115 days will cause the cell overflow as far as only 7 characters are available for the output.
Last Rate / [op/s]    | The moving average operations per second rate for the last period (10 seconds by default).
Last Rate / [MB/s]    | The moving average megabytes per second rate for the last period (10 seconds by default).
Mean Latency [us]     | The last mean latency measured in the microseconds.
Mean Duration [us]    | The last mean operation duration measured in the microseconds.

### 2.1.2. Files

Average metrics data is written to a CSV file `metrics.csv` with *dynamic path*. To prevent the average metrics file
output the configuration parameter `output-metrics-average-persist` should be set to "false". Note that the file output
for the metrics is always disabled for the load step slices (i.e. on the additional/remote nodes in the distributed
mode).

Field Name      | Description
----------------|------------
DateTimeISO8601 | Start timestamp in the ISO8601 format
OpType          | Load operation type (CREATE/READ/...)
Concurrency     | The configured concurrency limit per storage driver
NodeCount       | Count of the mongoose nodes used for the load (1 in case of the standalone mode, >1 in case of the distributed mode)
ConcurrencyCurr | The current summary concurrency level (count of concurrently executed load operations)
ConcurrencyMean | The mean summary concurrency level for the last *period* (10s by default)
CountSucc       | Total successful operations count
CountFail       | Total failed operations count
Size            | Total transferred byte count
StepDuration[s] | Total step duration
DurationSum[s]  | Total sum of the operations durations
TPAvg[op/s]     | Total average throughput
TPLast[op/s]    | Last final moving average throughput
BWAvg[MB/s]     | Total average bandwidth
BWLast[MB/s]    | Last final moving average bandwidth
DurationAvg[us] | Total average operations duration
DurationMin[us] | Minimum operation duration
DurationLoQ[us] | Low quartile of the operations duration distribution
DurationMed[us] | Median of the operations duration distribution
DurationHiQ[us] | High quartile of the operations duration distribution
DurationMax[us] | Maximum operation duration
LatencyAvg[us]  | Total average operations latency
LatencyMin[us]  | Minimum operation latency
LatencyLoQ[us]  | Low quartile of the operations latency distribution
LatencyMed[us]  | Median of the operations latency distribution
LatencyHiQ[us]  | High quartile of the operations latency distribution
LatencyMax[us]  | Maximum operation latency

## 2.2. Load Step Summary

At the end of each load step the summary metrics are produced.

### 2.2.1. Console

Console summary metrics output has YAML format for the better readability:
```yaml
---
# Results ##############################################################################################################
- Load Step Id:                copy_using_input_path_test_FS_LOCALxMEDIUM_SMALL
  Operation Type:              CREATE
  Node Count:                  1
  Concurrency:
    Limit Per Storage Driver:  100
    Actual:
      Last:                    0
      Mean:                    0.0
  Operations Count:
    Successful:                4096
    Failed:                    0
  Transfer Size:               40MB
  Duration [s]:
    Elapsed:                   6
    Sum:                       2.471925
  Throughput [op/s]:
    Last:                      554.2261935730298
    Mean:                      601.8219218336761
  Bandwidth [MB/s]:
    Last:                      5.4123651716116195
    Mean:                      5.877167205406994
  Operations Duration [us]:
    Avg:                       4175.429054054054
    Min:                       0
    LoQ:                       60
    Med:                       116
    HiQ:                       2684
    Max:                       216496
  Operations Latency [us]:
    Avg:                       4174.403716216216
    Min:                       0
    LoQ:                       59
    Med:                       115
    HiQ:                       2683
    Max:                       216495
```

* The console summary is displayed only on the entry node. It's not displayed on the additional nodes in the distributed mode.
* The equation (CONFIGURED_CURRENCY_LEVEL * DRIVER_COUNT * ELAPSED_TIME / OPERATIONS_DURATION_SUM) may be used as efficiency estimate.
* *Mean* rates are calculated as current total count of items/bytes divided by current elapsed time.
* *Last* rates are calculated as [exponentially decaying moving average](https://en.wikipedia.org/wiki/Moving_average#Exponential_moving_average) where

  ![alpha](http://i.piccy.info/i9/c211a78abdaeec65da61020c5dc83008/1485332899/627/722110/CodeCogsEqn.png)

  and "t" is the configured *load-metrics-period* parameter.

### 2.2.2. Files

The summary metrics produced at the end of each load step and the results are written to a CSV file `metrics.total.csv`.
The layout is the same as for average metrics file output. To disable the summary metrics file output the configuration
parameter `output-metrics-summary-persist` should be set to "false". Note that the file output for the metrics is always
disabled for the load step slices (i.e. on the additional/remote nodes in the distributed mode).

## 2.3. Operation Traces

The metrics for each load operation (request either file operation).

### 2.3.1. Console

The console output is absent.

### 2.3.2. Files

The file output is disabled by default.
To enable the file output, set the `output-metrics-trace-persist` configuration parameter to "true".

**Note**:
> Since v4.0.0 the file header (containing the field names) line is not printed for easier aggregation in case of the
> distributed mode.

**Output file**: `op.trace.csv` with *dynamic path*.

**Available fields**

| Field Name       | Description
| ---------------- | -----------------------------------------------------------------
| StorageNode      | The target storage node address/hostname
| ItemPath         | The resulting item path
| OpTypeCode       | The load operation type code
| StatusCode       | The load operation resulting status code
| ReqTimeStart[us] | The load operation start timestamp in microseconds
| Duration[us]     | The load operation total duration in microseconds
| RespLatency[us]  | The load operation response latency in microseconds
| DataLatency[us]  | The load operation response data latency ("1st byte" of the response content) in microseconds
| TransferSize     | The count of the bytes transferred within the load operation

**OpTypeCode**

| Code | Op Type
| ---- | --------
| 0    | NOOP
| 1    | CREATE
| 2    | READ
| 3    | UPDATE
| 4    | DELETE

**StatusCode**

| Code | Description                       | HTTP response codes                    |
| ---- | --------------------------------- | -------------------------------------- |
| O    | Pending (internal)                |                                        |
| 1    | Active (internal)                 |                                        |
| 2    | Interrupted                       |                                        |
| 3    | Unknown failure                   | all other codes                        |
| 4    | Success                           | 2xx                                    |
| 5    | I/O Failure                       |                                        |
| 6    | Timeout                           | 504                                    |
| 7    | Unrecognized storage response     |                                        |
| 8    | Client failure or invalid request | 100, 400, 405, 409, 411, 414, 416      |
| 9    | Internal storage failure          | 413, 415, 429, 500, 501, 502, 503, 505 |
| 10   | Item not found on the storage     | 404                                    |
| 11   | Authentication/access failure     | 401, 403                               |
| 12   | Data item corrupted               | 2xx                                    |
| 13   | Not enough space on the storage   | 507                                    |

## 2.4. Threshold

Mongoose controls the concurrency level by accounting the active channels at any moment of the time.
The channel may be an open file either established network connection. The channel is active when
it's assigned for an operation execution which is marked as active.

When Mongoose test step starts there's some short delay before the warmup is done and all channels
are busy with operations (active). It's expected that the performance rates are lower during this
warmup time range. Moreover, at the test step end some channels may remain active with the operation
while other channels are not selected for new operations.

To address these issues, it's necessary to account the performance metrics only while the active
channels count is higher than the configured threshold value (***threshold reached*** condition).
However, it's possible that the test step will never reach the *threshold reached* condition due to
some reasons (errors, very short operations, etc).

* Let the configured threshold be ***P***.
* Let configured concurrency level be ***C*** which is >0 and current busy channel count be ***N***.
* The *threshold reached* condition is *true* if N >= C * P

Metrics Manager:

1. Wait until all the storage drivers are in the *threshold reached* state
2. Create separate metrics instance and start the additional metrics accounting and aggregation
3. Wait until any storage driver exits the *threshold reached* state
4. Stop the additional metrics accounting and aggregation
5. Output the summary for the additional metrics

The load threshold value may be from 0 to 1 (inclusive).
The configuration parameter is `output-metrics-threshold`

CLI example: account the additional (threshold) metrics while the actual concurrency level is more than 80:
```bash
java -jar mongoose-<VER>.jar \
    --storage-driver-limit-concurrency=100 \
    --output-metrics-threshold=0.8
```

### 2.4.1. Console

1. The threshold state entrance is marked with the following log message:

   `<CONTEXT>: the threshold of <COUNT> active load operations count is reached, starting the additional metrics accounting`

2. The threshold state exit is marked with the following log message:

   `<CONTEXT>: the active load operations count is below the threshold of <COUNT>, stopping the additional metrics accounting`

### 2.4.2. Files

The layout is the same as usual metrics log files layout, the log file name is: `metrics.threshold.total.csv`. Note that
the file output for the metrics is always disabled for the load step slices (i.e. on the additional/remote nodes in the
distributed mode).
