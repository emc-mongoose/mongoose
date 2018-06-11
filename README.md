[![master](https://img.shields.io/travis/emc-mongoose/mongoose-base/master.svg)](https://travis-ci.org/emcmongoose/mongoose-base)
[![downloads](https://img.shields.io/github/downloads/emc-mongoose/mongoose-base/total.svg)](https://github.com/emc-mongoose/mongoose-base/releases)
[![release](https://img.shields.io/github/release/emc-mongoose/mongoose-base.svg)]()
[![Docker Pulls](https://img.shields.io/docker/pulls/emcmongoose/mongoose.svg)](https://hub.docker.com/r/emcmongoose/mongoose/)

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

# [Features](doc/features.md)

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
    * [Concurrency](doc/design/concurrency.md)
    * [Distributed Mode](doc/design/distributed_mode.md)
    * [Installer](doc/design/installer.md)
    * [Recycle Mode](doc/design/recycle_mode.md)
    * [Data Reentrancy](doc/design/data_reentrancy.md)
    * [Byte Range Operations](doc/design/byte_range_operations.md)
    * [Copy Mode](doc/design/copy_mode.md)

# Contributors

* [Andrey Kurilov](https://github.com/akurilov)
* Gennady Eremeev
* [Ilya Kisliakovsky](https://github.com/kisliakovsky)
* [Kirill Gusakov](https://github.com/gusakk)
* Mikhail Danilov
* [Mikhail Malygin](https://github.com/aphreet)
* [Olga Zhavzharova](https://github.com/Zhavzharova)
* [Veronika Kochugova](https://github.com/veronikaKochugova)
