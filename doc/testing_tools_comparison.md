# Storage performance testing tools comparison

## General
|                   | Mongoose  | COSBench | LoadRunner         | Locust |
| ---               | :---:     | :---:    | :---:              | :---:  |
| License           |           |Apache 2.0|proprietary software|        |
| Open Source       | y         |  y       |      n             |    y   |

## Purpose
|                   | Mongoose  | COSBench | LoadRunner | Locust |
| ---               | :---:     | :---:    | :---:      | :---:  |
| Load testing      |     y     |    y     |     y      |        |
| Stress testing    |     y     |          |            |        |
| Endurance testing |     y     |          |            |        |
| Sanity testing    |     y     |          |            |        |

## Scalability
|                                                    | Mongoose  | COSBench | LoadRunner | Locust |
| ---                                                | :---:     | :---:    | :---:      | :---:  |
| Horizontally (Distributed Mode)                    |     y     |     y    |            |        |
| Vertically (Max sustained concurrency per instance)|1_048_576  |          |            |        |

## Input
|                  | Mongoose  | COSBench | LoadRunner | Locust |
| ---              | :---:     | :---:    | :---:      | :---:  |
| GUI              |      n    |     y    |    y       |        |
| Parameterization |      y    |     y    |            |        |
| Scriptable       |     y     |    y     |     y      |        |

## Output
|                                      | Mongoose  | COSBench | LoadRunner | Locust |
| ---                                  | :---:     | :---:    | :---:      | :---:  |
| Metrics for each operation available |     y     |     y    |            |        |
| Saturation concurrency measurement   |     y     |     n    |            |        |
| Load generation patterns             |     y     |          |            |        |

## Load generation patterns
|                       | Mongoose  | COSBench | LoadRunner | Locust |
| ---                   | :---:     | :---:    | :---:      | :---:  |
| Weighted load support |      y    |          |            |        |
| Pipeline load         |      y    |          |            |        |
| Recycle mode          |      y    |          |            |        |

## Storages support
|                                          | Mongoose  | COSBench | LoadRunner | Locust |
| ---                                      | :---:     | :---:    | :---:      | :---:  |
| Supported storages                       |Amazon S3, EMC Atmos, OpenStack Swift, Filesystem, HDFS|Amazon S3, Swift, Ceph, Amplistor, Scality, CDMI|            |        |
| Extensible to support custom storage API |           |          |            |        |
