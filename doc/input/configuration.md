# Contents

1. [Overview](#1-overview)<br/>
1.1. [Quick Reference Table](#11-quick-reference-table)<br/>
1.1.1. [Base](#111-base)<br/>
1.1.2. [Network Storages](#112-network-storages)<br/>
1.1.3. [HTTP Network Storages](#113-http-network-storages)<br/>
1.1.4. [Pipeline Load Step](#114-pipeline-load-step)<br/>
1.1.5. [Weighted Load Step](#115-weighted-load-step)<br/>
1.2. [Specific Types](#12-specific-types)<br/>
1.2.1. [Time](#121-time)<br/>
1.2.2. [Size](#122-size)<br/>
2. [Parameterization](#2-parameterization)<br/>
2.1. [Symchronous Supply](#21-synchronous-supply)<br/>
2.2. [Asynchronous Supply](#22-asynchronous-supply)<br/>
2.3. [Syntax](#23-syntax)<br/>
2.3.1. [Available Formats](#231-available-formats)<br/>
2.3.1.1. [Integer](#2311-integer)<br/>
2.3.1.2. [Floating Point Number](#2312-floating-point-number)<br/>
2.3.1.3. [Date](#2313-date)<br/>
2.3.1.4. [Path](#2314-path)<br/>
2.4. [Use Cases](#24-use-cases)<br/>
2.4.1. [Variable Items Output Path](#241-variable-items-output-path)<br/>
2.4.2. [Variable HTTP Headers Values](#242-variable-http-headers-values)<br/>
2.4.3. [Multiuser Load](#243-multiuser-load)<br/>
3. [Aliasing](#3-aliasing)<br/>
3.1. [General](#31-general)<br/>
3.2. [Atmos](#32-atmos)<br/>
3.3. [S3](#33-s3)<br/>
3.4. [Swift](#34-swift)<br/>

# 1. Overview

All the configuration values have the default values which may be seen
in the file ```<MONGOOSE_DIR>/config/defaults.json```. The file contains
the comments so it's quite self-descriptive and may be used as quick
reference.

To configure the run an user should use the corresponding CLI arguments
to override the default values. A CLI argument is parsed as a JSON path:
```
--item-data-size=100KB
```
is mentioned as the corresponding JSON node:
```json
"item" : {
    "data" : {
        "size" : "100KB"
    }
}
```

## 1.1. Quick Reference Table

### 1.1.1. Base

| Name                                           | Type         | Default Value    | Description                                      |
|:-----------------------------------------------|:-------------|:-----------------|:-------------------------------------------------|
| item-data-input-file                           | Path         | null             | The source file for the content generation       |
| item-data-input-layer-cache                    | Integer > 0  | 25               | The maximum count of the data "layers" to be cached into the memory
| item-data-input-layer-size                     | Fixed Size   | 4MB              | The size of the content source ring buffer |
| item-data-input-seed                           | String (hex) | 7a42d9c483244167 | The initial value for the random data generation |
| item-data-ranges-concat                        | Range | null | The number/range of numbers of the source objects used to concatenate every destination objec
| item-data-ranges-fixed                         | Byte Range **list** | null | The fixed byte ranges to update or read (depends on the specified load type) |
| item-data-ranges-random                        | Integer >= 0 | 0 | The count of the random ranges to update or read |
| item-data-ranges-threshold                     | Size | 0 | The size threshold to enable multipart upload if supported by the configured storage driver |
| item-data-size                                 | Size | 1MB | The size of the data items to process. Doesn't have any effect if item.type=container |
| item-data-verify                               | Flag | false | Specifies whether to verify the content while reading the data items or not. Doesn't have any effect if load-type != read |
| item-input-file                                | Path | null | The source file for the items to process. If null the behavior depends on the load type. |
| item-input-path                                | String | null | The source path which may be used as items input if not "item-input-file" is specified. Also used for the copy mode as the path containing the items to be copied into the output path. |
| item-naming-type                               | Enum | random | Specifies the new items naming order. Has effect only in the case of create load. "asc": the new items are named in the ascending order, "desc": the new items are named in the descending order, "random": the new items are named randomly |
| item-naming-prefix                             | String | null | The name prefix for the processed items. A correct value is neccessary to pass the content verification in the case of read load.
| item-naming-radix                              | Integer >= 2 | 36 | The radix for the item ids. May be in the range of 2..36. A correct value is neccessary to pass the content verification in the case of read load.
| item-naming-offset                             | Integer >= 0 | 0 | The start id for the new item ids
| item-naming-length                             | Integer > 0 | 12 | The name length for the new items. Has effect only in the case of create (if not partial) load
| item-output-file                               | Path | null | Specified the target file for the items processed successfully. If null the items info is not saved.
| item-output-path                               | String | null | The target path. Null (default) value leads to path name generation and pre-creation.
| item-type                                      | Enum | data | The type of the item to use, the possible values are: "data", "path", "token". In case of filesystem "data" means files and "path" means directories
| load-batch-size                                | Integer >= 1| 4096 | The count of the items/tasks processed by a single invocation. It may be useful to set to 1 for MPU or DLO tests
| load-generator-recycle-enabled                 | Flag | false | Specifies whether to recycle the load tasks multiple times or not
| load-generator-recycle-limit                   | Integer >= 1 | 1000000 | The load tasks and results queues size limit
| load-generator-shuffle                         | Flag | false | Defines whether to shuffle or not the items got from the item input, what should make the order of the I/O tasks execution randomized
| load-service-threads                           | Integer >= 0 | 0 | The count of the service threads. 0 means automatic value (CPU cores/threads count)
| load-step-id                                   | String | null | The test step id. Generated automatically if not specified (null). Specifies also the logs sub directory path: `log/<STEP_ID>/`
| load-step-idAutoGenerated                      | Flag | true | Internal
| load-step-limit-concurrency                    | Integer >= 0 | 1 | The concurrency limit (per node in case of distributed mode). In case of filesystem this is the max number of open files at any moment. In case of HTTP this is the max number of the active connections at any moment.
| load-step-limit-count                          | Integer >= 0 | 0 | The maximum number of the items to process for any load step. 0 means infinite
| load-step-limit-fail-count                     | Integer >= 0 | 100000 | The maximum number of the failed I/O tasks before the step will be stopped, 0 means no limit
| load-step-limit-fail-rate                      | Boolean | false | Stop the step if failures rate is more than success rate and if the flag is set to true
| load-step-limit-rate                           | Float >= 0 | 0 | The maximum number of the tasks to execute per second (throughput limit). 0 means no rate limit.
| load-step-limit-size                           | Fixed size >= 0 | 0 | The maximum size of the data items to process. 0 means no size limit.
| load-step-limit-time                           | Time >= 0 | 0 | The maximum time to perform a load step. 0 means no time limit
| load-step-node-addrs                           | List of strings | <EMPTY> | Distributed mode: the list of the slave node IPs or hostnames, may include port numbers to override the default port number value. Standalone mode is used if empty (default behaviour).
| load-step-node-port                            | Integer > 0 | 1099 | Distributed mode: the common port number to start/connect the slave node
| load-type                                      | Enum | create | The operation to process the items, may be "create", "update", "read" or "delete"
| output-color                                   | Flag | true | Use colored standard output flag
| output-metrics-average-period                  | Time >= 0 | 0 | The time period for the load step's metrics console output. 0 means to not to output the metrics to the console
| output-metrics-average-persist                 | Flag | true | Persist the average (periodic) metrics if true
| output-metrics-average-table-header-period     | Integer > 0 | 20 | Output the metrics table header every N rows
| output-metrics-summary-perfDbResultsFile       | Flag | false | Output the results.xml file used as a PerfDb input if true
| output-metrics-summary-persist                 | Flag | true | Persist the load step's summary (total) metrics if true
| output-metrics-trace-persist                   | Flag | true | Persist the information about each load operation if true
| output-metrics-threshold                       | 0 <= Float <= 1 | 0 | The concurrency threshold to enable intermediate statistics calculation, 0 means no threshold
| run-node                                       | Flag | false | Run in the slave node or not
| run-scenario                                   | Path | null | The default file scenario to run, null means invoking the default.js scenario bundled into the distribution
| run-version                                    | String | 4.0.0 | The Mongoose version
| storage-auth-file                              | Path | null | The path to a credentials list file, containing the lines of comma-separated user ids and secret keys
| storage-auth-uid                               | String | null | The authentication identifier
| storage-auth-secret                            | String | null | The authentication secret
| storage-auth-token                             | String | null | S3: no effect, Atmos: subtenant, Swift: token
| storage-driver-queue-input                     | Integer > 0 | 1000000 | Storage drivers internal input tasks queue size limit
| storage-driver-queue-output                    | Integer > 0 | 1000000 | Storage drivers internal output tasks queue size limit
| storage-driver-threads                         | Integer >= 0 | 0 | The count of the shared/global I/O executor threads. 0 means automatic value (CPU cores/threads count)
| storage-driver-type                            | String | s3 | The identifier pointing to the one of the registered storage driver implementations to use

### 1.1.2. Network Storages

| Name                                           | Type         | Default Value    | Description                                      |
|:-----------------------------------------------|:-------------|:-----------------|:-------------------------------------------------|
| storage-net-node-addrs                         | List of strings | 127.0.0.1 | The list of the storage node IPs or hostnames to use for HTTP load. May include port numbers.
| storage-net-node-connAttemptsLimit             | Integer >= 0 | 0 | The limit for the subsequent connection attempts for each storage endpoint node. The node is excluded from the connection pool forever if the node has more subsequent connection failures. The default value (0) means no limit.
| storage-net-node-port                          | Integer > 0 | 9020 | The common port number to access the storage nodes, may be overriden adding the port number to the storage-driver-addrs, for example: "127.0.0.1:9020,127.0.0.1:9022,..."
| storage-net-node-slice                         | Flag | false | Slice the storage node addresses between the mongoose nodes using the greatest common divisor or not
| storage-net-bindBacklogSize                    | Integer >= 0 | 0 |
| storage-net-interestOpQueued                   | Flag | false |
| storage-net-keepAlive                          | Flag | true |
| storage-net-linger                             | Integer >= 0 | 0 |
| storage-net-reuseAddr                          | Flag | true |
| storage-net-rcvBuf                             | Fixed size >= 0 | 0 | The network connection input buffer size. Estimated automatically if 0 (default)
| storage-net-sndBuf                             | Fixed size >= 0 | 0 | The network connection output buffer size. Estimated automatically if 0 (default)
| storage-net-selectInterval                     | Integer > 0 | 100 |
| storage-net-tcpNoDelay                         | Flag | true |
| storage-net-timeoutMilliSec                    | Integer >= 0 | 1000000 | The socket timeout
| storage-net-ioRatio                            | 0 < Integer < 100 | 50 | Internal [Netty's I/O ratio parameter](https://github.com/netty/netty/issues/1154#issuecomment-14870909). It's recommended to make it higher for large request/response payload (>1MB)
| storage-net-transport                          | Enum | nio | The I/O transport to use (see the [details](http://netty.io/wiki/native-transports.html)). By default tries to use "nio" (the most compatible). For Linux try to use "epoll", for MacOS/BSD use "kqueue" (requires rebuilding).
| storage-net-ssl                                | Flag | false | The flag to enable the load through SSL/TLS. Currently only HTTPS implementation is available. Have no effect if configured storage type is filesystem.

#### 1.1.2.1. HTTP Network Storages

| Name                                           | Type         | Default Value    | Description                                      |
|:-----------------------------------------------|:-------------|:-----------------|:-------------------------------------------------|
| storage-net-http-fsAccess                      | Flag | false | Specifies whether filesystem access is enabled or not in the case of S3 or Atmos API
| storage-net-http-headers                       | Map | { "Connection" : "keep-alive", "User-Agent" : "mongoose/3.6.0" } | Custom HTTP headers section. An user may place here a key-value pair which will be used as HTTP header.
| storage-net-http-namespace                     | String | null | The HTTP storage namespace. WARNING: the default value (null) will not work in the case of Swift API
| storage-net-http-versioning                    | Flag | false | Specifies whether the versioning storage feature is used or not

#### 1.1.3. Pipeline Load Step

| Name                                           | Type         | Default Value    | Description                                      |
|:-----------------------------------------------|:-------------|:-----------------|:-------------------------------------------------|
| item-output-delay                              | Time >= 0 | 0 | The minimum delay between the subsequent I/O operations for each item. 0 means no delay.

#### 1.1.4. Weighted Load Step

| Name                                           | Type         | Default Value    | Description                                      |
|:-----------------------------------------------|:-------------|:-----------------|:-------------------------------------------------|
| load-generator-weight                          | Integer >= 0 | 1 | The relative operations weight for the particular load generator. Effective only if used with weighted load step and JSR-223 compliant scenario engine. The total step's weight is a sum of all included load generator weights. The particular load generator weight is a ratio of its weight value to the total step's weight.

## 1.2. Specific Types

### 1.2.1. Time

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

### 1.2.2. Size

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

# 2. Parameterization

The configuration value is usually fixed. However, it's possible to define a dynamic value changing
in the runtime. Each next value may be taken in a synchronous either asynchronous way.

## 2.1. Synchronous Supply

The next value is recalculated for each take attempt. The synchronous input is useful if the
recalculation complexity is low either different value on each take attempt is strictly required.

## 2.2. Asynchronous Supply

The next value is being recalculated in the background continuously. Taking the value frequently
is expected to yield a sequence of the same value. The asynchronous input is most useful when the
recalculation cost is relatively high and the values consumer doesn't require different value each
time.

## 2.3. Syntax

The common pattern syntax (excluding *path* format pattern) has the following general layout:

`%<TYPE_LETTER>(<SEED>){<FORMAT>}[<RANGE>]`

Where:
* `TYPE_LETTER` defines the value type, may be "d", "f", "D" ("p" has a bit different syntax, see below)
* `SEED` is ***optional***, the initial value for the random number generator used to obtain each next value
* `FORMAT` is ***optional***, the pattern to convert the value to a string
* `RANGE` is ***optional***, describes the value range as `<FROM>-<TO>` (`TO` value is exclusive)

For the number values (configured with "d" or "f") the format may be described using the Java's
DecimalFormat syntax: https://docs.oracle.com/javase/8/docs/api/java/text/DecimalFormat.html

### 2.3.1. Available Formats

#### 2.3.1.1. Integer

Generates random 64-bit signed integer value in the specified range (optional).

Examples:
* `%d`: any integer value from `java.lang.Long.MIN_VALUE` to `java.lang.Long.MAX_VALUE`
* `%d{##########}`: as above, but with padding with leading zeroes to make the result string length = 10
* `%d[0-100]`: any integer value from 0 to 99
* `%d{###}[0-100]`: any integer value from 00 to 99
* `%d(123456789){###}[0-100]`: any integer value from 00 to 99, internal PRNG will be initialized using the specified seed

#### 2.3.1.2. Floating Point Number

Examples:
* `%f`: any floating point value from 0 to 1 (exclusive)
* `%f[1.23-4.56]`: any floating point value from 1.23 (inclusive) to 4.56 (exclusive)
* `%f{#.#######}`: any floating point value formatted using the specified format (3.1415926 for example)
* `%f{#.##}[2.71828182846-3.1415926]`: any floating point value from *e* to *pi* (exclusive)

#### 2.3.1.3. Date

Generates random date in the specified format (mandatory) in the specified range (optional).

Examples:
* `%D`: any date time between Unix zero date time and *now*
* `%D{yyyy-MM-dd'T'HH:mm:ss}[2013/10/30-2017/02/09]`: any date time between the specified dates
* `%D{yyyy-MM-dd'T'HH:mm:ss}`: any date time between Unix zero date time and *now* in the specified format
* `%D{yyyy-MM-dd'T'HH:mm:ss}[2013/10/30-2017/02/09]`: any date time between the specified dates in the specified format

***Note***:
> * The format may be described using the Java's SimpleDateFormat syntax:
https://docs.oracle.com/javase/8/docs/api/java/text/SimpleDateFormat.html

> * Range boundary dates should be in `yyyy/MM/dd` or `yyyy/MM/dd'T'HH:mm:ss` format

#### 2.3.1.4. Path

Generates the path hierarchy elements using specified hierarchy *"width"* and *"depth"*.

The synchronous implementation is only available.

Also, has different layout:

`%p{<WIDTH>;<DEPTH>}` or `%p(<SEED>){<WIDTH>;<DEPTH>}`

## 2.4. Use Cases

### 2.4.1. Variable Items Output Path

Parameterized configuration parameter: `item-output-path`

Example: dynamic files output path defined by some particular "width" (16) and "depth" (2):

```bash
java -jar mongoose-<VERSION>.jar \
    --item-output-path=/mnt/storage/%p\{16\;2\} \
    --storage-driver-type=fs \
    ...
```

### 2.4.2. Variable HTTP Headers Values

Parameterized configuration parameter: `storage-net-http-headers-*`

CLI example, note the "\" characters to escape the whitespaces in the header value:

```bash
java -jar mongoose-<VERSION>.jar \
    --storage-net-http-headers=myOwnHeaderName:MyOwnHeaderValue\ %d[0-1000]\ %f{###.##}[-2--1]\ %D{yyyy-MM-dd'T'HH:mm:ssZ}[1970/01/01-2016/01/01]
```

Scenario example, note the parameterized header name:

```javascript
var varHttpHeadersConfig = {
    "storage" : {
        "net" : {
            "http" : {
                "headers" : {
                    "x-amz-meta-$d[1-30]" : "%D{yyyy-MM-dd'T'HH:mm:ssZ}[1970/01/01-2016/01/01]"
                }
            }
        }
    }
};

Load
    .config(varHttpHeadersConfig)
    .run();
```

### 2.4.3. Multiuser Load

Parameterized configuration parameters:
* `item-output-path`
* `storage-auth-uid`

Let's realize the case when someone needs to perform a load using many (hundreds, thousands)
destination paths (S3 buckets, Swift containers, filesystem directories, etc) using many different
credentials.

```javascript
var multiUserConfig = {
    "item" : {
        "data" : {
            "size" : "10KB"
        },
        "output" : {
            "file" : "objects.csv",
            "path" : "bucket-%d(314159265){00}[0-99]"
        }
    },
    "load" : {
        "step" : {
            "limit" : {
                "concurrency" : 10,
                "count" : 10000
            }
        }
    },
    "storage" : {
        "auth" : {
            "file" : "credentials.csv",
            "uid" : "user-%d(314159265){00}[0-99]"
        },
        "driver" : {
            "type" : "s3"
        }
    }
};

Load
    .config(multiUserConfig)
    .run();
```

**Note**:
> * The seed value "314159265" should be used to init the internal PRNG to align the bucket names with user ids (The same "XY" value for each pair of "bucket-XY" and "user-XY" supplied).
> * The current externally loaded credentials count limit is 1 million.

In this case, the file "credentials.csv" should be prepared manually. Example contents:
```
user-00,secret-00
user-01,secret-01
user-02,secret-02
...
user-99,secret-99
```
(in the real world it is expected that the storage users with the listed names and secret keys are already existing)

To make such example file you may use the following 2 commands:
```bash
for i in $(seq 0 9); do echo "user-0$i,secret-0$i" >> credentials.csv; done;
for i in $(seq 10 99); do echo "user-$i,secret-$i" >> credentials.csv; done
```

# 3. Aliasing

The configuration aliasing is used primarily for backward compatibility
to map old configuration paths to the new ones.
