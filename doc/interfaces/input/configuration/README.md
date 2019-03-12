# Configuration

1. [Overview](#1-overview)<br/>
1.1. [Reference Table](#11-reference-table)<br/>
1.2. [Specific Types](#12-specific-types)<br/>
1.2.1. [Time](#121-time)<br/>
1.2.2. [Size](#122-size)<br/>
1.2.3. [Dictionary](#123-dictionary)<br/>
1.2.4. [Expression](#124-expression)<br/>
2. [Aliasing](#2-aliasing)<br/>

## 1. Overview

All the configuration values have the default values which may be seen
in the file ```<MONGOOSE_DIR>/config/defaults.yaml```. The file contains
the comments so it's quite self-descriptive and may be used as quick
reference.

### 1.1. Reference Table

| Name                                           | Type         | Default Value    | Description                                      |
|:-----------------------------------------------|:-------------|:-----------------|:-------------------------------------------------|
| item-data-input-file                           | Path         | null             | The source file for the content generation       |
| item-data-input-layer-cache                    | Integer > 0  | 25               | The maximum count of the data "layers" to be cached into the memory
| item-data-input-layer-size                     | Fixed Size   | 4MB              | The size of the content source ring buffer |
| item-data-input-seed                           | String (hex) | 7a42d9c483244167 | The initial value for the random data generation |
| item-data-ranges-concat                        | Range | null | The number/range of numbers of the source objects used to concatenate every destination objec
| item-data-ranges-fixed                         | Byte Range **list** | null | The fixed byte ranges to update or read (depends on the specified load type) |
| item-data-ranges-random                        | Integer >= 0 | 0 | The count of the random ranges to update or read |
| item-data-ranges-threshold                     | Size | 0 | The size threshold to enable the multipart upload if supported by the configured storage driver |
| item-data-size                                 | Size | 1MB | The size of the data items to process. Doesn't have any effect if item.type=container |
| item-data-verify                               | Flag | false | Specifies whether to verify the content while reading the data items or not. Doesn't have any effect if load-type != read |
| item-input-file                                | Path | null | The source file for the items to process. If null the behavior depends on the load type. |
| item-input-path                                | String | null | The source path which may be used as items input if not "item-input-file" is specified. Also used for the copy mode as the path containing the items to be copied into the output path. |
| item-naming-length                             | Integer > 0 | 12 | The name length for the new items. Has effect only in the case of create (if not partial) load
| item-naming-seed                               | Integer or Expression | %{math:xor(int64:reverse(time:millisSinceEpoch()), int64:reverseBytes(time:nanos()))} | The initial id for the new item ids
| item-naming-prefix                             | String or Expression | null | The name prefix for the processed items. A correct value is neccessary to pass the content verification in the case of read load.
| item-naming-radix                              | Integer >= 2 | 36 | The radix for the item ids. May be in the range of 2..36. A correct value is neccessary to pass the content verification in the case of read load.
| item-naming-step                               | Integer | 1 | The item naming step. Makes sense in case of "serial" naming type. Negative values cause descending order.
| item-naming-type                               | Enum | random | Specifies the new items naming order. Has effect only in the case of create load. "serial": the new items are named in a sequential order, "random": the new items are named randomly |
| item-output-file                               | Path | null | Specified the target file for the items processed successfully. If null the items info is not saved.
| item-output-path                               | String or Expression | %{date:format(\"yyyyMMdd.HHmmss.SSS\").format(date:from(time:millisSinceEpoch())} | The target path. By default the expression will once generate the constant value equal to the timestamp.
| item-type                                      | Enum | data | The type of the item to use, the possible values are: "data", "path", "token". In case of filesystem "data" means files and "path" means directories
| load-batch-size                                | Integer >= 1| 4096 | The count of the items/operations processed by a single invocation. It may be useful to set to 1 for MPU or DLO tests
| load-op-limit-count                            | Integer >= 0 | 0 | The maximum number of the load operations to execute for a load step. 0 means infinite
| load-op-limit-fail-count                       | Integer >= 0 | 100000 | The maximum number of the failed load operations before the step will be stopped, 0 means no limit
| load-op-limit-fail-rate                        | Boolean | false | Stop the step if failures rate is more than success rate and if the flag is set to true
| load-op-limit-rate                             | Float >= 0 | 0 | The maximum number of the load operations to execute per second (throughput limit). 0 means no rate limit.
| load-op-limit-recycle                          | Integer >= 1 | 1000000 | The load operations and results queues size limit
| load-op-recycle                                | Flag | false | Specifies whether to recycle the successfully finished operations multiple times or not
| load-op-retry                                  | Flag | false | Specifies whether to retry the failed operations or not
| load-op-shuffle                                | Flag | false | Defines whether to shuffle or not the items got from the item input, what should make the order of the load operations execution randomized
| load-op-type                                   | Enum | create | The operation to process the items, may be "create", "update", "read" or "delete"
| load-service-threads                           | Integer >= 0 | 0 | The **global** count of the service threads. 0 means automatic value (CPU cores/threads count)
| load-step-id                                   | String | null | The test step id. Generated automatically if not specified (null). Specifies also the logs sub directory path: `log/<STEP_ID>/`
| load-step-idAutoGenerated                      | Flag | false | Internal
| load-step-limit-size                           | Fixed size >= 0 | 0 | The maximum size of the data items to process. 0 means no size limit.
| load-step-limit-time                           | Time >= 0 | 0 | The maximum time to perform a load step. 0 means no time limit
| load-step-node-addrs                           | List of strings | <EMPTY> | Distributed mode: the list of the slave node IPs or hostnames, may include port numbers to override the default port number value. Standalone mode is used if empty (default behaviour).
| load-step-node-port                            | Integer > 0 | 1099 | Distributed mode: the common port number to start/connect the slave node
| output-color                                   | Flag | true | Use colored standard output flag
| output-metrics-average-period                  | Time >= 0 | 0 | The time period for the load step's metrics console output. 0 means to not to output the metrics to the console
| output-metrics-average-persist                 | Flag | true | Persist the average (periodic) metrics if true
| output-metrics-average-table-header-period     | Integer > 0 | 20 | Output the metrics table header every N rows
| output-metrics-quantiles                       | List |[0.25,0.5,0.75]| Output quantiles for metrics (only for [Monitoring API](doc/interfaces/api/monitoring#monitoring-api))
| output-metrics-summary-persist                 | Flag | true | Persist the load step's summary (total) metrics if true
| output-metrics-trace-persist                   | Flag | true | Persist the information about each load operation if true
| output-metrics-threshold                       | 0 <= Float <= 1 | 0 | The concurrency threshold to enable intermediate statistics calculation, 0 means no threshold
| run-comment                                    | String | "" | A user defined comment to run the scenario via the Control API
| run-node                                       | Flag | false | Run in the slave node or not
| run-scenario                                   | Path | null | The default file scenario to run, null means invoking the default.js scenario bundled into the distribution
| run-version                                    | String | 4.0.0 | The Mongoose version
| storage-auth-file                              | Path | null | The path to a credentials list file, containing the lines of comma-separated item path, user id and secret key
| storage-auth-uid                               | String | null | The authentication identifier
| storage-auth-secret                            | String | null | The authentication secret
| storage-auth-token                             | String | null | S3: no effect, Atmos: subtenant, Swift: token
| storage-namespace                              | String | null | The storage namespace
| storage-driver-limit-concurrency               | Integer >= 0 | 1 | The concurrency limit (per node in case of distributed mode). In case of filesystem this is the max number of open files at any moment. In case of HTTP this is the max number of the active connections at any moment.
| storage-driver-limit-queue-input               | Integer > 0 | 1000000 | Storage drivers internal input operations queue size limit
| storage-driver-limit-queue-output              | Integer > 0 | 1000000 | Storage drivers internal output operations queue size limit
| storage-driver-threads                         | Integer >= 0 | 0 | The count of the shared/global I/O executor threads. 0 means automatic value (CPU cores/threads count)
| storage-driver-type                            | String | s3 | The identifier pointing to the one of the registered storage driver implementations to use

#### 1.2. Specific Types

##### 1.2.1. Time

The configuration parameters supporting the time type:
* item-output-delay
* output-metrics-average-period
* load-step-limit-time

| Value | Effect
| ----- | ------
| "0"   | 0/infinite/not set
| "-1"  | Invalid value
| "1"   | 1 second
| "1s"  | 1 second
| "2m"  | 2 minutes
| "3h"  | 3 hours
| "4d"  | 4 days
| "5w"  | Invalid value
| "6M"  | Invalid value
| "7y"  | Invalid value

##### 1.2.2. Size

The configuration parameters supporting the time type:

* item-data-content-ring-size
* item-data-size
* item-data-ranges-threshold
* storage-net-rcvBuf
* storage-net-sndBuf
* load-step-limit-size

| Value   | Effect
| ------- | ------
| "-1"    | Invalid Value
| "0"     | 0 bytes (Infinity in case of `load-step-limit-size`)
| "1"     | 1 bytes
| "1024"  | 1024 bytes or 1KB
| "0B"    | 0 bytes (Infinity in case of `load-step-limit-size`)
| "1024B" | 1024 bytes or 1KB
| "1KB"   | 1024 bytes or 1KB
| "2MB"   | 2MB
| "6EB"   | 6EB (exobytes)
| "7YB"   | Invalid Value

##### 1.2.3. Dictionary

Some configuration values support the dictionary type. Don't use the command line arguments for the dictionary values
setting.

##### 1.2.4. Expression

The [expression language](base/src/main/java/com/emc/mongoose/config/el/README.md) allows to assign the dynamic values 
to some configuration parameters.

## 2. Aliasing

The configuration aliasing is used primarily for backward compatibility to map old configuration paths to the new ones.
Also there's a shortcut alias for the load operation types:

| Alias  | Meaning
|--------|--------
| create | load-op-type=create
| read   | load-op-type=read
| update | load-op-type=update
| delete | load-op-type=delete
| noop   | load-op-type=noop
