# Mongoose

[![master](https://img.shields.io/travis/emc-mongoose/mongoose/master.svg)](https://travis-ci.org/emcmongoose/mongoose)
[![downloads](https://img.shields.io/github/downloads/emc-mongoose/mongoose/total.svg)](https://github.com/emc-mongoose/mongoose/releases)
[![release](https://img.shields.io/github/release/emc-mongoose/mongoose.svg)]()
[![Docker Pulls](https://img.shields.io/docker/pulls/emcmongoose/mongoose.svg)](https://hub.docker.com/r/emcmongoose/mongoose/)

## Description

Mongoose is a storage performance testing tool.

It is designed to be used for:
* [Load Testing](https://en.wikipedia.org/wiki/Load_testing)
* [Stress Testing](https://en.wikipedia.org/wiki/Stress_testing)
* [Soak/Longevity/Endurance Testing](https://en.wikipedia.org/wiki/Soak_testing)
* [Volume Testing](https://en.wikipedia.org/wiki/Volume_testing)
* [Smoke](https://en.wikipedia.org/wiki/Smoke_testing_(software))/[Sanity](https://en.wikipedia.org/wiki/Sanity_check) Testing]

Mongoose is able to sustain millions of concurrent connections and
millions of operations per second.

Basically, Mongoose may be started very simply:
```bash
java -jar mongoose.jar
```

## Features
1. Distributed Mode
2. Reporting:
    1. Item lists for reusing
    2. Statistics for the rates and timings
    3. High-resolution metrics for each operation
3. Load Types:
    1. Create (with Copy Mode as an extension)
    2. Read
    3. Update (with Append as an extension)
    4. Delete
    5. No-op
4. Item Types:
    1. Paths (Bucket/Container/etc)
    2. Data Items (Object/File/etc)
    3. Tokens (Subtenant/Auth Tokens/etc)
5. Cloud Storages Support:
    1. Amazon S3
    2. EMC Atmos
    3. OpenStack Swift
4. Filesystem Storage Support
5. Content Modification/Verification Ability
6. Custom Content
7. Circular Load Mode
8. Scenario Scripting
9. Throttling
10. Web GUI
11. Configuration Parametrization
12. Custom Items Naming
13. SSL/TLS Support
14. Docker Integration

## Further Reading
See <https://github.com/emc-mongoose/mongoose/wiki> for details
