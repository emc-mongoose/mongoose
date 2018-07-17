# Storage performance testing tools comparison

## General
|                   | Mongoose  | COSBench | LoadRunner         | Locust |
| ---               | :---:     | :---:    | :---:              | :---:  |
| License           |           |Apache 2.0|proprietary software|        |
| Open Source       | y         |  y       |      n             |    y   |

## Purpose
|                   | Mongoose  | COSBench | LoadRunner | Locust |
| ---               | :---:     | :---:    | :---:      | :---:  |
| Load testing      |     y     |    y     |     y      |   y    |
| Stress testing    |     y     |    y     |            |        |
| Endurance testing |     y     |          |            |        |
| Sanity testing    |     y     |          |            |        |

## Scalability
|                                                    | Mongoose  | COSBench | LoadRunner | Locust |
| ---                                                | :---:     | :---:    | :---:      | :---:  |
| Horizontally (Distributed Mode)                    |     y     |     y    |            |    y   |
| Vertically (Max sustained concurrency per instance)|1_048_576  |          |            |> 1_000_000|

## Input
|                  | Mongoose  | COSBench | LoadRunner | Locust |
| ---              | :---:     | :---:    | :---:      | :---:  |
| GUI              |      n    |     y    |    y       |   n    |
| Parameterization |      y    |     y    |            |        |
| Scriptable       |     y     |    y     |     y      |    y   |

## Output
|                                      | Mongoose  | COSBench | LoadRunner | Locust |
| ---                                  | :---:     | :---:    | :---:      | :---:  |
| Metrics for each operation available |     y     |     y    |            |        |
| Saturation concurrency measurement   |     y     |     n    |            |        |

## Load generation patterns
|                       | Mongoose  | COSBench | LoadRunner | Locust |
| ---                   | :---:     | :---:    | :---:      | :---:  |
| Weighted load support |      y    |   y      |            |        |
| Pipeline load         |      y    |   n      |            |        |
| Recycle mode          |      y    |y (only for creation)|            |        |

## Storages support

* Locust and LoadRunner are tools for testing web services

|                                          | Mongoose  | COSBench |
| ---                                      | :---:     | :---:    |
| Supported storages                       |Amazon S3, EMC Atmos, OpenStack Swift, Filesystem, HDFS|OpenStack Swift, Amazon S3, Amplidata, Scality, Ceph, CDMI, Google Cloud Storage, Aliyun OSS|
| Extensible to support custom storage API |    y      |    y     |
