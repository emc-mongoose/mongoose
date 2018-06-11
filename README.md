[![master](https://img.shields.io/travis/emc-mongoose/mongoose-base/master.svg)](https://travis-ci.org/emcmongoose/mongoose-base)
[![downloads](https://img.shields.io/github/downloads/emc-mongoose/mongoose-base/total.svg)](https://github.com/emc-mongoose/mongoose-base/releases)
[![release](https://img.shields.io/github/release/emc-mongoose/mongoose-base.svg)]()
[![Docker Pulls](https://img.shields.io/docker/pulls/emcmongoose/mongoose.svg)](https://hub.docker.com/r/emcmongoose/mongoose/)

# Contents

* [Overview](#overview)
* Features
    * Scale Out
    * Customization
    * Extension
* Documentation
    * Deployment
    * User Guide
    * Troubleshooting
    * API
        * Extensions
            * Load Steps
            * Storage Drivers
        * Dependencies
    * Input
        * CLI
        * Configuration
        * Scenarios
    * Output
        * General
        * Metrics
    * Design
        * Architecture
        * Concurrency
        * Distributed Mode
        * Installer
        * Configuration
        * Recycle Mode
        * Data Reentrancy
        * Byte Range Operations
        * Copy Mode
* Contributors
* License

# Overview

Mongoose is a scaleable, customizable and extensible storage performance
testing tool.

It is designed to be used for:
* [Load Testing](https://en.wikipedia.org/wiki/Load_testing)
* [Stress Testing](https://en.wikipedia.org/wiki/Stress_testing)
* [Soak/Longevity/Endurance Testing](https://en.wikipedia.org/wiki/Soak_testing)
* [Volume Testing](https://en.wikipedia.org/wiki/Volume_testing)
* [Smoke](https://en.wikipedia.org/wiki/Smoke_testing_(software))/[Sanity](https://en.wikipedia.org/wiki/Sanity_check) Testing

Mongoose is able to sustain millions of concurrent connections and
execute millions of operations per second.

# Features

## 1. Scale Out

### 1.1. Distributed Mode

### 1.2. Fibers

Using fibers allows to sustain millions of concurrent tasks easily
without significant performance degradation.

## 2. Customization

### 2.1. Flexible Configuration

Supports the parameterization

### 2.2. Load Generation Patterns

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

### 2.3. Scenarios

Allow to organize the load steps in the required order and reuse the
complex performance tests.

### 2.4. Metrics Reporting

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

## 3. Extension

Mongoose is designed to be agnostic to the particular extensions
implementations. This allows to support any storage, scenario language,
different load step kinds.

### 3.1. Load Steps

### 3.2. Storage Drivers

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

### 3.3. Scenario Engine

Any Mongoose scenario may be written using any JSR-223 compliant
scripting language. Javascript support is available out-of-the-box.

# Documentation

# Contributors

* [Andrey Kurilov](https://github.com/akurilov)
* Gennady Eremeev
* [Ilya Kisliakovsky](https://github.com/kisliakovsky)
* [Kirill Gusakov](https://github.com/gusakk)
* Mikhail Danilov
* [Mikhail Malygin](https://github.com/aphreet)
* [Olga Zhavzharova](https://github.com/Zhavzharova)
* [Veronika Kochugova](https://github.com/veronikaKochugova)

# License

