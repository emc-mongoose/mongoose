# Storage performance testing tools comparison

## General
|                   | Mongoose  | COSBench | LoadRunner         | Locust |
| ---               | :---:     | :---:    | :---:              | :---:  |
| License           |MIT License|Apache 2.0|proprietary software|MIT License|
| Open Source       |:heavy_check_mark:|:heavy_check_mark:    |    :heavy_multiplication_x:           |  :heavy_check_mark:|

## Purpose
|                   | Mongoose  | COSBench | LoadRunner | Locust |
| ---               | :---:     | :---:    | :---:      | :---:  |
| Load testing      | :heavy_check_mark:  |  :heavy_check_mark:  |   :heavy_check_mark:   | :heavy_check_mark: |
| Stress testing    | :heavy_check_mark:  |  :heavy_check_mark:  |            |        |
| Endurance testing | :heavy_check_mark:  |          |            |        |
| Sanity testing    | :heavy_check_mark:  |          |            |        |

## Scalability
|                                                    | Mongoose  | COSBench | LoadRunner | Locust |
| ---                                                | :---:     | :---:    | :---:      | :---:  |
| Horizontally (Distributed Mode)                    |   :heavy_check_mark:  |   :heavy_check_mark: |            |  :heavy_check_mark:|
| Vertically (Max sustained concurrency per instance)|1_048_576  |[1024](http://cosbench.1094679.n5.nabble.com/how-many-connections-users-can-cosbench-create-to-test-one-swift-storage-tp325p326.html)|            |1_000_000|

## Input
|                  | Mongoose  | COSBench | LoadRunner | Locust |
| ---              | :---:     | :---:    | :---:      | :---:  |
| GUI              |    :heavy_multiplication_x:  |   :heavy_check_mark: |  :heavy_check_mark:    | :heavy_multiplication_x:  |
| [Parameterization](https://github.com/emc-mongoose/mongoose/wiki/v3.6-Configuration#2-parametrization) |    :heavy_check_mark: |   :heavy_multiplication_x:  |            |  :heavy_check_mark:|
| Scriptable       |   :heavy_check_mark:  |  :heavy_check_mark:  |   :heavy_check_mark:   |  :heavy_check_mark:|
| Script format    |JSR-223 compatible languages|XML|[ANSI C, Java, .Net, JS](https://en.wikipedia.org/wiki/LoadRunner)|Python|

## Output
|                                      | Mongoose  | COSBench | LoadRunner | Locust |
| ---                                  | :---:     | :---:    | :---:      | :---:  |
| Metrics for each operation available |   :heavy_check_mark:  |   :heavy_check_mark: |            |        |
| Saturation concurrency measurement   |   :heavy_check_mark:  |   :heavy_multiplication_x:  |            |        |

## Load generation patterns
|                       | Mongoose  | COSBench | LoadRunner | Locust |
| ---                   | :---:     | :---:    | :---:      | :---:  |
| Weighted load support |    :heavy_check_mark: | :heavy_check_mark:   |            |        |
| Pipeline load         |    :heavy_check_mark: | :heavy_multiplication_x:    |            |        |
| Recycle mode          |    :heavy_check_mark: |:heavy_check_mark: (only for creation)|            |        |

## Storages support

* Locust and LoadRunner are tools for testing web services

|                                          | Mongoose  | COSBench |
| ---                                      | :---:     | :---:    |
| Supported storages                       |Amazon S3, EMC Atmos, OpenStack Swift, Filesystem, HDFS|OpenStack Swift, Amazon S3, Amplidata, Scality, Ceph, CDMI, Google Cloud Storage, Aliyun OSS|
| Extensible to support custom storage API |  :heavy_check_mark:   |  :heavy_check_mark:  |
