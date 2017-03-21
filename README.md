# Mongoose

## Description
Mongoose is a high-load storage performance testing automation tool.

The Mongoose Load Engine is capable to work with:

* A million of concurrent connections
* A million of operations per second
* A million of items which may be processed multiple times in the circular load mode
* A million of items which may be stored in the storage mock

Basically, Mongoose may be started very simply:
```bash
java -jar mongoose.jar
```

## Features
1. Distributed Mode
2. Reporting:
    1. Item lists for reusing
    2. Statistics for the rates and timings
    3. High-resolution timings for each operation
3. Load Types:
    1. Create (with Copy Mode as an extension)
    2. Read
    3. Update (with Append as an extension)
    4. Delete
    5. No-op
4. Item Types:
    1. Containers (Bucket/Directory/etc)
    2. Data Items (Object/File/etc)
    3. Tokens (Subtenant/etc - to be implemented soon)
5. Cloud Storages Support:
    1. Amazon S3
    2. EMC Atmos
    3. OpenStack Swift
4. Filesystem Load Support
5. Content Modification/Verification Ability
6. Custom Content
7. Circular Load Mode
8. Scenario Scripting
9. Throttling
10. Web GUI
11. Dynamic Configuration Values
12. Custom Items Naming
13. SSL/TLS Support
14. Docker Integration

## Further Reading
See <https://github.com/emc-mongoose/mongoose/wiki> for details
