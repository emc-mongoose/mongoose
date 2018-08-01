# Contents

1. **[Configuration](#1-configuration)**<br/>
1.1. **<i>[Configuration Syntax](#11-configuration-syntax)</i>**<br/>
1.2. **<i>[Aliasing](#12-aliasing)</i>**<br/>
1.3. **<i>[Parameterized Configuration](#13-parameterized-configuration)</i>**<br/>
1.3.1. **<i>[Parameterized HTTP headers](#131-parameterized-http-headers)</i>**<br/>
1.3.2. **<i>[Parameterized Output Path](#132-parameterized-output-path)</i>**<br/>
1.3.3. **<i>[Multiuser Load](#133-multiuser-load)</i>**<br/>
2. **[Items](#2-items)**<br/>
2.1. **<i>[Item Types](#21-item-types)</i>**<br/>
2.1.1. *[Data Items](#211-data-items)*<br/>
2.1.1.1. [Fixed Size Data Items](#2111-fixed-size-data-items)<br/>
2.1.1.1.1. [Empty Data Items](#21111-empty-data-items)<br/>
2.1.1.2. [Random Size Data Items](#2112-random-size-data-items)<br/>
2.1.1.2.1. [Biased Random Size Data Items](#21121-biased-random-size-data-items)<br/>
2.1.2. *[Path Items](#212-path-items)*<br/>
2.1.3. *[Token Items](#213-token-items)*<br/>
2.2. **<i>[Items Input](#22-items-input)</i>**<br/>
2.2.1. *[Items Input File](#221-items-input-file)*<br/>
2.2.2. *[Items Path Listing Input](#222-items-path-listing-input)*<br/>
2.2.3. *[New Items Input](#223-new-items-input)*<br/>
2.2.3.1. [Random Item Ids](#2231-random-item-ids)<br/>
2.2.3.2. [Ascending Item Ids](#2232-ascending-item-ids)<br/>
2.2.3.3. [Descending Item Ids](#2233-descending-item-ids)<br/>
2.2.3.4. [Items Id Prefix](#2234-items-id-prefix)<br/>
2.2.3.5. [Items Id Radix](#2235-items-id-radix)<br/>
2.2.3.6. [Items Id Offset](#2236-items-id-offset)<br/>
2.2.3.7. [Items Id Length](#2237-items-id-length)<br/>
2.3. **<i>[Items Output](#23-items-output)</i>**<br/>
2.3.1. *[Items Output Delay](#231-items-output-delay)*<br/>
2.3.2. *[Items Output File](#232-items-output-file)*<br/>
2.3.3. *[Items Output Path](#233-items-output-path)*<br/>
2.3.3.1. [Constant Items Output Path](#2331-constant-items-output-path)<br/>
2.3.3.2. [Pattern Items Output Path](#2332-pattern-items-output-path)<br/>
3. **[Content](#3-content)**<br/>
3.1. **<i>[Uniform Random Data Payload](#31-uniform-random-data-payload)</i>**<br/>
3.2. **<i>[Payload From the External File](#32-payload-from-the-external-file)</i>**<br/>
4. **[Concurrency](#4-concurrency)**<br/>
4.1. **<i>[Limited Concurrency](#41-limited-concurrency)</i>**<br/>
4.2. **<i>[Unlimited Concurrency](#42-unlimited-concurrency)</i>**<br/>
5. **[Recycle Mode](#5-recycle-mode)**<br/>
6. **[Test Steps](#6-test-steps)**<br/>
6.1. **<i>[Test Steps Identification](#61-test-steps-identification)</i>**<br/>
6.2. **<i>[Test Steps Limitation](#62-test-steps-limitation)</i>**<br/>
6.2.1. *[Steps Are Infinite by Default](#621-steps-are-infinite-by-default)*<br/>
6.2.2. *[Limit Step by Processed Item Count](#622-limit-step-by-processed-item-count)*<br/>
6.2.3. *[Limit Step by Rate](#623-limit-step-by-rate)*<br/>
6.2.4. *[Limit Step by Processed Data Size](#624-limit-step-by-processed-data-size)*<br/>
6.2.5. *[Limit Step by Time](#625-limit-step-by-time)*<br/>
6.2.6. *[Limit Step by End of Items Input](#626-limit-step-by-end-of-items-input)*<br/>
7. **[Output](#7-output)**<br/>
7.1. **<i>[Console Coloring](#71-console-coloring)</i>**<br/>
7.2. **<i>[Metrics Output](#72-metrics-output)</i>**<br/>
7.2.1. *[Average Metrics Output](#721-average-metrics-output)*<br/>
7.2.1.1. [Average Metrics Output Period](#7211-average-metrics-output-period)<br/>
7.2.1.2. [Average Metrics Output Persistence](#7212-average-metrics-output-persistence)<br/>
7.2.1.3. [Average Metrics Table Header Output Period](#7213-average-metrics-table-header-output-period)<br/>
7.2.2. *[Summary Metrics Output](#722-summary-metrics-output)*<br/>
7.2.3. *[Trace Metrics Output](#723-trace-metrics-output)*<br/>
7.2.4. *[Metrics Accounting Threshold](#724-metrics-accounting-threshold)*<br/>
8. **[Load Types](#8-load-types)**<br/>
8.1. **<i>[Noop](#81-noop)</i>**<br/>
8.2. **<i>[Create](#82-create)</i>**<br/>
8.2.1. *[Create New Items](#821-create-new-items)*<br/>
8.2.2. *[Copy Mode](#822-copy-mode)*<br/>
8.3. **<i>[Read](#83-read)</i>**<br/>
8.3.1. *[Read With Disabled Validation](#831-read-with-disabled-verification)*<br/>
8.3.2. *[Read With Enabled Validation](#832-read-with-enabled-verification)*<br/>
8.3.3. *[Partial Read](#833-partial-read)*<br/>
8.3.3.1. [Random Byte Ranges Read](#8331-random-byte-ranges-read)<br/>
8.3.3.1.1. [Single Random Byte Range Read](#93311-single-random-byte-range-read)<br/>
8.3.3.1.2. [Multiple Random Byte Ranges Read](#93312-multiple-random-byte-ranges-read)<br/>
8.3.3.2. [Fixed Byte Ranges Read](#8332-fixed-byte-ranges-read)<br/>
8.3.3.2.1. [Read From offset of N bytes to the end](#83321-read-from-offset-of-n-bytes-to-the-end)<br/>
8.3.3.2.2. [Read Last N bytes](#83322-read-last-n-bytes)<br/>
8.3.3.2.3. [Read Bytes from N1 to N2](#83323-read-bytes-from-n1-to-n2)<br/>
8.3.3.2.4. [Read Multiple Fixed Ranges](#83324-read-multiple-fixed-ranges)<br/>
8.4. **<i>[Update](#84-update)</i>**<br/>
8.4.1. *[Update by Overwrite](#841-update-by-overwrite)*<br/>
8.4.2. *[Random Ranges Update](#842-random-ranges-update)*<br/>
8.4.2.1. [Single Random Range Update](#8431-single-random-range-update)<br/>
8.4.2.2. [Multiple Random Ranges Update](#8432-multiple-random-ranges-update)<br/>
8.4.3. *[Fixed Ranges Update](#843-fixed-ranges-update)*<br/>
8.4.3.1. [Overwrite from the offset of N bytes to the end](#8431-overwrite-from-the-offset-of-n-bytes-to-the-end)<br/>
8.4.3.2. [Overwrite Last N bytes](#8432-overwrite-last-n-bytes)<br/>
8.4.3.3. [Overwrite Bytes from N1 to N2](#8433-overwrite-bytes-from-n1-to-n2)<br/>
8.4.3.4. [Append](#8434-append)<br/>
8.4.3.5. [Multiple Fixed Ranges Update](#8435-multiple-fixed-ranges-update)<br/>
8.5. **<i>[Delete](#85-delete)</i>**<br/>
9. **[Scenarios](#9-scenarios)**<br/>
9.1. **<i>[Scenarios DSL](#91-scenarios-dsl)</i>**<br/>
9.2. **<i>[Default Scenario](#92-default-scenario)</i>**<br/>
9.3. **<i>[Custom Scenario File](#93-custom-scenario-file)</i>**<br/>
9.4. **<i>[Scenario Step Configuration](#94-scenario-step-configuration)</i>**<br/>
9.4.1. *[Override the Default Configuration in the Scenario](#941-override-the-default-configuration-in-the-scenario)*<br/>
9.4.2. *[Step Configuration Reusing](#942-step-configuration-reusing)*<br/>
9.4.3. *[Reusing The Items in the Scenario](#943-reusing-the-items-in-the-scenario)*<br/>
9.4.4. *[Environment Values Substitution in the Scenario](#944-environment-values-substitution-in-the-scenario)*<br/>
9.5. **<i>[Scenario Step Types](#95-scenario-step-types)</i>**<br/>
9.5.1. *[Shell Command](#951-shell-command)*<br/>
9.5.1.1. [Blocking Shell Command](#9511-blocking-shell-command)<br/>
9.5.1.2. [Non-blocking Shell Command](#9512-non-blocking-shell-command)<br/>
9.5.2. *[Load Step](#952-load-step)*<br/>
9.5.3. *[Precondition Load Step](#953-precondition-load-step)*<br/>
9.5.4. *[Weighted Load Step](#954-weighted-load-step)*<br/>
9.5.5. *[Chain Load Step](#955-chain-load-step)*<br/>
10. **[Storage Driver](#10-storage-driver)**<br/>
10.1. **<i>[Distributed Storage Drivers](#101-distributed-storage-drivers)</i>**<br/>
10.1.1. *[Single Local Separate Storage Driver Service](#1011-single-loacal-separate-storage-driver-service)*<br/>
10.1.2. *[Many Local Separate Storage Driver Services (at different ports)](#1012-many-local-separate-storage-driver-services-at-different-ports)*<br/>
10.1.3. *[Single Remote Storage Driver Service](#1013-single-remote-storage-driver-service)*<br/>
10.1.4. *[Many Remote Storage Driver Services](#1014-many-remote-storage-driver-services)*<br/>
10.2. **<i>[Configure the Storage](#102-configure-the-storage)</i>**<br/>
10.2.1. *[Create Auth Token On Demand](#1021-create-auth-token-on-demand)*<br/>
10.2.2. *[Create Destination Path On Demand](#1022-create-destination-path-on-demand)*<br/>
10.3. **<i>[Filesystem Storage Driver](#103-filesystem-storage-driver)</i>**<br/>
10.4. **<i>[Network Storage Driver](#104-network-storage-driver)</i>**<br/>
10.4.1. *[Node Balancing](#1041-node-balancing)*<br/>
10.4.2. *[SSL/TLS](#1042-ssltls)*<br/>
10.4.3. *[Connection Timeout](#1043-connection-timeout)*<br/>
10.4.4. *[I/O Buffer Size Adjustment for Optimal Performance](#1044-io-buffer-size-adjustment-for-optimal-performance)*<br/>
10.4.5. *[HTTP Storage Driver](#1045-http-storage-driver)*<br/>
10.4.5.2. [Atmos](#10452-atmos)<br/>
10.4.5.3. [S3](#10453-s3)<br/>
10.4.5.3.1. [EMC S3 extensions](#104531-emc-s3-extensions)<br/>
10.4.5.4. [Swift](#10454-swift)<br/>


# 1. Configuration

## 1.1. Configuration Syntax

See the [[Configuration|v3.6 Configuration]] documentation for syntax details.

## 1.2. Aliasing

See the [[Configuration Aliasing|v3.6 Configuration#3-aliasing]] documentation for details.

## 1.3. Parameterized Configuration

See the [[Configuration Parametrization|v3.6 Configuration#2-parametrization]] documentation for details.

### 1.3.1. Parameterized HTTP Headers.

See [[Variable HTTP Headers Values|v3.6 Configuration#242-variable-http-headers-values]] for details.

### 1.3.2. Parameterized Output Path.

See [[Variable Items Output Path|v3.6 Configuration#241-variable-items-output-path]] for details.

# 1.3.3. Multiuser Load.

See [[Multiuser Load|v3.6 Configuration#243-multiuser-load]] for details.

# 2. Items

A storage may be loaded using *Items* and some kind of operation (CRUD).
The only thing which item has is a mutable name.

## 2.1. Item Types

Mongoose supports different item types:
- A data (object, file) item
- A path (directory, bucket, container) item
- A token item

### 2.1.1. Data Items

The data items type is used by default.

#### 2.1.1.1. Fixed Size Data Items

Fixed data item size is used by default. The default size value is 1MB.
```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --item-data-size=10KB
```

##### 2.1.1.1.1. Empty Data Items

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --item-data-size=0
```

#### 2.1.1.2. Random Size Data Items

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --item-data-size=5MB-15MB
```

#### 2.1.1.2.1. Biased Random Size Data Items

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --item-data-size=0-100MB,0.2
```

Note:
* The bias value is appended to the range after the comma (0.2 in the example above).
* The generated value is biased towards the high end if bias value is less than 1.
* The generated value is biased towards the low end if bias value is more than 1.

### 2.1.2. Path Items

The path items type may be useful to work with directories/buckets/containers
(depending on the storage driver type used)

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --item-type=path
```

### 2.1.3. Token Items

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --item-type=token
```

## 2.2. Items Input

Items input is a source of the items which should be used to perform the operations (crete/read/etc).
The items input may be a file or a path which should be listed.

### 2.2.1. Items Input File

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --item-input-file=<PATH_TO_ITEMS_FILE> ...
```

### 2.2.2. Items Path Listing Input

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --item-input-path=/bucket1 ...
```

### 2.2.3. New Items Input

New items input is used automatically if no other items input is configured.
Useful to create a new random data on the storage.

### 2.2.3.1. Random Item Ids

Random item ids are used by default. The collision probability is highly negligible (2<sup>-63</sup>-1).

### 2.2.3.2. Ascending Item Ids

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --item-naming-type=asc ...
```

### 2.2.3.3. Descending Item Ids

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --item-naming-type=desc ...
```

### 2.2.3.4. Items Id Prefix

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --item-naming-prefix=item_ ...
```

### 2.2.3.5. Items Id Radix

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --item-naming-radix=10 ...
```

### 2.2.3.6. Items Id Offset

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --item-naming-offset=12345 ...
```

### 2.2.3.7. Items Id Length

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --item-naming-length=13 ...
```

## 2.3. Items Output

### 2.3.1. Items Output Delay

The processed items info may be output with a specified delay. This may be useful to test a storage
replication using the "chain" step (see the scenario step types for details). The configured delay is
in seconds.

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --item-output-delay=60
```

### 2.3.2. Items Output File

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --item-output-file=items.csv
```

### 2.3.3. Items Output Path

#### 2.3.3.1. Constant Items Output Path

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --item-output-path=/bucketOrContainerOrDir
```

#### 2.3.3.2. Pattern Items Output Path

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --item-output-path=/mnt/storage/%p\{16\;2\} ...
```

# 3. Content

While creating/verifying/updating the data items Mongoose is able to use different data input types.
By default it uses the memory buffer filled with random data. Also Mongoose is able to fill this
data input buffer with a data from any external file.

## 3.1. Uniform Random Data Payload

The uniform random data payload is used by default. It uses the
configurable seed number to pre-generate some amount (4MB) of the random
uniform data. To use the custom seed use the following option:

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --item-data-content-seed=5eed42b1gb00b5
```

## 3.2. Payload From the External File

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --item-data-content-file=<PATH_TO_CONTENT_FILE>
```

# 4. Concurrency

The concurrency metric has different meaning for different storage driver types:

* **File** Storage Driver

  A count simultaneously open files being written/read/etc.

* **Netty-based** Storage Driver and its derivatives

  A count of simultaneous active connections (channels).

**Note:**
> System's max open files limit may be required to increased to use high concurrency levels:
> ```bash
> ulimit -n 1048576
> ```

## 4.1. Limited Concurrency

The default concurrency limit is 1. Mongoose is able to use a custom concurrency limit:

**Example:**
```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --load-limit-concurrency=1000000
```

## 4.2. Unlimited Concurrency

The concurrency limit may be disabled (by setting its value to 0)

**Example**:
```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --load-limit-concurrency=0
```

**Note**:
> It may be useful to limit the rate to measure the actual concurrency
> while it's not limited

# 5. Recycle Mode

Recycle mode forces the step to recycle the I/O tasks executing them again and again. It may be
useful to perform read/update/append/overwrite the objects/files multiple times each.

**Note:**
> The recycle feature is applicable to **read** and **update** load types only.

**Example:**
```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --load-generator-recycle-enabled
```

For details see the [[Recycle Mode|v3.6 Recycle Mode]] specification.

# 6. Test Steps

Test step is an unit of metrics reporting and test execution control.

For each test step:
- total metrics are calculated and reported
- limits are configured and controlled

## 6.1. Test Steps Identification

By default Mongoose generates the test step id for each new test step. The step id is used
as the output log files parent directory name. It may be useful to override the default step name
with a descriptive one.

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --test-step-id=myTest1
```

## 6.2. Test Steps Limitation

### 6.2.1. Steps Are Infinite by Default

A test step tries to execute eternally if its item input is infinite and no other limits are configured.

### 6.2.2. Limit Step by Processed Item Count

To make a test step to process (CRUD) no more than 1000 items, for example:
```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --test-step-limit-count=1000
```

### 6.2.3. Limit Step by Rate

It may be useful to limit the rate by a max number of operations per second. The rate
limit value may be a real number, for example 0.01 (op/s).

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --load-rate-limit=1234.5
```

### 6.2.4. Limit Step by Processed Data Size

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --test-step-limit-size=123GB
```

### 6.2.5. Limit Step by Time

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --test-step-limit-time=15m
```

### 6.2.6. Limit Step by End of Items Input

Any test step configured with the valid items input should finish (at most) when all the items got
from the input are processed (copied/read/updated/deleted). This is true only if test step is not
configured to recycle the I/O tasks again and again (recycle mode is disabled).

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --item-input-[file|path]=<INPUT_FILE_OR_PATH> ...
```

In the example above, the test step will finish when all items from the specified items file are
processed.

# 7. Output

## 7.1. Console Coloring

By default, the standard output contains the color codes for better readability.
To disable the standard output color codes use the following option:

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --output-color=false
```

## 7.2. Metrics Output

### 7.2.1. Average Metrics Output

#### 7.2.1.1. Average Metrics Output Period

The default time interval between the metric outputs is 10s. This value may be changed.

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --output-metrics-average-period=1m
```

#### 7.2.1.2. Average Metrics Output Persistence

By default each load step outputs the current metrics periodically to the console (as a table record) and into the log file.
To disable the average metrics file output use the following option:

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --output-metrics-average-persist=false
```

#### 7.2.1.3. Average Metrics Table Header Output Period

By default the table header is displayed every 20 records.
To change this number, use the following option:

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --output-metrics-average-table-header-period=50
```

### 7.2.2. Summary Metrics Output

By default each load step outputs the summary metrics at its end to the console and into the log file.
To disable the summary metrics file output use the following option:

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --output-metrics-summary-persist=false
```

## 7.2.3. Trace Metrics Output

There's an ability to log the info about every I/O operation been executed versus a storage.
This kind of info is called "I/O trace". To output the I/O trace records into the log file,
specify the following option:

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --output-metrics-trace-persist
```

## 7.2.4. Metrics Accounting Threshold

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --test-step-metrics-threshold=0.95
```

# 8. Load Types

## 8.1. Noop

The "dry run" operation type. Does everything except actual storage I/O. May be useful to measure
the Mongoose's internal performance.

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --noop
```

## 8.2. Create

Create load type is used by default. The behavior may differ on the other configuration parameters.

### 8.2.1. Create New Items

"Create" performs writing new items to a storage by default.

### 8.2.2. Copy Mode

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --item-input-[file|path]=<INPUT_FILE_OR_PATH> --item-output-path=/bucketOrDir
```

## 8.3. Read

### 8.3.1. Read With Disabled Validation

Read operations don't perform a content validation by default.
```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --read ...
```

### 8.3.2. Read With Enabled Validation

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --read --item-data-verify ...
```

### 8.3.3. Partial Read

#### 8.3.3.1. Random Byte Ranges Read

##### 8.3.3.1.1. Single Random Byte Range Read

```bash
java -jar mongoose.jar \
	--read \
	--item-data-ranges-random=1 \
	--item-input-file=items.csv \
	...
```

##### 8.3.3.1.2. Multiple Random Byte Ranges Read

```bash
java -jar mongoose.jar \
	--read \
	--item-data-ranges-random=5 \
	--item-input-file=items.csv \
	...
```

#### 8.3.3.2. Fixed Byte Ranges Read

##### 8.3.3.2.1. Read from offset of N bytes to the end

Example: read the data items partially (from offset of 2KB to the end):
```bash
java -jar mongoose.jar \
	--read \
	--item-data-ranges-fixed=2KB- \
	--item-input-file=items.csv \
	...
```

##### 8.3.3.2.2. Read Last N bytes

Example: read the last 1234 bytes of the data items:
```bash
java -jar mongoose.jar \
	--read \
	--item-data-ranges-fixed=-1234 \
	--item-input-file=items.csv \
	...
```

##### 8.3.3.2.3. Read Bytes from N1 to N2

Example: partially read the data items each in the range from 2KB to 5KB:
```bash
java -jar mongoose.jar \
	--read \
	--item-data-ranges-fixed=2KB-5KB \
	--item-input-file=items.csv \
	...
```

##### 8.3.3.2.4. Read Multiple Fixed Ranges

```bash
java -jar mongoose.jar \
	--read \
	--item-data-ranges-fixed=0-1KB,2KB-5KB,8KB- \
	--item-input-file=items.csv \
	...
```

## 8.4. Update

### 8.4.1. Update by Overwrite

To overwrite the data items it's necessary to skip the byte ranges
configuration for the "update" load type. It may be also useful to
specify the different content source to overwrite with different data:

```bash
java -jar mongoose.jar \
	--update \
	--item-data-content-file=custom/content/source/file.data \
	--item-input-file=items2overwrite.csv \
	--item-output-file=items_overwritten.csv \
	...
```

If there's file with custom content source available it's possible to
use also custom content generation seed (hex):

```bash
java -jar mongoose.jar \
	--update \
	--item-data-content-seed=5eed42b1gb00b5 \
	--item-input-file=items2overwrite.csv \
	--item-output-file=items_overwritten.csv \
	...
```

### 8.4.2. Random Ranges Update

#### 8.4.2.1. Single Random Range Update

```bash
java -jar mongoose.jar \
	--update \
	--item-data-ranges-random=1 \
	--item-input-file=items2update.csv \
	--item-output-file=items_updated.csv \
	...
```

#### 8.4.2.2. Multiple Random Ranges Update

Random ranges update example:
```bash
java -jar mongoose.jar \
	--update \
	--item-data-ranges-random=5 \
	--item-input-file=items2update.csv \
	--item-output-file=items_updated.csv \
	...
```

### 8.4.3. Fixed Ranges Update

#### 8.4.3.1. Overwrite from the offset of N bytes to the end

```bash
java -jar mongoose.jar \
	--update \
	--item-data-ranges-fixed=2KB- \
	--item-input-file=items2overwrite_tail2KBs.csv \
	--item-output-file=items_with_overwritten_tails.csv \
	...
```

#### 8.4.3.2. Overwrite Last N bytes

Example: overwrite the last 1234 bytes of the data items:
```bash
java -jar mongoose.jar \
	--update \
	--item-data-ranges-fixed=-1234 \
	--item-input-file=items2overwrite_tail2KBs.csv \
	--item-output-file=items_with_overwritten_tails.csv \
	...
```

#### 8.4.3.3. Overwrite Bytes from N1 to N2

Example: overwrite the data items in the range from 2KB to 5KB:
```bash
java -jar mongoose.jar \
	--update \
	--item-data-ranges-fixed=2KB-5KB \
	--item-input-file=items2overwrite_range.csv \
	--item-output-file=items_overwritten_in_the_middle.csv \
	...
```

#### 8.4.3.4. Append

Example: append 16KB to the data items:
```bash
java -jar mongoose.jar \
	--update \
	--item-data-ranges-fixed=-16KB- \
	--item-input-file=items2append_16KB_tails.csv \
	--item-output-file=items_appended.csv \
	...
```

#### 8.4.3.5. Multiple Fixed Ranges Update

```bash
java -jar mongoose.jar \
	--update \
	--item-data-ranges-fixed=0-1KB,2KB-5KB,8KB- \
	--item-input-file=items2update.csv \
	--item-output-file=items_updated.csv \
	...
```

## 8.5. Delete

```bash
java -jar mongoose.jar \
	--delete \
	--item-input-file=items2delete.csv \
	...
```

# 9. Scenarios

See the [[Scenarios Reference|v3.6 Scenarios]] for details.

## 9.1. Scenarios DSL

See the [[Scenarios DSL Reference|v3.6-Scenarios#2-dsl]] for details.

## 9.2. Default Scenario

Mongoose can not run without a scenario. So it uses the default scenario
implicitly if the scenario file to run is not specified obviously. The
file containing the default scenario is located at ```scenario/default.json```.

The default scenario contents:
```javascript
Load.run();
```

## 9.3. Custom Scenario File

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar \
    --test-scenario-file=<PATH_TO_SCENARIO_FILE>
```

## 9.4. Scenario Step Configuration

### 9.4.1. Override the Default Configuration in the Scenario

The configuration values from the step's configuration override the
default configuration and the CLI options:

```javascript
var loadStepConfig = {
    "test": {
        "step": {
            "limit": {
                "time": "1m"
            }
        }
    }
};

Load
    .config(loadStepConfig)
    .run();
```

In the case above doesn't matter which `test-step-id` CLI option is
specified, the value "step_0" will override this.

### 9.4.2. Step Configuration Reusing

The configuration values from the step's configuration are inherited by
all child steps (and possibly overridden).

```javascript
var loadStepConfig1 = {
    "test": {
        "step": {
            "limit": {
                "time": "1m"
            }
        }
    }
};
var loadStepConfig2 = {
    "test": {
        "step": {
            "limit": {
                "count": 100000
            }
        }
    }
};

var loadStep1 = Load.config(loadStepConfig1);
var loadStep2 = loadStep1.config(loadStepConfig2);

loadStep1.run();
loadStep2.run();
```

### 9.4.3. Reusing The Items in the Scenario

```javascript
var preconditionLoadStepConfig = {
    "item" : {
       "output" : {
          "file" : "items.csv"
       }
    }
    ...
};
var loadStepConfig = {
    "item" : {
       "input" : {
          "file" : "items.csv"
       }
    }
    ...
};

Load
    .config(preconditionLoadStepConfig)
    .run();

Load
    .config(loadStepConfig)
    .run();
```

### 9.4.4. Environment Values Substitution in the Scenario

To run the scenario below please define `ITEM_INPUT_FILE` either
`ITEM_INPUT_PATH` environment variable and the `ITEM_OUTPUT_PATH`
environment variable

```javascript
var CopyLoadUsingEnvVars = CreateLoad
    .config(
        {
            "item": {
                "input": {
                    "file": ITEM_INPUT_FILE,
                    "path": ITEM_INPUT_PATH
                },
                "output": {
                    "path": ITEM_OUTPUT_PATH
                }
            }
        }
    );

CopyLoadUsingEnvVars.run();
```

## 9.5. Scenario Step Types

### 9.5.1. Shell Command

#### 9.5.1.1. Blocking Shell Command

Sleep between the steps for example:

```javascript
Command
    .value("echo Hello world!")
    .run();

Command
    .value("ps alx | grep java")
    .run();
```

#### 9.5.1.2. Non-blocking Shell Command

```json
var command1 = Command.value("echo Hello world!");

var command2 = Command.value("ps alx | grep java");

Parallel
    .step(command1)
    .step(command2)
    .run();
```

### 9.5.2. Load Step

See the [[Load Step|v3.6 Scenarios#312-load]] documentation for a details.

### 9.5.3. Parallel Step

Executes the child steps in parallel

```javascript
var loadStep1 = Load.config(...);
var loadStep2 = Load.config(...);

Parallel
    .step(loadStep1)
    .step(loadStep2)
    .run();
```

See the [[Parallel Step|v3.6 Scenarios#313-parallel]] documentation for a details.

### 9.5.4. Weighted Load Step

For details see [[Weighted Load Reference|v3.6 Scenarios#314-weighted-load]].

### 9.5.5. Chain Load Step

For details see [[Chain Load Reference|v3.6 Scenarios#315-chain-load]].

# 10. [[Storage Driver|v3.6-Architecture#storage-driver]]

## 10.1. Distributed Storage Drivers

Mongoose is able to work in the so called distributed mode what allows to scale out the load
performed on a storage. In the distributed mode there's a instance controlling the distributed load
execution progress. This instance usually called "controller" and usually should be running on a
dedicated host. The controller aggregates the results from the remote (usually) storage driver
services which perform the actual load on the storage.

### 10.1.1. Single Local Separate Storage Driver Service

- Start the storage driver service:
```bash
java -jar <MONGOOSE_DIR>/mongoose-storage-driver-service.jar
```

- Start the controller:
```
java -jar <MONGOOSE_DIR>/mongoose.jar \
    --storage-driver-remote \
    ...
```

### 10.1.2. Many Local Separate Storage Driver Services (at different ports)

- Start the 1st storage driver service:
```bash
java -jar <MONGOOSE_DIR>/mongoose-storage-driver-service.jar \
    --storage-driver-port=1099
```

- Start the 1st storage driver service:
```bash
java -jar <MONGOOSE_DIR>/mongoose-storage-driver-service.jar \
    --storage-driver-port=1100
```

- Start the controller:
```
java -jar <MONGOOSE_DIR>/mongoose.jar \
	--storage-driver-remote \
	--storage-driver-addrs=127.0.0.1:1099,127.0.0.1:1100 \
	...
```

### 10.1.3. Single Remote Storage Driver Service

- Start the storage driver service on one host:
```bash
java -jar <MONGOOSE_DIR>/mongoose-storage-driver-service.jar
```

- Start the controller on another host:
```
java -jar <MONGOOSE_DIR>/mongoose.jar \
	--storage-driver-remote \
	--storage-driver-addrs=<DRIVER_IP_ADDR> \
	...
```

### 10.1.4. Many Remote Storage Driver Services

- Start the storage driver service on each host using the following command:
```bash
java -jar <MONGOOSE_DIR>/mongoose-storage-driver-service.jar
```

- Start the controller on another host:
```
java -jar <MONGOOSE_DIR>/mongoose.jar \
	--storage-driver-remote \
	--storage-driver-addrs=<DRIVER1>,<DRIVER2>,... \
	...
```

## 10.2. Configure the Storage

Users would like to not to care if some configuration parameters are not specified explicitly or a
target storage is not fully prepared for the test. For example missing bucket (S3), subtenant
(Atmos), target directory, etc. Mongoose will try to configure/create such things automatically on
demand and cache them for further reuse by other I/O tasks.

Note:
> Mongoose test step creates a kind of *knowledge* about the storage which may become irrelevant.
> For example, Mongoose creates/checks the target bucket once and remembers the result.
> If the bucket is deleted by 3rd side during the Mongoose test step it will continue to consider
> the bucket existing despite the arising failures.

### 10.2.1. Create Auth Token On Demand

If no authentication token is specified/exists Mongoose tries to create it.
This functionality is currently implemented for Atmos and Swift storage drivers.

### 10.2.2. Create Destination Path On Demand

If no output path is specified/exists Mongoose tries to create it (create destination directory/bucket/container).
This functionality is currently implemented for filesystem, S3 and Swift storage drivers.

## 10.3. Filesystem Storage Driver

Please refer to the storage driver's [readme](https://github.com/emc-mongoose/mongoose-storage-driver-fs)

## 10.4. Network Storage Driver

### 10.4.1. Node Balancing

Mongoose uses the round-robin way to distribute I/O tasks if multiple storage endpoints are used.
If a connection fail Mongoose will try to distribute the active connections equally among the
endpoints.

### 10.4.2. SSL/TLS

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar \
    --storage-net-ssl \
    --storage-net-node-port=9021 \
    ...
```

### 10.4.3. Connection Timeout

Sometimes the test is run against the storage via network and the storage endpoint may fail to
react on a connection. Mongoose should fail such I/O task and continue to go on. There's an ability
to set a response timeout which allows to interrupt the I/O task and continue to work.

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar \
    --storage-net-timeoutMillisec=100000 \
    ...
```

### 10.4.4. I/O Buffer Size Adjustment for Optimal Performance

Mongoose automatically adopts the input and output buffer sizes depending on the step info. For
example, for *create* I/O type the input buffer size is set to the minimal value (4KB) and the
output buffer size is set to configured data item size (if any). If *read* I/O type is used the
behavior is right opposite - specific input buffer size and minimal output buffer size. This
improves the I/O performance significantly. But users may set the buffer sizes manually.

Example: setting the *input* buffer to 100KB:
```bash
java -jar <MONGOOSE_DIR>/mongoose.jar \
    --storage-net-rcvBuf=100KB \
    ...
```

Example: setting the *output* buffer to 10MB:
```bash
java -jar <MONGOOSE_DIR>/mongoose.jar \
    --storage-net-sndBuf=10MB \
    ...
```

### 10.4.5. HTTP Storage Driver

#### 10.4.5.2. Atmos

Please refer to the storage driver's [readme](https://github.com/emc-mongoose/mongoose-storage-driver-atmos)

#### 10.4.5.3. S3

Please refer to the storage driver's [readme](https://github.com/emc-mongoose/mongoose-storage-driver-s3)

##### 10.4.5.3.1. EMC S3 Extensions

Please refer to the storage driver's [readme](https://github.com/emc-mongoose/mongoose-storage-driver-emc-s3)

#### 10.4.5.4. Swift

Please refer to the storage driver's [readme](https://github.com/emc-mongoose/mongoose-storage-driver-swift)
