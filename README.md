[![master](https://img.shields.io/travis/emc-mongoose/mongoose-base/master.svg)](https://travis-ci.org/emcmongoose/mongoose-base)
[![downloads](https://img.shields.io/github/downloads/emc-mongoose/mongoose-base/total.svg)](https://github.com/emc-mongoose/mongoose-base/releases)
[![release](https://img.shields.io/github/release/emc-mongoose/mongoose-base.svg)]()
[![Docker Pulls](https://img.shields.io/docker/pulls/emcmongoose/mongoose.svg)](https://hub.docker.com/r/emcmongoose/mongoose/)

# Contents

* Overview
* Features
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

Mongoose is a storage performance testing tool.

It is designed to be used for:
* [Load Testing](https://en.wikipedia.org/wiki/Load_testing)
* [Stress Testing](https://en.wikipedia.org/wiki/Stress_testing)
* [Soak/Longevity/Endurance Testing](https://en.wikipedia.org/wiki/Soak_testing)
* [Volume Testing](https://en.wikipedia.org/wiki/Volume_testing)
* [Smoke](https://en.wikipedia.org/wiki/Smoke_testing_(software))/[Sanity](https://en.wikipedia.org/wiki/Sanity_check) Testing

Mongoose is able to sustain millions of concurrent connections and
millions of operations per second.

## Getting Started

Please refer to the [deployment](https://github.com/emc-mongoose/mongoose-base/wiki/v3.6-Deployment)
page for the details.

## Documentation

**[Mongoose v3.6](https://github.com/emc-mongoose/mongoose-base/wiki/v3.6-Overview)**

[Mongoose v3.5](https://github.com/emc-mongoose/mongoose-base/wiki/v3.5-Overview)

[Mongoose v3.4](https://github.com/emc-mongoose/mongoose-base/wiki/v3.4-Overview)

[Mongoose v3.3](https://github.com/emc-mongoose/mongoose-base/wiki/v3.3-Overview)

[Mongoose v3.1](https://github.com/emc-mongoose/mongoose-base/wiki/v3.1-Overview)

[Mongoose v3.0](https://github.com/emc-mongoose/mongoose-base/wiki/v3.0-Overview)

[Mongoose v2.x.x](http://emc-mongoose.github.io/mongoose)

[Mongoose v1.x.x](https://github.com/emc-mongoose/mongoose-base/wiki/v1.x-Overview)

## Key Features

1. **[Configuration](https://github.com/emc-mongoose/mongoose-base/wiki/v3.5-Configuration)**

    Mongoose has rich configuration subsystem supporting the
    [parametrization](https://github.com/emc-mongoose/mongoose-base/wiki/v3.5-Configuration#2-parametrization).

2. **[Items](https://github.com/emc-mongoose/mongoose-base/wiki/v3.5-User-Guide#2-items)**

    Item is the unit of the load progress and metrics accounting. An item may be a file,
    cloud storage data object, some kind of token or something else. The
    [*data* items](https://github.com/emc-mongoose/mongoose-base/wiki/v3.5-User-Guide#211-data-items) describe the data to use to perform a load
    operation (some fixed or random size, content source, etc).

    Mongoose may persist the items used in a run/job info to a specified
    [output](https://github.com/emc-mongoose/mongoose-base/wiki/v3.5-User-Guide#23-items-output) which later may be used as
    [items input](https://github.com/emc-mongoose/mongoose-base/wiki/v3.5-User-Guide#22-items-input) for another run/job.

3. **[Content](https://github.com/emc-mongoose/mongoose-base/wiki/v3.5-User-Guide#3-content)**

    Different data may be used to perform a load on a storage. By default Mongoose uses random,
    uncompressible data. An user may supply
    [custom data input](https://github.com/emc-mongoose/mongoose-base/wiki/v3.5-User-Guide#32-payload-from-the-external-file) file instead.

4. **[Concurrency](https://github.com/emc-mongoose/mongoose-base/wiki/v3.5-User-Guide#4-concurrency)**

    In contradiction to all other comparable performance testing software, Mongoose doesn't use the
    threads to set up a specified concurrency level. This allows to use
    [unlimited concurrency](https://github.com/emc-mongoose/mongoose-base/wiki/v3.5-User-Guide#42-unlimited-concurrency) levels easily.

5. **[Recycle Mode](https://github.com/emc-mongoose/mongoose-base/wiki/v3.5-User-Guide#5-recycle-mode)**

    The so called *recycling* feature allows to use the limited amount of *items* for an unlimited
    test run/load job by reusing these items again and again (circularly).

6. **[Test Steps](https://github.com/emc-mongoose/mongoose-base/wiki/v3.5-User-Guide#6-test-steps)**

    A *test step* is an unit of a test run with its own configuration and metrics reporting.

    An user is able to configure and [identify a test step](https://github.com/emc-mongoose/mongoose-base/wiki/v3.5-User-Guide#61-test-steps-identification) and
    [limit it using one of available constraints](https://github.com/emc-mongoose/mongoose-base/wiki/v3.4-User-Guide#62-test-steps-limitation).

7. **[Metrics Reporting](https://github.com/emc-mongoose/mongoose-base/wiki/v3.5-User-Guide#72-metrics-output)**

    The metrics reported by Mongoose are designed to be most useful for performance analysis.
    The following metrics are accounted:
    * Counts: items, bytes, elapsed and effective times.
    * Rates: items per second and bytes per second.
    * Timing distributions for operation durations and network latencies.

    There are the metrics outputs performed periodically while a load job is running and
    total ones when a load job is finished.

    Also, it's possible to
    [obtain the highest-precision metrics (for each operation)](https://github.com/emc-mongoose/mongoose-base/wiki/v3.6-User-Guide#723-trace-metrics-output).

8. **[Load Types](https://github.com/emc-mongoose/mongoose-base/wiki/v3.6-User-Guide#8-load-types)**

    The CRUD notation is used to describe the load operations. However, there are some extensions:
    * Create may act as *[copying](https://github.com/emc-mongoose/mongoose-base/wiki/v3.6-User-Guide#822-copy-mode)* the items from the source to the destination
    * Read may [validate the data on the fly](https://github.com/emc-mongoose/mongoose-base/wiki/v3.5-User-Guide#832-read-with-enabled-validation)
    * Read may be [partial](https://github.com/emc-mongoose/mongoose-base/wiki/v3.5-User-Guide#833-partial-read)
    * Update may act as *[append](https://github.com/emc-mongoose/mongoose-base/wiki/v3.5-User-Guide#8434-append)*
    * [Noop](https://github.com/emc-mongoose/mongoose-base/wiki/v3.5-User-Guide#81-noop) operation is also available.

    One of the most interesting things is a *data reentrancy*. This allows to validate the data
    read back from the storage successfully even after the data items have been randomly updated
    multiple times before.

9. **[Scenarios](https://github.com/emc-mongoose/mongoose-base/wiki/v3.6-User-Guide#9-scenarios)**

    Mongoose is able to run the tests described with scenario files in using the specific DSL.
    Scenario syntax allow to:
    * [Configure the load steps](https://github.com/emc-mongoose/mongoose-base/wiki/v3.6-User-Guide#942-step-configuration-reusing)
    * Organize the steps sequentially or [in parallel](https://github.com/emc-mongoose/mongoose-base/wiki/v3.6-User-Guide#953-parallel-step)
    * [Reuse the step configuration](https://github.com/emc-mongoose/mongoose-base/wiki/v3.6-User-Guide#942-step-configuration-reusing)
    * [Substitute the values using environment variables](https://github.com/emc-mongoose/mongoose-base/wiki/v3.6-User-Guide#944-environment-values-substitution-in-the-scenario)
    * [Execute the external commands](https://github.com/emc-mongoose/mongoose-base/wiki/v3.6-User-Guide#951-shell-command)
    * Execute advanced load steps, such as [weighted load](https://github.com/emc-mongoose/mongoose-base/wiki/v3.6-User-Guide#954-weighted-load-step) either [chain load](https://github.com/emc-mongoose/mongoose-base/wiki/v3.6-User-Guide#955-chain-load-step).

10. **[Storage Driver](https://github.com/emc-mongoose/mongoose-base/wiki/v3.6-User-Guide#10-storage-driver)**

    Mongoose is able to work in the distributed mode what allows to scale out the load
    performed on a storage. In the distributed mode there's a controller which aggregates the
    results from the several remote storage drivers. Storage drivers perform the actual
    load on the storage.

    Mongoose supports different storage types:
    * [Amazon S3](https://github.com/emc-mongoose/mongoose-storage-driver-s3)
    * [EMC Atmos](https://github.com/emc-mongoose/mongoose-storage-driver-atmos)
    * [OpenStack Swift](https://github.com/emc-mongoose/mongoose-storage-driver-swift)
    * [Filesystem](https://github.com/emc-mongoose/mongoose-storage-driver-fs)
    * [HDFS](https://github.com/emc-mongoose/mongoose-storage-driver-hdfs)

    It's possible to implement a [custom storage driver](https://github.com/emc-mongoose/mongoose-base/wiki/v3.5-Custom-Storage-Driver) to extend the storages support.

## Authors

* [Andrey Kurilov](https://github.com/akurilov)

## Acknowledgements

* Gennady Eremeev
* [Ilya Kisliakovsky](https://github.com/kisliakovsky)
* [Kirill Gusakov](https://github.com/gusakk)
* Mikhail Danilov
* [Mikhail Malygin](https://github.com/aphreet)
* [Olga Zhavzharova](https://github.com/Zhavzharova)
* [Veronika Kochugova](https://github.com/veronikaKochugova)
