# Configuration

1. [Overview](#1-overview)<br/>
1.1. [Reference Table](#11-reference-table)<br/>
1.2. [Specific Types](#12-specific-types)<br/>
1.2.1. [Time](#121-time)<br/>
1.2.2. [Size](#122-size)<br/>
1.2.3. [Dictionary](#123-dictionary)<br/>
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
2.4.2. [Multiuser Load](#242-multiuser-load)<br/>
3. [Aliasing](#3-aliasing)<br/>

## 1. Overview

All the configuration values have the default values which may be seen
in the file ```<MONGOOSE_DIR>/config/defaults.json```. The file contains
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
| item-naming-type                               | Enum | random | Specifies the new items naming order. Has effect only in the case of create load. "asc": the new items are named in the ascending order, "desc": the new items are named in the descending order, "random": the new items are named randomly |
| item-naming-prefix                             | String | null | The name prefix for the processed items. A correct value is neccessary to pass the content verification in the case of read load.
| item-naming-radix                              | Integer >= 2 | 36 | The radix for the item ids. May be in the range of 2..36. A correct value is neccessary to pass the content verification in the case of read load.
| item-naming-offset                             | Integer >= 0 | 0 | The start id for the new item ids
| item-naming-length                             | Integer > 0 | 12 | The name length for the new items. Has effect only in the case of create (if not partial) load
| item-output-file                               | Path | null | Specified the target file for the items processed successfully. If null the items info is not saved.
| item-output-path                               | String | null | The target path. Null (default) value leads to path name generation and pre-creation.
| item-type                                      | Enum | data | The type of the item to use, the possible values are: "data", "path", "token". In case of filesystem "data" means files and "path" means directories
| load-batch-size                                | Integer >= 1| 4096 | The count of the items/operations processed by a single invocation. It may be useful to set to 1 for MPU or DLO tests
| **load-op-limit-count**                        | Integer >= 0 | 0 | The maximum number of the load operations to execute for a load step. 0 means infinite
| **load-op-limit-fail-count**                   | Integer >= 0 | 100000 | The maximum number of the failed load operations before the step will be stopped, 0 means no limit
| **load-op-limit-fail-rate**                    | Boolean | false | Stop the step if failures rate is more than success rate and if the flag is set to true
| **load-op-limit-rate**                         | Float >= 0 | 0 | The maximum number of the load operations to execute per second (throughput limit). 0 means no rate limit.
| **load-op-limit-recycle**                      | Integer >= 1 | 1000000 | The load operations and results queues size limit
| **load-op-recycle**                            | Flag | false | Specifies whether to recycle the successfully finished operations multiple times or not
| **load-op-retry**                              | Flag | false | Specifies whether to retry the failed operations or not
| **load-op-shuffle**                            | Flag | false | Defines whether to shuffle or not the items got from the item input, what should make the order of the load operations execution randomized
| **load-op-type**                               | Enum | create | The operation to process the items, may be "create", "update", "read" or "delete"
| load-service-threads                           | Integer >= 0 | 0 | The **global** count of the service threads. 0 means automatic value (CPU cores/threads count)
| load-step-id                                   | String | null | The test step id. Generated automatically if not specified (null). Specifies also the logs sub directory path: `log/<STEP_ID>/`
| load-step-idAutoGenerated                      | Flag | true | Internal
| load-step-limit-size                           | Fixed size >= 0 | 0 | The maximum size of the data items to process. 0 means no size limit.
| load-step-limit-time                           | Time >= 0 | 0 | The maximum time to perform a load step. 0 means no time limit
| **load-step-node-addrs**                       | List of strings | <EMPTY> | Distributed mode: the list of the slave node IPs or hostnames, may include port numbers to override the default port number value. Standalone mode is used if empty (default behaviour).
| **load-step-node-port**                        | Integer > 0 | 1099 | Distributed mode: the common port number to start/connect the slave node
| output-color                                   | Flag | true | Use colored standard output flag
| output-metrics-average-period                  | Time >= 0 | 0 | The time period for the load step's metrics console output. 0 means to not to output the metrics to the console
| output-metrics-average-persist                 | Flag | true | Persist the average (periodic) metrics if true
| output-metrics-average-table-header-period     | Integer > 0 | 20 | Output the metrics table header every N rows
| **output-metrics-quantiles**                   | List |[0.25,0.5,0.75]| Output quantiles for metrics (only for [Monitoring API](https://github.com/emc-mongoose/mongoose/tree/master/doc/interfaces/api/monitoring#monitoring-api))
| output-metrics-summary-persist                 | Flag | true | Persist the load step's summary (total) metrics if true
| output-metrics-trace-persist                   | Flag | true | Persist the information about each load operation if true
| output-metrics-threshold                       | 0 <= Float <= 1 | 0 | The concurrency threshold to enable intermediate statistics calculation, 0 means no threshold
| **run-comment**                                | String | "" | A user defined comment to run the scenario via the Control API
| **run-node**                                   | Flag | false | Run in the slave node or not
| **run-scenario**                               | Path | null | The default file scenario to run, null means invoking the default.js scenario bundled into the distribution
| **run-version**                                | String | 4.0.0 | The Mongoose version
| storage-auth-file                              | Path | null | The path to a credentials list file, containing the lines of comma-separated user ids and secret keys
| storage-auth-uid                               | String | null | The authentication identifier
| storage-auth-secret                            | String | null | The authentication secret
| storage-auth-token                             | String | null | S3: no effect, Atmos: subtenant, Swift: token
| storage-namespace                              | String | null | The storage namespace
| **storage-driver-limit-concurrency**           | Integer >= 0 | 1 | The concurrency limit (per node in case of distributed mode). In case of filesystem this is the max number of open files at any moment. In case of HTTP this is the max number of the active connections at any moment.
| **storage-driver-limit-queue-input**           | Integer > 0 | 1000000 | Storage drivers internal input operations queue size limit
| **storage-driver-limit-queue-output**          | Integer > 0 | 1000000 | Storage drivers internal output operations queue size limit
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

## 2. Parameterization

The configuration value is usually fixed. However, it's possible to define a dynamic value changing
in the runtime. Each next value may be taken in a synchronous either asynchronous way.

### 2.1. Synchronous Supply

The next value is recalculated for each take attempt. The synchronous input is useful if the
recalculation complexity is low either different value on each take attempt is strictly required.

### 2.2. Asynchronous Supply

The next value is being recalculated in the background continuously. Taking the value frequently
is expected to yield a sequence of the same value. The asynchronous input is most useful when the
recalculation cost is relatively high and the values consumer doesn't require different value each
time.

### 2.3. Syntax

The common pattern syntax (excluding *path* format pattern) has the following general layout:

`%<TYPE_LETTER>(<SEED>){<FORMAT>}[<RANGE>]`

Where:
* `TYPE_LETTER` defines the value type, may be "d", "f", "D" ("p" has a bit different syntax, see below)
* `SEED` is ***optional***, the initial value for the random number generator used to obtain each next value
* `FORMAT` is ***optional***, the pattern to convert the value to a string
* `RANGE` is ***optional***, describes the value range as `<FROM>-<TO>` (`TO` value is exclusive)

For the number values (configured with "d" or "f") the format may be described using the Java's
DecimalFormat syntax: https://docs.oracle.com/javase/8/docs/api/java/text/DecimalFormat.html

#### 2.3.1. Available Formats

##### 2.3.1.1. Integer

Generates random 64-bit signed integer value in the specified range (optional).

Examples:
* `%d`: any integer value from `java.lang.Long.MIN_VALUE` to `java.lang.Long.MAX_VALUE`
* `%d{##########}`: as above, but with padding with leading zeroes to make the result string length = 10
* `%d[0-100]`: any integer value from 0 to 99
* `%d{###}[0-100]`: any integer value from 00 to 99
* `%d(123456789){###}[0-100]`: any integer value from 00 to 99, internal PRNG will be initialized using the specified seed

##### 2.3.1.2. Floating Point Number

Examples:
* `%f`: any floating point value from 0 to 1 (exclusive)
* `%f[1.23-4.56]`: any floating point value from 1.23 (inclusive) to 4.56 (exclusive)
* `%f{#.#######}`: any floating point value formatted using the specified format (3.1415926 for example)
* `%f{#.##}[2.71828182846-3.1415926]`: any floating point value from *e* to *pi* (exclusive)

##### 2.3.1.3. Date

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

##### 2.3.1.4. Path

Generates the path hierarchy elements using specified hierarchy *"width"* and *"depth"*.

The synchronous implementation is only available.

Also, has different layout:

`%p{<WIDTH>;<DEPTH>}` or `%p(<SEED>){<WIDTH>;<DEPTH>}`

### 2.4. Use Cases

#### 2.4.1. Variable Items Output Path

Parameterized configuration parameter: `item-output-path`

Example: dynamic files output path defined by some particular "width" (16) and "depth" (2):

```bash
java --module-path mongoose-<VERSION>.jar --module com.emc.mongoose \
    --item-output-path=/mnt/storage/%p\{16\;2\} \
    --storage-driver-type=fs \
    ...
```

#### 2.4.2. Multiuser Load

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
        "op" : {
            "limit" : {
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
            "limit" : {
                "concurrency" : 10
            },
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

## 3. Aliasing

The configuration aliasing is used primarily for backward compatibility to map old configuration paths to the new ones.
Also there's a shortcut alias for the load operation types:

| Alias  | Meaning
|--------|--------
| create | load-op-type=create
| read   | load-op-type=read
| update | load-op-type=update
| delete | load-op-type=delete
| noop   | load-op-type=noop
