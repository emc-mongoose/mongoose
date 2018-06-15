# Contents

1. [Scalability](#1-scalability)<br/>
 1.1. [Vertical](#11-vertical)<br/>
 1.2. [Horizontal](#12-horizontal)<br/>
2. [Customization](#2-customization)<br/>
 2.1. [Flexible Configuration](#21-flexible-configuration)<br/>
 2.2. [Load Generation Patterns](#22-load-generation-patterns)<br/>
 2.3. [Scenarios](#23-scenarios)<br/>
 2.4. [Metrics Reporting](#24-metrics-reporting)<br/>
3. [Extension](#3-extension)<br/>
 3.1. [Load Steps](#31-load-steps)<br/>
 3.2. [Storage Drivers](#32-storage-drivers)<br/>
 3.3. [Scenario Engines](#33-scenario-engine)<br/>

# 1. Scalability

## 1.1. Vertical

Using [fibers](https://github.com/akurilov/fiber4j) allows to sustain millions of concurrent tasks easily without
significant performance degradation.

## 1.2. Horizontal

The [distributed mode](design/distributed_mode.md) in Mongoose was designed to make each Mongoose node working
independently as much as possible. Any *distributed load step* execution may be initiated from any node from the given
set. Then the chosen node becomes a *master node* (temporarily). The nodes involved in the given distributed load step
execution become *slave nodes*. All necessary input is prepared (*sliced*) and distributed among the nodes before the
actual load step start to get rid of the redundant interaction via the network during the load step execution. The slave
nodes are being polled periodically to synchronize the load step state. After the load step is done, the summary data
may be (optionally) aggregated and persisted on the master node.

# 2. Customization

## 2.1. Flexible Configuration

Supports the [parameterization](input/configuration.md#2-parameterization) and [extension](api/extensions/general.md)
but remains type-safe and structure-safe

## 2.2. Load Generation Patterns

* CRUD operations and the extensions: Noop, [Copy](design/copy_mode.md)

* [Parial Operations](design/byte_range_operations.md)

* [Composite Operations](design/storage_side_concatenation.md)

* Complex Load Steps
    * [Pipeline Load](design/pipeline_load.md)
    * [Weighted Load](design/weighted_load.md)
* [Recycle Mode](design/recycle_mode.md)

* [Data Reentrancy](design/data_reentrancy.md)

  Allows to validate the data read back from the storage successfully even after the data items have been randomly
  updated multiple times before

* Custom Payload Data

## 2.3. [Scenarios](input/scenarios.md)

Allow to organize the load steps in the required order and reuse the complex performance tests

## 2.4. [Metrics Reporting](output/metrics.md)

The metrics reported by Mongoose are designed to be most useful for the performance analysis. The following metrics are
available:

* Counts

  * Items
  * Bytes transferred
  * Time
    * Effective
    * Elapsed

* Rates

  * Items per second
  * Bytes per second

* Timing distributions for:

  * Operation durations
  * Network latencies

* Actual concurrency

  It's possible to limit the rate and measure the sustained actual concurrency

The *average* metrics output is being done periodically while a load step is running. The *summary* metrics output is
done once when a load step is finished. Also, it's possible to obtain the highest precision metrics (for each operation,
so called *I/O trace* records).

# 3. [Extension](api/extensions/general.md)

Mongoose is designed to be agnostic to the particular extensions implementations. This allows to support any storage,
scenario language, different load step kinds.

## 3.1. [Load Steps](api/extensions/load_steps.md)

It's possible to implement a custom load step.

## 3.2. Storage Drivers

Mongoose accounts and operates with abstract *items* which may be files, objects, directories, tokens, buckets, etc. The
exact behaviour is defined by the particular storage driver implementation.

Mongoose supports some storage types out-of-the-box:
* Amazon S3
* EMC Atmos
* OpenStack Swift
* Filesystem

The following additional storage driver implementations are publicly available:
* [HDFS](https://github.com/emc-mongoose/mongoose-storage-driver-hdfs)

## 3.3. Scenario Engine

Any Mongoose scenario may be written using any JSR-223 compliant scripting language. Javascript support is available
out-of-the-box.
