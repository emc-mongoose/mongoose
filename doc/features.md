# Contents

1. Scale Out
 1.1. Distributed Mode
 1.2. Fibers
2. Customization
 2.1. Flexible Configuration
 2.2. Load Generation Patterns
 2.3. Scenarios
 2.4. Metrics Reporting
3. Extension
 3.1. Load Steps
 3.2. Storage Drivers
 3.3. Scenario Engines

# 1. Scale Out

## 1.1. Distributed Mode

## 1.2. Fibers

Using fibers allows to sustain millions of concurrent tasks easily
without significant performance degradation.

# 2. Customization

## 2.1. Flexible Configuration

Supports the parameterization

## 2.2. Load Generation Patterns

* CRUD operations and the extensions: Noop, Copy

* Parial Operations

* Composite Operations

* Complex Load Steps
    * Weighted Load
    * Pipeline Load
* Recycle Mode

* Data Reentrancy

  Allows to validate the data read back from the storage successfully
  even after the data items have been randomly updated multiple times
  before

* Custom Payload Data

## 2.3. Scenarios

Allow to organize the load steps in the required order and reuse the
complex performance tests.

## 2.4. Metrics Reporting

The metrics reported by Mongoose are designed to be most useful for the
performance analysis. The following metrics are available:

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

  It's possible to limit the rate and measure the sustained actual
  concurrency.

The metrics output is being performed periodically while a load step
is running and once when a load step is finished (summary). Also, it's
possible to obtain the highest precision metrics (for each
operation).

# 3. Extension

Mongoose is designed to be agnostic to the particular extensions
implementations. This allows to support any storage, scenario language,
different load step kinds.

## 3.1. Load Steps

## 3.2. Storage Drivers

Mongoose accounts and operates with abstract *items* which may be files,
objects, directories, tokens, buckets, etc. The exact behaviour is
defined by the particular storage driver implementation.

Mongoose supports some storage types out-of-the-box:
* Amazon S3
* EMC Atmos
* OpenStack Swift
* Filesystem

The following additional storage driver implementations are available:
* [HDFS](https://github.com/emc-mongoose/mongoose-storage-driver-hdfs)

## 3.3. Scenario Engine

Any Mongoose scenario may be written using any JSR-223 compliant
scripting language. Javascript support is available out-of-the-box.
