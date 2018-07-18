[![master](https://img.shields.io/travis/emc-mongoose/mongoose/master.svg)](https://travis-ci.org/emcmongoose/mongoose)
[![downloads](https://img.shields.io/github/downloads/emc-mongoose/mongoose/total.svg)](https://github.com/emc-mongoose/mongoose/releases)
[![release](https://img.shields.io/github/release/emc-mongoose/mongoose.svg)]()
[![Docker Pulls](https://img.shields.io/docker/pulls/emcmongoose/mongoose.svg)](https://hub.docker.com/r/emcmongoose/mongoose/)

# Contents

* [Overview](#overview)
* [Features](#features)
* [Documentation](#documentation)
* [Contributors](#contributors)
* [Links](#links)

# Overview

Mongoose is a powerful storage performance testing tool.

It is designed to be used for:
* [Load Testing](https://en.wikipedia.org/wiki/Load_testing)
* [Stress Testing](https://en.wikipedia.org/wiki/Stress_testing)
* [Soak/Longevity/Endurance Testing](https://en.wikipedia.org/wiki/Soak_testing)
* [Volume Testing](https://en.wikipedia.org/wiki/Volume_testing)
* [Smoke](https://en.wikipedia.org/wiki/Smoke_testing_(software))/[Sanity](https://en.wikipedia.org/wiki/Sanity_check)
  Testing

It is easily scalable to perform millions of IOPS and emulate millions of concurrent clients.

See also the **[comparison with similiar tools](doc/comparison.md)**.

# Features

1. [Scalability](doc/features.md#1-scalability)<br/>
 1.1. [Vertical](doc/feature.md#11-vertical)<br/>
 1.2. [Horizontal](doc/feature.md#12-horizontal)<br/>
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
        * [General](doc/api/extensions/general.md)
        * [Load Steps](doc/api/extensions/load_steps.md)
        * Storage Drivers
            * [General](doc/api/extensions/storage_drivers/general.md)
            * [S3](doc/api/extensions/storage_drivers/s3.md)
            * [Atmos](doc/api/extensions/storage_drivers/atmos.md)
            * [Swift](doc/api/extensions/storage_drivers/swift.md)
            * [FS](doc/api/extensions/storage_drivers/fs.md)
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
    * [Process](doc/development/process.md)
    * [Notes](doc/development/notes.md)
    * [Changelog](doc/development/changelog.md)

# Contributors

* [Andrey Kurilov](https://github.com/akurilov)
* Gennady Eremeev
* [Ilya Kisliakovsky](https://github.com/kisliakovsky)
* [Kirill Gusakov](https://github.com/gusakk)
* Mikhail Danilov
* [Mikhail Malygin](https://github.com/aphreet)
* [Olga Zhavzharova](https://github.com/Zhavzharova)
* [Veronika Kochugova](https://github.com/veronikaKochugova)

# Links
* [Travis CI](https://travis-ci.org/emc-mongoose/mongoose)
* [Docker Hub](https://hub.docker.com/r/emcmongoose/mongoose)
