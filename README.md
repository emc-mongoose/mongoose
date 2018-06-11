[![master](https://img.shields.io/travis/emc-mongoose/mongoose-base/master.svg)](https://travis-ci.org/emcmongoose/mongoose-base)
[![downloads](https://img.shields.io/github/downloads/emc-mongoose/mongoose-base/total.svg)](https://github.com/emc-mongoose/mongoose-base/releases)
[![release](https://img.shields.io/github/release/emc-mongoose/mongoose-base.svg)]()
[![Docker Pulls](https://img.shields.io/docker/pulls/emcmongoose/mongoose.svg)](https://hub.docker.com/r/emcmongoose/mongoose/)

# Contents

* Overview
* Features
* Documentation
* Contributors

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

1. [Scale Out](doc/features.md#1-scale-out)<br/>
 1.1. [Distributed Mode](doc/features.md#11-distributed-mode)<br/>
 1.2. [Fibers](doc/features.md#12-fibers)<br/>
2. [Customization](doc/features.md#2-customization)<br/>
 2.1. [Flexible Configuration](doc/features.md#21-flexible-configuration)<br/>
 2.2. [Load Generation Patterns](doc/features.md#22-load-generation-patterns)<br/>
 2.3. [Scenarios](doc/features.md#23-scenarios)<br/>
 2.4. [Metrics Reporting](doc/features.md#24-metrics-reporting)<br/>
3. [Extension](doc/features.md#3-extension)<br/>
 3.1. [Load Steps](doc/features.md#31-load-steps)<br/>
 3.2. [Storage Drivers](doc/features.md#32-storage-drivers)<br/>
 3.3. [Scenario Engines](doc/features.md#33-scenario-engine)<br/>

# Documentation

* [Deployment](doc/deployment.md)
* [User Guide](doc/user_guide.md)
* [Troubleshooting](doc/troubleshooting.md)
* API
    * Extensions
        * [Load Steps](doc/api/extensions/load_steps.md)
        * [Storage Drivers](doc/api/extensions/storage_drivers.md)
    * [Dependencies](doc/api/dependencies.md)
* Input
    * [CLI](doc/input/cli.md)
    * [Configuration](doc/input/configuration.md)
    * [Scenarios](doc/input/scenarios.md)
* Output
    * [General](doc/output/general.md)
    * [Metrics](doc/output/metrics.md)
* Design
    * [Architecture](doc/design/architecture.md)
    * [Distributed Mode](doc/design/distributed_mode.md)
    * [Installer](doc/design/installer.md)
    * [Recycle Mode](doc/design/recycle_mode.md)
    * [Data Reentrancy](doc/design/data_reentrancy.md)
    * [Byte Range Operations](doc/design/byte_range_operations.md)
    * [Copy Mode](doc/design/copy_mode.md)
    * [Pipeline Load](doc/design/pipeline_load.md)
    * [Weighted Load](doc/design/weighted_load.md)
* Development
    * Notes
    * Changelog

# Contributors

* [Andrey Kurilov](https://github.com/akurilov)
* Gennady Eremeev
* [Ilya Kisliakovsky](https://github.com/kisliakovsky)
* [Kirill Gusakov](https://github.com/gusakk)
* Mikhail Danilov
* [Mikhail Malygin](https://github.com/aphreet)
* [Olga Zhavzharova](https://github.com/Zhavzharova)
* [Veronika Kochugova](https://github.com/veronikaKochugova)

