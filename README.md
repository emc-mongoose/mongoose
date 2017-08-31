# Mongoose

[![master](https://img.shields.io/travis/emc-mongoose/mongoose/master.svg)](https://travis-ci.org/emcmongoose/mongoose)
[![downloads](https://img.shields.io/github/downloads/emc-mongoose/mongoose/total.svg)](https://github.com/emc-mongoose/mongoose/releases)
[![release](https://img.shields.io/github/release/emc-mongoose/mongoose.svg)]()
[![Docker Pulls](https://img.shields.io/docker/pulls/emcmongoose/mongoose.svg)](https://hub.docker.com/r/emcmongoose/mongoose/)

# Mongoose

## Description

Mongoose is a storage performance testing tool.

It is designed to be used for:
* [Load Testing](https://en.wikipedia.org/wiki/Load_testing)
* [Stress Testing](https://en.wikipedia.org/wiki/Stress_testing)
* [Soak/Longevity/Endurance Testing](https://en.wikipedia.org/wiki/Soak_testing)
* [Volume Testing](https://en.wikipedia.org/wiki/Volume_testing)
* [Smoke](https://en.wikipedia.org/wiki/Smoke_testing_(software))/[Sanity](https://en.wikipedia.org/wiki/Sanity_check) Testing

Mongoose is able to sustain millions of concurrent connections and
millions of operations per second.

Basically, Mongoose may be started very simply:
```bash
java -jar mongoose.jar
```

## Features

### Core Functionality

1. **[[Configuration|v3.5 User Guide#1-configuration]]**

    Mongoose has rich configuration subsystem supporting the
    [[parametrization|v3.5 User Guide#v3.4-User-Guide#13-parameterized-configuration]].

2. **[[Items|v3.5 User Guide#2-items]]**

    Item is the unit of the load progress and metrics accounting. An item may be a file,
    cloud storage data object, some kind of token or something else. The
    [[*data* items|v3.5 User Guide#211-data-items]] describe the data to use to perform a load
    operation (some fixed or random size, content source, etc).

    Mongoose may persist the items used in a run/job info to a specified
    [[output|v3.5 User Guide#23-items-output]] which later may be used as
    [[items input|v3.5 User Guide#22-items-input]] for another run/job.

3. **[[Content|v3.5 User Guide#3-content]]**

    Different data may be used to perform a load on a storage. By default Mongoose uses random,
    uncompressible data. An user may supply
    [[custom data input|v3.5 User Guide#32-payload-from-the-external file]] instead.

4. **[[Concurrency|v3.5 User Guide#4-concurrency]]**

    In contradiction to all other comparable performance testing software, Mongoose doesn't use the
    threads to set up a specified concurrency level. This allows to use unlimited concurrency levels easily.

5. **[[Recycle Mode|v3.5 User Guide#5-circularity]]**

    The so called *recycling* feature allows to use the limited amount of *items* for an unlimited
    test run/load job by reusing these items again and again (circularly).

6. **[[Test Steps|v3.5 User Guide#6-test-steps]]**

    A *test step* is an unit of a test run with its own configuration and metrics reporting.

    An user is able to configure and [[identify a test step|v3.5-User-Guide#61-test-steps-naming]] and
    [[limit it using one of available constraints|v3.4-User-Guide#62-test-steps-limitation.

7. **[[Metrics Reporting|v3.5 User Guide#7-metrics-reporting]]**

    The metrics reported by Mongoose are designed to be most useful for performance analysis.
    The following metrics are accounted:
    * Counts: items, bytes, elapsed and effective times.
    * Rates: items per second and bytes per second.
    * Timing distributions for operation durations and network latencies.

    There are the metrics outputs performed periodically while a load job is running and
    total ones when a load job is finished.

    Also, it's possible to
    [[obtain the highest-precision metrics (for each operation)|v3.5-User-Guide#74-io-traces-reporting]].

8. **[[Load Types|v3.5 User Guide#8-load-types]]**

    The CRUD notation is used to describe the load operations. However, there are some extensions:
    * Create may act as *[[copying|v3.4-User-Guide#822-copy-mode]]* the items from the source to the destination
    * Read may [[validate the data on the fly|v3.4-User-Guide#832-read-with-enabled-verification]]
    * Read may be [[partial|v3.4-User-Guide#833-partial-read]]
    * Update may act as *[[append|v3.4-User-Guide#8434-append]]*
    * [[Noop|v3.4-User-Guide#81-noop]] operation is also available.

    One of the most interesting things is a *data reentrancy*. This allows to validate the data
    read back from the storage successfully even after the data items have been randomly updated
    multiple times before.

9. **[[Scenarios|v3.4 User Guide#9-scenarios]]**

    Mongoose is able to run the tests described with scenario files in the JSON format. Scenario
    syntax allow to:
    * [[Inherit the jobs configuration|v3.4-User-Guide#942-step-configuration-inheritance]]
    * [[Substitute the values using the environment variables|v3.4-User-Guide#944-environment-values-substitution-in-the-scenario]].
    * [[Execute the external commands|v3.4-User-Guide#951-shell-command-step]]
    * Execute the steps [[sequentially|v3.4-User-Guide#955-sequential-step]] or in [[parallel|v3.4-User-Guide#954-parallel-step]]
    * [[Execute the steps in a loop|v3.4-User-Guide#956-loop-step]]
    * Execute the so called *[[weighted load|v3.4-User-Guide#9572-weighted-load-step]]*
    * Execute the so called *[[chain load|v3.4-User-Guide#958-chain-load-step]]*

10. **[[Storage Driver|v3.4 User Guide#10-storage-driver]]**

    Mongoose is able to work in the distributed mode what allows to scale out the load
    performed on a storage. In the distributed mode there's a controller which aggregates the
    results from the several remote storage drivers. Storage drivers perform the actual
    load on the storage.

    Mongoose supports different storage types: a
    [[filesystem|v3.4-User-Guide#103-filesystem-storage-driver]] or a cloud storage.
    Currently, the following cloud storage APIs are supported:
    * [[Amazon S3|v3.4-User-Guide#10452-atmos]]
    * [[EMC Atmos|v3.4-User-Guide#10453-s3]]
    * [[OpenStack Swift|v3.4-User-Guide#10454-swift]].

    It's possible to implement a [[custom storage driver|v3.4 Custom Storage Driver]] to extend the storages support.

### Other

1. [[Deployment using Docker|v3.5-Quickstart#docker]]
2. [[Storage Mock|v3.5-Quickstart#hello-world]]
3. [[Web GUI|v3.5-Quickstart#web-gui]]

## Documentation

**[[Mongoose v3.5|v3.5 Overview]]**

[[Mongoose v3.4|v3.4/Overview]]

[[Mongoose v3.3|v3.3 Overview]]

[[Mongoose v3.1|v3.1/Overview]]

[[Mongoose v3.0|v3.0/Overview]]

[Mongoose v2.x.x](http://emc-mongoose.github.io/mongoose)

[[Mongoose v1.x.x|v1.x/Overview]]

