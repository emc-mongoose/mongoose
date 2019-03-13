# Contents

## 2019
* [5.0.0](#anchor-500) TBD
* ~~4.1.2~~
* [4.1.1](#anchor-411) 2019-01-16

## 2018
* [4.1.0](#anchor-410) 2018-12-12
* [4.0.3](#anchor-403) 2018-11-28
* [4.0.2](#anchor-402) 2018-11-13
* [4.0.1](#anchor-401) 2018-10-08
* [4.0.0](#anchor-400) 2018-10-01
* [3.6.2](#anchor-362) 08/08/18
* [3.6.1](#anchor-361) 03/26/18

## 2017
* [3.6.0](#anchor-360) 12/24/17
* [3.5.1](#anchor-351) 11/05/17
* [3.4.2](#anchor-342) 08/18/17
* [3.3.0](#anchor-330)
* [3.2.1](#anchor-321)
* [3.1.0](#anchor-310) 01/31/17
* [3.0.5](#anchor-305) 01/16/17

## 2016
* [2.5.6](#anchor-256) 12/21/16
* [2.4.3](#anchor-243) 08/19/16
* [2.2.0](#anchor-220) 06/01/16
* [2.1.0](#anchor-210) 05/13/16
* [2.0.0](#anchor-200) 05/06/16
* [1.4.1](#anchor-141)
* [1.4.0](#anchor-140)
* [1.3.2](#anchor-132) 02/26/16
* [1.3.0](#anchor-130) 02/18/16
* [1.2.2](#anchor-122) 01/21/16

## 2015
* [1.2.0](#anchor-120) 12/19/15
* [1.1.3](#anchor-113) 11/18/15
* [1.1.2](#anchor-112) 11/18/15
* [1.1.1](#anchor-111) 11/13/15
* [1.1.0](#anchor-110)
* [1.0.2](#anchor-102) 10/26/15
* [1.0.1](#anchor-101) 10/15/15
* [1.0.0](#anchor-100) 10/08/15
* [0.9.0](#anchor-090) 08/27/15
* [0.8.0](#anchor-080) 05/15/15
* [0.7.0](#anchor-070) 04/28/15
* [0.6.4](#anchor-064) 03/30/15
* [0.6.3](#anchor-063)
* [0.6](#anchor-06) 03/02/15
* [0.5](#anchor-05) 01/13/15

## 2014
* [0.4](#anchor-04) 11/27/14
* [0.3](#anchor-03) 10/30/14
* [0.2](#anchor-02) 10/10/14
* [0.1.6](#anchor-016) 09/29/14
* [0.1.5](#anchor-015) 09/22/14
* [0.1.4](#anchor-014) 09/17/14
* [0.1.3](#anchor-013) 08/15/14

# 5.0.0

## New Features and Enhancements

### Functional

1. [Expression Language](../../base/src/main/java/com/emc/mongoose/config/el/README.md) is based on the JSR-341
standard.
2. Custom URI query for the HTTP requests.
3. S3 V4 authentication.
4. Swift V2 authentication support.

### Non-functional

1. [Migration to Java 11](java11/) caused the significant performance improvement and allowed the developers to use the
cutting edge language features.
2. Use YAML instead of JSON in all configuration files. Issue [link](https://mongoose-issues.atlassian.net/browse/GOOSE-1327).
3. Handle the unhappy cases more correctly. Issue [link](https://mongoose-issues.atlassian.net/browse/GOOSE-1319).

## Fixed Bugs

| Id | Description |
|----|-------------|
| [GOOSE-1279](https://mongoose-issues.atlassian.net/browse/GOOSE-1279) | Negative actual concurrency reporting |
| [GOOSE-1319](https://mongoose-issues.atlassian.net/browse/GOOSE-1316) | Swift storage driver: handle multi byte ranges read responses correctly |

# 4.1.1

## New Features and Enhancements

### Functional

1. Run API improvement: allow to start the new scenario run with the implicit default configuration/scenario

### Non-functional

1. Automated tests for the MS Windows environment

## Fixed Bugs

| Id | Description |
|----|-------------|
| [GOOSE-1265](https://mongoose-issues.atlassian.net/browse/GOOSE-1265) | Log the config for each load step |
| [GOOSE-1315](https://mongoose-issues.atlassian.net/browse/GOOSE-1315) | Do not create any log files under the `${ctx:step_id}` directory |
| [GOOSE-1317](https://mongoose-issues.atlassian.net/browse/GOOSE-1317) | Add the CORS related headers to the Remote API responses |

# 4.1.0

## New Features and Enhancements

### Functional

#### 1. [Remote API](doc/interfaces/api/remote).

Implemented for the node mode. The available actions are:
1. Get Mongoose node configuration defaults
2. Get Mongoose node configuration schema
3. Run a scenario
4. Check the scenario if it is running
5. Stop the scenario if it is running
6. Get the specified log messages
7. Get the performance metrics for all running load steps in the Prometheus exporter format

Note that the metrics are being supplied in the [Prometheus exporter format](https://prometheus.io/docs/instrumenting/exposition_formats).

#### 2. [Configurable Timing Metrics Quantiles](doc/interfaces/api/remote#611-custom-quantiles)

It's possible to supply the configurable quantiles for the timing metrics (duration/latency). This will work for all
the timing metrics supplied via the Remote API. The legacy metrics output way (standard output and log files) are
not affected and will continue to report the fixed set of predefinded quantiles: low quartile (0.25), median (0.5)
and high quartile (0.75).

### Non-functional

#### 1. Mongoose source code migrated to Git Lab

New source code repository location: https://gitlab.com/emcmongoose/mongoose

#### 2. Git Lab CI is used instead of Travis CI

New CI location: https://gitlab.com/emcmongoose/mongoose/pipelines

## Fixed Bugs

| Id | Short Description |
|----|-------------------|
| [GOOSE-1284](https://mongoose-issues.atlassian.net/browse/GOOSE-1284) | \[S3] Bucket versioning checking request causes 403 response |
| [GOOSE-1293](https://mongoose-issues.atlassian.net/browse/GOOSE-1293) | Incorrect console rate output |
| [GOOSE-1301](https://mongoose-issues.atlassian.net/browse/GOOSE-1293) | Auto installer overwrites the manually changed defaults every run

# 4.0.3

## Fixed Bugs

| Id | Short Description |
|----|-------------------|
| SLTM-1223 | Insufficient configured threads

# 4.0.2

## New Features and Enhancements

#### 1. Configuration Layout Changes

| Old parameter name (v < 4.0.2)  | New parameter name (v >= 4.0.2)
|---------------------------------|--------------------------------
| storage-net-http-namespace      | storage-namespace

## Fixed Bugs

| Id | Short Description |
|----|-------------------|
| [BASE-1282](https://mongoose-issues.atlassian.net/browse/BASE-1282) | Few load operations results are lost sometimes

# 4.0.1

## Fixed Bugs

| Id | Short Description |
|----|-------------------|
| [BASE-1276](https://mongoose-issues.atlassian.net/browse/BASE-1276) | Fails to create the file if the parent directory doesn't exist |
| [BASE-1277](https://mongoose-issues.atlassian.net/browse/BASE-1277) | Hang for some time when a distributed load step has already finished |

# 4.0.0

## New Features and Enhancements

### Functional

#### 1. P2P Distributed Mode

The new distributed mode design is based on P2P (peer-to-peer) principles such as task/workload slicing (partitioning)
and independent execution. This allows to choose any node from the set to initiate the run. Also there's no more need to
dedicate the host for the controller. The remote API is also available to deploy applications on top of Mongoose node
set such as GUI.

#### 2. Automated Installer

New Mongoose is delivered as a single jar instead of tarball. This jar installs all the required files automatically if
needed in the user home directory. Also, the installer detects the extensions and installs them too.

#### 3. Extensible Configuration

Some extensions require specific configuration options. To support the specific configuration options the extensible
configuration was implemented. The new [external library](https://github.com/akurilov/confuse) is used for this purpose.
The configuration is assembled dynamically, including the sub-configurations provided by the extensions resolved in the
runtime. **Note** that command line arguments shouldn't be used to specify the dictionary type values like the custom
HTTP headers.

### Non-functional

1. Mongoose v4.0.0 doesn't work with deprecated JSON scenarios anymore. So the tool converting the Mongoose v3.x
scenarios to v4.0.0 scenarios is provided: https://github.com/emc-mongoose/scenario-converter-3to4

2. New public [issue tracker (Jira)](https://mongoose-issues.atlassian.net/browse/BASE) is introduced instead of GitHub
issue tracker.

## Fixed Bugs

| Id | Short Description |
|----|-------------------|
| [BASE-1226](https://mongoose-issues.atlassian.net/browse/BASE-1226) | Load operations results queue contains the unhandled elements
| SLTM-1164 | Ugly error message
| SLTM-1177 | Reported duration/latency min/max values are not absolute
| SLTM-1185 | Configuration: failed to convert string value to a list of strings
| SLTM-1195 | SSL performance degradation after migration to Netty
| SLTM-1213 | Incorrect final metrics : latency > duration

# 3.6.2

## Fixed Bugs

* (1204) Storage node failover support

# 3.6.1

## Fixed Bugs

* (1175) Mongoose reports and Descriptive Statistics discrepancies

# 3.6.0

## New Features and Enhancements

### Functional

1. **[[Extensions Mechanism|v3.6 Extensions]]**

    The new simple way to use a custom scripting engine either a storage
    driver implementation. Just put the extension jar file(s) into the
    `ext` directory of Mongoose and it's ready to use.

2. **[[JSR-223 Compliant Scenario Engine|v3.6 Scenarios]]**

    Powerful scripting capability using any language supporting JSR-223
    standard. Javascript is proposed as default scenarios language.

### Non-functional

1. All storage driver implementations moved to the separate projects
    under the same [GitHub organization](https://github.com/emc-mongoose).
    The list of the storage drivers supported currently:
    1. [EMC Atmos](https://github.com/emc-mongoose/mongoose-storage-driver-atmos)
    2. [EMC S3](https://github.com/emc-mongoose/mongoose-storage-driver-emc-s3) (extensions)
    3. [Filesystem](https://github.com/emc-mongoose/mongoose-storage-driver-fs)
    4. [HDFS](https://github.com/emc-mongoose/mongoose-storage-driver-hdfs) (**new**)
    5. [NFS](https://github.com/emc-mongoose/mongoose-storage-driver-nfs) (not working currently, under development)
    6. [Amazon S3](https://github.com/emc-mongoose/mongoose-storage-driver-s3) (generic)
    7. [OpenStack Swift](https://github.com/emc-mongoose/mongoose-storage-driver-swift)

    The **[[deployment|v3.6 Deployment]]** procedure significantly changed,
    so please keep attention on this. Note also that base/core mongoose
    distribution doesn't include any storage driver implementation since
    the new version.

2. The connection pool used by Mongoose moved to the separate project
[netty-connection-pool](https://github.com/akurilov/netty-connection-pool).

3. The source code repository name changed from `mongoose` to
`mongoose-base` due to storage driver implementations separation.
Accessing via the old name redirects to the new one.

4. Moved all content example files and all scenario files under the
common `example` directory.

5. Changed the default new items name length to 12 characters in
order to make the new item name characters distribution uniform (in
the space of the default radix of 36: [0-9a-z]). Previously all new
item names began with character "0" either "1" what was not uniform
enough.

## Fixed Bugs

* (1120) Path items input doesn't finish the listing
* (1147) Connection pool deadlock if a connection is dropped


# 3.5.1

## New Features and Enhancements

### Functional

1. [[Concurrency Model|v3.5 Concurrency Model]] reworked and enhanced.

    1. New [[Unlimited Concurrency|v3.5 Concurrency Model#actual-concurrency-measurement]] feature added.

        Allows to measure the actual maximum concurrency which the service/storage being test can sustain.

    2. [[Configurable I/O vs Calculations Balance|v3.5 Performance#2-tuning]].

    3. [Coroutines](https://github.com/akurilov/java-coroutines) library became a separate project.

2. [[Recycle Mode|v3.5 Recycle Mode]] reworked.

3. [[Monitoring API|v3.5 Monitoring API]].

4. Output configuration enhancements
   1. Generate the new test id for each new test if the test id is not
      configured. It's not recommended to use the `--test-step-id` CLI argument since v3.5.
   2. The logging configuration file is moved from the "user space"
      to the resource bundle. The logging is configured through the main configuration.
   3. New output options.
      1. [[Console output coloring flag|v3.5 General Output#coloring]].
      2. [[Average metrics time period|v3.5 Metrics Output#1-load-average]].
      3. [[Average metrics persistence flag|v3.5 Metrics Output#12-files]].
      4. [[Average metrics table header period|v3.5 Metrics Output#11-console]].
      5. [[Summary metrics persistence flag|v3.5 Metrics Output#22-files]].
      6. [[Trace metrics persistence flag|v3.5 Metrics Output#32-files]].
   4. Log the defaults content, launch command and the scenario content

5. Miscellaneous.
    1. Avoid flood of error messages.
    2. Docker image fix and size decrease.
    3. Fixed RMI port for the distributed mode and remote monitoring purposes.
    4. Set the corresponding ring buffer size if the content input file is configured

## Fixed Bugs

* (1036) Multiuser load case - destination path checking requests failing
* (1047) Recycling the load tasks order is unpredictable
* (1051) I/O trace log contains the records for the pending load tasks
* (1064) Max latency is higher than max duration
* (1065) File storage driver causes out of direct memory
* (1068) Connection leak on the connection pool close
* (1076) External XML results file reporting: include configured item size instead of transfer size
* (1085) Subsequent load step doesn't append the same items output file

## [[Performance|v3.5 Performance]]

Follow the link above for the details

## [[Configuration|v3.5 Configuration]]

| Old parameter name (v < 3.5.0)  | New parameter name (v >= 3.5.0)
|---------------------------------|--------------------------------
| N/A                             | load-service-threads
| N/A                             | storage-net-node-connAttemptsLimit
| N/A                             | item-data-ranges-concat
| load-circular                   | load-generator-recycle-enabled
| load-queue-size                 | load-generator-recycle-limit, storage-driver-queue-input, storage-driver-queue-output
| load-rate-limit                 | load-limit-rate
| storage-driver-concurrency      | load-limit-concurrency
| storage-driver-io-workers       | storage-driver-threads
| item-data-content-file          | item-data-input-file
| item-data-content-seed          | item-data-input-seed
| item-data-content-ring-cache    | item-data-input-layer-cache
| item-data-content-ring-size     | item-data-input-layer-size
| test-step-limit-rate            | load-rate-limit
| test-step-metrics-period        | output-metrics-average-period
| test-step-metrics-threshold     | output-metrics-threshold
| test-step-name                  | test-step-id
| test-step-precondition          | N/A (see the [[Metrics Output|v3.5 Metrics Output]] documentation for details)

# 3.4.2

Includes the v3.3 (won't be released) new functionality and fixes:<br/>
[[See v3.3 Release Notes|v3.3 Overview#release-notes]]

## New Features and Enhancements

### Functional

1. **[[Multipart Upload|v3.4 Multipart Upload]]**.

 The behavior may be improved due configurable batch size introduction.
 It's recommended to use the batch size of 1 for a multipart upload
 tests.

## Fixed Bugs

1. (823) SSL/TLS support regression after introducing Netty in 3.x.
2. (976) Latency measurement failures.
3. (979) Failed to stop the remaining I/O tasks.
4. (1003) Manual interruption - content source closed before I/O interrupted causing NPE.
5. (1006) File storage driver: verification after update fails in ~0.2% cases.
6. (1014) MPU/DLO: I/O buffer is adjusted to the whole item size but not the part size.
7. (1015) Unique results map contains not unique elements.
8. (1016) Scenario values substitution pattern matches only one occurrence per value.
9. (1044) Decrease the size of the Docker image.
10. (1045) Use fixed RMI port.
11. (1066) Missing method to set the custom HTTP headers

## Miscellaneous

1. **Standard Output Changes**

    * More neutral colors
    * Metrics are displayed as a table
    * Highlighted metrics
    * Highlighted errors counter with color depending on the errors ratio
    * Highlighted the operation type with color depending on the particular type

    | v3.3.x                          | v3.4.x                    |
    |---------------------------------|---------------------------|
    | ![v3.3](images/stdout-coloring-v3.3.png) | ![v3.4](images/stdout-coloring-v3.4.png) |


2. **Performance Improvements**

    * [[Coroutine-like|v3.4 Architecture#reentrant-service-tasks]] execution flow approach.
        This allowed to make the load generator concurrent and make the distributed mode linearly scalable.

    * Logging subsystem reworked to separate the log event streams more efficiently.

    * Conditional metrics snapshot recalculation decreases the CPU usage.

3. **Centralized Metrics Processing**

    In the new version all the metrics are processed by the "Load Monitor" component containing the
    "Metrics Manager" singleton instance. Previously, the load monitor component included the
    execution control functionality which is separated to the "Load Controller" component. Such
    architecture change gives the following advantages:

     * Joint interface for the metrics fetching by external tools
     * More readable combined metrics output

4. **[[Configuration layout change|v3.4 Configuration]]**

    Detailed configuration layout change info:

    | Old parameter name (v < 3.4.0)  | New parameter name (v >= 3.4.0)
    |---------------------------------|--------------------------------
    | N/A                             | item-data-content-ring-cache
    | item-data-content-ringSize      | item-data-content-ring-size
    | N/A                             | load-batch-size

5. **Advanced Test Coverage**

    The automated tests are run by Travis CI using multiple
    parameterized build stages. This allowed to increase the coverage
    approximately by 2 orders of magnitude.

# 3.3.0

Not released (cancelled)

## New Features and Enhancements

### Functional

1. **[[Multiuser Load Case|v3.3 Multiuser Load Case]]**

    Sometimes the performance depends on how many distinct users are using a storage concurrently.
    This may happen because each user may allocate some transient resources on the storage side.
    The feature is designed to test the performance in the multi-user environment.

## Fixed Bugs

TODO

## Miscellaneous

1. **Modularity**

    1. **[[Custom Storage Driver Plugin|v3.3 Custom Storage Driver]]**

        Initially Mongoose worked via Amazon S3 REST API. Then support for the EMC Atmos and OpenStack
        Swift API had been added. Finally Mongoose was redesigned to support the work with filesystem
        what made Mongoose load engine independent on the particular storage type. Currently adding
        the support of any other storage type supporting CRUD operations is not a trouble.

        It's time to enhance the range of the storage types supported by Mongoose. The way to do this is
        described in the documentation.

    2. The storage mock aka **"Nagaina" moved to the separate [project](https://github.com/emc-mongoose/nagaina)**

        That was done to make Mongoose even more modular and even lighter in size.
        Nagaina has its own docker image and released tarballs.

    3. Mongoose [[Components|v3.3 Components]] are available in the Maven Central Repo.

        This will allow to use to embed any Mongoose functionality into a user application.

2. **Configuration layout change**

    Detailed configuration layout change info:

    | Old parameter name (v < 3.3.0)  | New parameter name (v >= 3.3.0)
    |---------------------------------|--------------------------------
    | load-concurrency                | storage-driver-concurrency
    | load-job-name                   | test-step-name
    | load-limit-count                | test-step-limit-count
    | load-limit-rate                 | test-step-limit-rate
    | load-limit-size                 | test-step-limit-size
    | load-limit-time                 | test-step-limit-time
    | load-metrics-period             | test-step-metrics-period
    | load-metrics-precondition       | test-step-precondition
    | load-metrics-threshold          | test-step-metrics-threshold
    | scenario-file                   | test-scenario-file
    | *storage-type**                 | *storage-driver-type**
    | *storage-http-api**             | *storage-driver-type**

    (*) Note the last 2 parameters. Saying strictly they are completely deprecated and may not be
    mapped to the new `storage-driver-type` parameter safely. But the only case which may cause a
    failure is setting the deprecated parameter `storage-type` to the "http" value. This is not
    expected because in all previous versions `storage-type` was set to "http" by default.

# 3.2.1

## New Features and Enhancements

### Functional

1. [[Intermediate Statistics|v3.2-User-Guide#73-metrics-reporting-triggered-by-load-threshold]]
2. [[Mixed|v3.2-User-Guide#957-mixed-load-job]] and [[Weighted|v3.2-User-Guide#9572-weighted-load-job]] Load
3. [[Partial Read|v3.2-User-Guide#833-partial-read]] (*[[Design Specification|v3.2-Byte-Ranges-Read-and-Update]]*)
4. Atmos API Support. [[Subtenants load|v3.2-User-Guide#213-token-items]] functionality added (create, delete).
5. S3 API Support. [[Buckets load|v3.2-User-Guide#212-path-items]] functionality added (create, read, delete).
6. Swift API Support
    1. [[Tokens load|v3.2-User-Guide#213-token-items]] functionality (create)
    2. [[Containers load|v3.2-User-Guide#212-path-items]] functionality (create, read, delete)

## Fixed Bugs

* (891) Idle load job state is not reached on the manual interruption
* (892) Circular/Distributed count limit implementation is inaccurate
* (923) Mongoose 3.1.0 String Index out of bound exception
* (905) Quick fading when one of the target nodes went offline
* (937) Circular read - monitor is getting the results for active/pending I/O tasks
* (938) Circular load job hangs if all I/O tasks are failed and the count limit is set
* (944) Distributed mode issue while running in the Docker container
* (953) Not working: S3 MPU/Swift DLO

## Miscellaneous

1. Advanced the test coverage with new system tests.

    For detailed coverage info see the [[Functional Testing]] page.

2. Configuration layout change

    Some *"socket-..."* and *"storage-..."* configuration parameters moved under
    **"storage-net-..."** prefix/path. This was done to differentiate the FS storage
    driver configuration from Net storage driver configuration. Both CLI and scenario files
    backward compatibility is provided. Using deprecated configuration parameter names will cause
    warning messages. It's recommended to ***check the custom/user scenarios against the
    provided scenario schema*** (<MONGOOSE_DIR>/scenario/schema.json).

    Detailed configuration layout change info:

    | Old parameter name (v < 3.2.0)  | New parameter name (v >= 3.2.0)
    |---------------------------------|--------------------------------
    | socket-timeoutMilliSec          | storage-net-timeoutMilliSec
    | socket-reuseAddr                | storage-net-reuseAddr
    | socket-keepAlive                | storage-net-keepAlive
    | socket-tcpNoDelay               | storage-net-tcpNoDelay
    | socket-linger                   | storage-net-linger
    | socket-bindBacklogSize          | storage-net-bindBacklogSize
    | socket-interestOpQueued         | storage-net-interestOpQueued
    | socket-selectInterval           | storage-net-selectInterval
    | storage-ssl                     | storage-net-ssl
    | storage-http-api                | storage-net-http-api
    | storage-http-fsAccess           | storage-net-http-fsAccess
    | storage-http-headers            | storage-net-http-headers
    | storage-http-namespace          | storage-net-http-namespace
    | storage-http-versioning         | storage-net-http-versioning
    | storage-node-addrs              | storage-net-node-addrs
    | storage-node-port               | storage-net-node-port

# 3.1.0

## New Features and Enhancements

### Functional

1. Chain Operations

  The "chain" feature is designed for zone replication testing.
  It allows to write the objects and read them from other zone.
  Each object is being read immediately (or after configurable delay) after it was written.
  So a "chain" load job doesn't wait all objects to be written before reading starts.
  Chain load jobs are not limited with create/read operations
  but may also include update, delete in any comprehensible combination.
  The feature was implemented as an extension of the scenario engine.

### Performance

1. Specialized and more efficient non-blocking connection pool was implemented to replace Netty's bundled connection pool. The performance rates increased by 10-40 % depending on test configuration.

## Fixed Bugs

919. I/O tasks distribution among the storage drivers worked incorrectly.
920. I/O path inconsistency.

## Miscellaneous

1. Configuration JSON schema was ported from v2.x.x for validation purposes
2. Scenario JSON schema was ported from v2.x.x for validation purposes

# 3.0.5

## Major Improvements

* [[New Architecture|v3.0/Architecture]]

* [[Non-blocking filesystem storage driver|v3.0/NIO Storage Driver]]

* Netty-based storage driver for the HTTP

  Using Netty instead of Apache's HTTP core NIO library is expected to
  improve the performance

* [[User-friendly CLI|v3.0.0-CLI.md]]

## New Features

* [[Multi-Part Upload|v3.0/Multi Part Upload]]
* [[Mixed/Weighted Load Support|v3.0/Mixed and Weighted Load]]
* [[Fixed Byte Ranges Update|v3.0/Fixed Byte Ranges Update]]

## Essential Differences from 2.x.x

* Configured concurrent connection count (in the case of a non-FS storage driver) is shared between all the configured storage nodes. So if it's configured concurrency level of N1 and the count of the storage nodes is N2 then the total concurrent connection count is still N1 (before 3.0.0 it was N1 x N2).
* I/O trace logging is disabled by default by performance considerations. A user should manually modify the logging configuration file to enable the I/O trace logging.
* Processed items logging is disabled by default by performance considerations. A user should manually specify the item output file if needed.
* Content verification on read is disabled by default by performance considerations.

## Other Notable Changes

* Faster 64-bit data verification instead of old byte-by-byte approach.
* Epoll I/O mechanism is used if available.
* Configurable I/O traces fields output.
* New "NOOP" load type support useful to perform the dry runs.

# 2.5.6

The latest version of the discontinued 2.x.x branch

Closed tickets:
* long run hang in the distributed mode
* circular append in the distributed mode hangs

# 2.4.3

Closed tickets:
* "for" variable is not resolved when use with "headers"
* Mongoose 2.4.2 Making loop inclusive

# 2.2.0

General Notes
The normal development process was interrupted due to urgent new functionality requirement. This requirement is to be able to read back the files written to the variable destination path.

New Features and Enhancements
Core functionality
Ability to read back the items written to the variable path
Directories, files and objects may be written to a variable path described with width and depth. In order to read back these items spread across the directories and subdirectories Mongoose should persist the item path into the items.csv output file. Note that the format of the items.csv and perf.trace.csv output files are changed.

Non-functional
Turn back to the CRUD notation
There's a requirement from the users to speak CRUD instead of WRD. This is the notation used prior to v2.0.0 with some differences:

The Copy Mode is enabled if both load type is "Create" and the source items container set to a non-null value
"Append" becomes a case of "Update"

# 2.1.0

General Notes
This version includes the new functionality required urgently due to ECS field request.

Also, v2.1.0 is much better compatible with old versions (prior to v2.0.0) in comparison to v2.0.0.

New Features and Enhancements
Core functionality
SSL/TLS Support.
There are an urgent requirement to make the tool able to perform a load via HTTPS in addition to HTTP. The tool will trust any certificates returned by the server so no additional configuration is required. In order to start using this functionality it's neccessary to add only 2 additional options:
network.ssl=true
storage.port=<SECURE_PORT>
The implementation supports the following protocols:
TLSv1
TLSv1.1
TLSv1.2
SSLv3
Non-functional
Scenario JSON schema.
Any user is welcome to write its own scenarios for Mongoose. The scenario syntax is described in the corresponding documentation section. The scenario JSON schema allows to validate any custom Mongoose scenario syntax making the writing the scenarios much easier.
Better Backward Compatibility (where possible) with old versions.
The key improvements are:
Aliasing the old configuration parameters to the new ones.
Deprecation Warning messages in case of deprecated configuration parameters detection.
Default Scenario is used when nothing else is specified. It's useful to fall back to use the default scenario if none is specified. The interactive mode should be invoked using special CLI argument as a new functionality.

java -jar mongoose.jar	Default scenario (scenario/default.json) is used to run
java -jar mongoose.jar -I	Semi-interactive mode: await a scenario text from the standard input
java -jar mongoose.jar -f <PATH>	The scenario specified by <PATH> is used to run
java -jar mongoose.jar client -f <PATH>	The scenario specified by <PATH> is used to run in the distributed mode

# 2.0.0

General Notes
Mongoose 2.x is no more compatible with previous versions. That was done to perform a big switch to a new Configuration Layout and Scripting Engine.

The new version of Mongoose is expected to be

Scriptable by a User
Supporting new use cases such as "Mixed Load" and "Weighted Load"
Including the reach set of predefined example scenarios for a user reference.
Also, web GUI is temporary unavailable for the new version as far as it requires an additional work.

New Features and Enhancements
Core functionality
Scripting Engine
The feature introduces the ability to execute the JSON scenario provided on an input (a file or the standard input). This allows to perform the scenarios of unlimited complexity compared to 3 hardcoded scenarios in the previous versions (single, chain, rampup). The scenario syntax is made to be simple so any user is free to write its own scenarios.
Mixed Load Cases Support
Mongoose is able to perform a load to several target buckets, using several users or anything else in the new version.
Weighted Load Case Support
Mongoose is able to perform a load described by a set of the load type ratios, for example: 20% of the requests are Write requests and 80% are Read ones.
Copy Mode
Sometimes it's very useful to perform a copy operation on the multiple files instead of write one. The performance rates may be significantly different for the copy and write operations. Some cloud storage APIs also support copying the objects (S3 and Swift) so the functionality may be a general. In case of S3 and Swift there's no payload sent while copying the objects so these requests may be significantly faster than writing new objects.
The feature allows to copy the files/directories/objects to a different destination. Implemented as an extension of the "Write" load type.
Load Limit By Total Size
It was possible to limit a load job by an item count, a time and a rate in the previous versions. There are the new requirement to make it possible to limit by total processed size. For example, a load job should stop after writing 1TB of a data to the storage.
Non-functional
Example Scenarios
There are almost a hundred of the predefined scenario files written for a user reference, covering most of use cases. They are available in the new version distribution.
Filesystem Load Engine Performance Improvements
Using FileChannels instead of ByteChannels to perform filesystem I/O is expected to increase the performance because of ByteBuffer skipping.
New Configuration Layout
The configuratio layout has been changed significantly to make it more meaningfull and compatible with current and future features.
New Load Type Notation
WRD (Write/Read/Delete) notation is used in the new version instead of CRUDA (Create/Read/Update/Delete/Append) used previously. This was done to make it compatible with future Partial Read feature. Update/Append load types become a partial cases of more general Write load type.
Fixed bugs
Summary	T	Created	P	Status	Resolution
Current load rate limitation approach is too weak	Bug	Mar 28, 2016	Major	CLOSED	Fixed
Load goes through 4 target nodes while configured 12	Bug	Jan 09, 2016	Major	CLOSED	Cannot Reproduce
Mongoose 1.2.0 - perf.sum.csv is sometimes 0 bytes	Bug	Dec 20, 2015	Blocker	CLOSED	Cannot Reproduce
Web UI - log events doesn't load in some cases	Bug	Dec 17, 2015	Minor	CLOSED	Cannot Reproduce

# 1.4.1

The latest version of the discontinued 1.x.x branch. Comparing to the
v1.2.2 has broken web GUI but includes also the following new features:

* Custom HTTP headers
* Writing files to the variable destination files
* Custom Items Naming

# 1.4.0

General Notes
New Features and Enhancements
Core functionality
Custom HTTP headers format extensions
It's required that the variable values in custom HTTP headers have a special format for several reasons.
For details see:
Mongoose Dynamic Configuration Values design notes.
How to generate custom HTTP headers with dynamic values
Writing files to the variable path feature
It's required to write the files to the set of the nested subdirectories described with a "width" and "depth" parameters.
For details see: How to write files on the variable path
Non-functional
Availability of Mongoose as a Docker image in the registry
For details see: How to use Mongoose with Docker
Fixed bugs
Summary	T	Created	P	Status	Resolution
Mongoose 1.3.2 Small Object Write to local NFS mounted directory performance

# 1.3.2

Fixed bugs:
Atmos, S3 - doesn't work - canonical request string contains raw pattern instead of variable data

# 1.3.0

General Notes
It's expected to be the last major release before the 2.0 version development.

New Features and Enhancements
Core functionality
Dynamic values for the custom HTTP headers.
It's required to sent the HTTP request with custom HTTP headers having the variable values defined by patterns and ranges.
For details see:
Mongoose Dynamic Configuration Values design notes.
How to generate custom HTTP headers with dynamic values
Item naming scheme enhanced implementation.
The following new naming options become available:
name prefix
custom fixed name length
id encoding radix
id start offset
For details see:
How to write the items with decimal names starting from 1000000 to 9999999
How to write the items with names having a prefix and a binary random number

# 1.2.2

Fixed bugs:
frequent debug logging to the messages.log file

# 1.2.0

General Notes
Circular load feature has been completely rewritten in order to support Update/Append load types.
Kirill Gusakov has left the team so the development process is expected to be slower.
New Features and Enhancements
Core functionality
Filesystem load engine.
It's required to perform the I/O operations over the files or directories. Distributed mode is supported also in order to perform the tests for the distributed filesystems like NFS.
For details see:
Mongoose Filesystem Load Engine
How to write the files
How to create the directories
Circular Update/Append load jobs support.
For details see: How to do infinite load using finite items source
Latency/Duration real-time charts added in the Web UI.
Custom HTTP headers for the requests generated by Mongoose.
For details see: How to add custom HTTP headers to the generated requests
Basic implementation of the custom item naming scheme feature
For example see: How to write the items with names in the sequential ascending order
Non-functional
The option to turn off console output coloring.
For details see: How to disable the console output coloring
Distributed mode performance improvements.
Storage node balancing improvements.
Fixed bugs
Summary	Created	P	Status	Resolution
Summary not displayed on ^C hitting	Dec 16, 2015	Major	CLOSED	Fixed
Mongoose indicates that it's exiting but the process hung	Dec 13, 2015	Major	CLOSED	Fixed
Storage node balancing load is not uniform when the load is low	Dec 07, 2015	Major	CLOSED	Fixed
wrong labels for axis on charts	Dec 04, 2015	Major	CLOSED	Fixed
Mongoose 1.1.3 Writing to bucket with prefix does not work in distributed mode	Nov 30, 2015	Major	CLOSED	Fixed
circularity feature doesn't support append and update	Sep 17, 2015	Blocker	CLOSED	Fixed
distributed mode bottleneck	Jun 11, 2015	Blocker	CLOSED	Cannot Reproduce
duplicate library files in ./webapp/WEB-INF/lib/ directory of distribution package

# 1.1.3

Fixed bugs:
v1.1.x write performance degradation

# 1.1.2

Fixed bugs:
Mongoose 1.1.1 - Unable to read in distributed mode	Nov 17, 2015
load client hangs	Nov 15, 2015
Atmos API: read fails via namespace interface	Nov 13, 2015
Mongoose doesn't exit/show error message if invalid items list path is specified	Nov 13, 2015
Mongoose 1.1 - Read Hung

# 1.1.1

Fixed bugs:
Mongoose 1.1 - Read Hung CLOSED
Mongoose doesn't exit/show error message if invalid items list path is specified CLOSED

# 1.1.0

General Notes
There are two major new things:
Custom content generation.
Abstract load engine with reference container-specific implementation in addition to already existing object-specific one. This is significant enhancement as far as this will allow to implement in the future version easily another specific load engine implementations, for example operating with local files, users, namespaces, authentication tokens, etc.
Please note the output files format/naming changes described below.
New Features and Enhancements
Core Functionality
Custom content generation
Mongoose becomes able to create the data items with different kind of payload: random data (the only way to generate the data which is supported in previous versions), custom text or custom binary data. The custom data modification using update or append load jobs is supported. The custom data corruption checking using read load job is supported too.
See:
How to write new data items filled with zero bytes
How to write new data items filled with text from Rikki-Tikki-Tavi tale by R. Kipling
How to write new data items filled with custom data from an external file
Container load engine
Mongoose load engine is reworked in the way to be more abstract. New load engine supports not only data items (e.g. objects) but another entiites also. There are a load engine implementation which is able to process the containers (S3 buckets or Swift containers). Create, read and delete operations are supported.
See:
How to create a lot of buckets concurrently
How to read a lot of buckets concurrently
How to delete a lot of buckets concurrently
How to perform a load over the Swift containers instead of buckets
Storage directories support
There are a new capability to specify the path of the directory on the storage which will be used as a target for a load: create in the directory, read from the directory, etc. Bucket/container listing also supports the directory setting. Note that the directory setting has the effect only if file access mode is enabled. Storage API support: Atmos, S3, Swift.
See:
How to create the objects in the specific subdirectory on the storage side
Time to 1st byte of the response payload measurement.
Additional latency metric is reported into the perf.trace.csv log file. The 1st latency is the time between the request has been fully sent and the response is received. This is so called "response latency" or just "latency". The 2nd latency is the time between the request has been fully sent and the 1st byte of the content is received. This is so called "data latency". The data latency is meaningful only in case of a read load.
See:
Metrics reported by Mongoose
Reporting
The format of the perf.trace.csv log file is changed in order to support the data latency measurement. Please refer to Results analysis section for the details.
The file name of the processed items list is changed from data.items.csv to items.csv because of abstract load engine implementation.
Non-Functional
The introduction of the new Sequencing Connection Pool improved the maximum throughput from 60K op/s to 70K op/s.
Fixed Bugs
Mongoose to supress the following debug message: DEBUG | BasicDataItemProducer | 7-S3-Read-60x4x2 | Failed to transfer the data items

# 1.0.2

Fixed bugs:
Mongoose 1.0 - Multi-client read imbalance CLOSED

# 1.0.1

Fixed bugs:
Mongoose 1.0 - is not working CLOSED
load client exits w/o summary if count limit is very little CLOSED
storage node balancing bottleneck in comparison w/ v0.8.2

# 1.0.0

New Features and Enhancements
unlimited reading using data items list of limited size
distributed mode - servers to nodes mapping and assignment
Mongoose must unambiguously report configuration of run
Adding duration. Time takes to perform an operation
6-number summary for duration/latency metrics
distributed mode is not aware of any other producer types other than file
C10K
No python

# 0.9.0

New Features
Mongoose embedding. It must be possible to use Mongoose as a library for load generation.
Run pause/resume function. There must be a way to pause the run for several minutes and resume it.
Resumed run must provide correct reporting with regards to performance metrics and counting

Resumed run must pick up limitations of the paused run

Resumed run must be able to adopt new configuration

The feature should work from CLI and Web UI

Tthe feature should work under Linux/MacOS/Windows

# 0.8.0

New Features and Enhancements
User got control over Mongoose speed. User can specify "think time" between 2 successive operations.
See HowTo wiki for more information
CSV format support
perf.avg and per.sum report files are in CSV format now
All the CSV-files produced contain no markup symbols
All the CSV-files produced use a single delimiter
All the CSV-files produced start with a header line

# 0.7.0

New Features and Enhancements
API and Operations
ECS implementation of OpenStack Swift API support
ECS implementation of Atmos subtenants listing and automatic creation
Data items of zero size support
User Interface and Reporting
New Mongoose Logo
Ability to save charts
Separate log directory for each run
Multiple minor enhancements
All default configuration migrated to conf/properties.json file
Non-functional Enhancements
Way more stable release
Higher performance

# 0.6.4

Bug: Distributed read hangs
The release contains several bug fixes regarding distributed data reading plus some memory consumption tuning.

# 0.6.3

The release contains the fix for the low bandwidth issue occurring on big objects operations.

# 0.6

New features:
* Atmos API support for CRUD operations.
* Realtime charts for any scenario (single, chain, rampup) in web UI.
* Performance improvements (able to sustain the create rate of more than 30K obj/s).
* All values in log files are now in decimal format.

# 0.5

We introduced following new features:

Now Mongoose has possibility to start test from Web UI (was tested in Firefox and Google Chrome, doesn’t support IE)
Mongoose has set of parameters, which could be configured by a shortcuts:
 -b,--bucket <arg>  Bucket to write data to
 -c,--count <arg>  Count of objects to write
 -d,--delete  Perform object delete
 -h,--help  Displays this message
 -i,--ip <arg>  Comma-separated list of ip addresses to write to
 -l,--length <arg>  Size of the object to write
 -o,--use-deployment-output  Use deployment output
 -r,--read <arg>  Perform object read
 -s,--secret <arg>  Secret
 -t,--threads <arg>  Number of parallel threads
 -u,--user <arg>  User
 -w,--write  Perform object write
 -z,--run-id <arg>  Sets run id
Mongoose supports standalone vipr environments, so if needed datanodes may be listed following way:
 –i=ip_address:port1, ip_address:port3, ip_address:port3
 Added error codes returned by storage in error.log
And couple of options which may impact ViPR performance:

Now bucket may be created with filesystem access
Now bucket may be created with versioning support
I’d like also add some short instruction how UI could be used:

Copy mongoose tar file to the client host
Do # tar –xvf mongoose-0.5.0.tar
Do # java -jar mongoose-0.5/mongoose.jar webui
Now webui is accessible by http://<host_ip>:8080/

# 0.4

New metric: Latency (actually response latency): this value is measured in nanoseconds and shows how much time passed since mongoose sent first byte to storage till first byte  from storage was returned. This value depends on object size in case of Create or Update, and doesn't in case of Read and Delete

Changed format:

perf.trace.csv:
removed some unnecessary data
added response latency (named as latency) - time required to get first byte from storage after request was sent from Mongoose
perf.avg.csv:
added response latency (named as latency) - time required to get first byte from storage after request was sent from Mongoose
duration metrics values were sorted. Average value is now on 1st position
Summary message format changed.

Updated scenarios:

chain:
has two modes now: simultaneous=(true/false). default is false. If true: every object consequentially processed via all operations defined for chain load (i.e. create, read, update and delete). So different operations may happen concurrently in a parallel threads. If false: all objects will be processed by a defined operation and result will be saved to a buffer. As soon as all required objects will be processed (or time limit will be reached) next operation will start.
rampup (requires to update chain properties as well) - Will follow number of threads and object sizes defined in .\conf\properties\scenario\rampup
!!!Please mind that this scenario uses chain with defined properties. If you want different set of operation from CRUD - either define it via -Dscenario.chain.load=create-Dscenario.chain.load property!!!

Major bugs fixed:
Message: "FATAL Ignoring log event after log4j was shut down" after test completion

# 0.3.0

Main features completed:
* node balancing broken due to refactoring for v0.2
* unlimited data appends and updates
* client doesn't handle a remote load executor in the specific conditions
* distributed shutdown hangup impacting performance measurements
* more verbose output on data corruption
* numeric result codes for storage response instead of constant names
* report full server message in case of ANY non-2xx response
* tail checking code doesn't work currently due to layer switching

# 0.2

Summary
* create s3 bucket
* file size abbreviations should be case insensitive
* answer 200 on every http create/delete/update/append-request

# 0.1.6

Writing and reading became both faster than previously.
64Mb write and read tests show higher rates than the ones available for Grinder.

The tool outputs its own efficiency for every finished load executor.
The efficiency is calculated as sum of all response times divided by test time and divided by total connections/threads count and logged as percent value.

The configuration parameters "load.create.size.{min|max}" are renamed to "data.size.{min|max}".

# 0.1.5

Closed issue: multi-layer byte range update

# 0.1.4

The main goal of the version is ranges update functionality and read data verification. There are also other changes so please have a look into these release notes if you are going to use the tool.

1.

As far as ViPR doesn’t support currently multiple byte range update via S3 REST API there are only a kind of workaround to simulate multiple byte ranges update.

The user should use scenario “chain” and multiple subsequent updates in order to get the objects updated many times.  The example of CLI to perform the multiple objects updates follows below:

$ java –Drun.time=10.hours –Drun.scenario.name=chain –Dscenario.chain.load=create,update,read,update,read,…,update,read,delete <OTHER_USER_SPECIFIC_ARGUMENTS> –jar mongoose-0.1.4/mongoose.jar

2.

The data integrity verification is checked during read using the range update mask but not the checksum as before. Every object mask is persisted in the output file “data.items.csv” so the format of the file is changed. The example of the output file contents follows below:

…

3bd9b7aa89de570b,100000,080011420032

73fdb2dc6a5bf5cf,100000,21000242408008

257dcec2e417b6eb,100000,06180040800101

507f8f874d96c66c,100000,17a000002040

…

Note the hexadecimal mask values which are highlighted.

3.

The source data ring parameters are not persisted into the header of output file anymore. The user should make self sure that the same data ring is used for the objects data verification as one used for these objects creation/modification. The configuration parameters (and CLI arguments) which determine the source data ring are: data.ring.seed and data.ring.size. If another data ring is used for objects reading with enabled data content verification (by default it is true) the tool will display the following error messages:

Content verification failed for "765e5841deaca7dc"

4.

The number of ranges which may be updated w/o overlapping is calculated from the object size. If user tries to perform more updates the error occurs and the object doesn’t update.

# 0.1.3

1st registered version, 1st issue: data.items.csv is empty on both driver and controller sides
