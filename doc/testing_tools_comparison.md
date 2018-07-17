# Storage performance testing tools comparison
* [COSBench](https://github.com/intel-cloud/cosbench)
* [LoadRunner](https://software.microfocus.com/en-us/products/loadrunner-load-testing/overview)
* [Locust](https://locust.io/)

## General
|                   | Mongoose  | COSBench | LoadRunner         | Locust |
| ---               | :---:     | :---:    | :---:              | :---:  |
|**License**        |[MIT License](https://en.wikipedia.org/wiki/MIT_License)|[Apache 2.0](https://en.wikipedia.org/wiki/Apache_License#Version_2.0)|[proprietary software](https://en.wikipedia.org/wiki/Proprietary_software)|[MIT License](https://en.wikipedia.org/wiki/MIT_License)|
|**Open Source**    |:heavy_check_mark:|:heavy_check_mark:    |:x:|  :heavy_check_mark:|

## Purpose
|                   | Mongoose  | COSBench | LoadRunner | Locust |
| ---               | :---:     | :---:    | :---:      | :---:  |
|**[Load testing](https://en.wikipedia.org/wiki/Load_testing)** |:heavy_check_mark:|:heavy_check_mark:|:heavy_check_mark:|:heavy_check_mark:|
|**[Stress testing](https://en.wikipedia.org/wiki/Stress_testing)** |:heavy_check_mark:|:heavy_check_mark:|      |      |
|**[Endurance testing](https://en.wikipedia.org/wiki/Soak_testing)**|:heavy_check_mark:|          |       |      |

## Scalability
|                                                    | Mongoose  | COSBench | LoadRunner | Locust |
| ---                                                | :---:     | :---:    | :---:      | :---:  |
|**Horizontally** (Distributed Mode)                 |:heavy_check_mark:|:heavy_check_mark:|            |:heavy_check_mark:|
|**Vertically** (Max sustained concurrency per instance)|[1_048_576](https://github.com/emc-mongoose/mongoose/blob/feature-v4-doc/doc/features.md#12-fibers)|[1024](http://cosbench.1094679.n5.nabble.com/how-many-connections-users-can-cosbench-create-to-test-one-swift-storage-tp325p326.html)|            |[1_000_000](https://locust.io/)|

## Input
|                  | Mongoose  | COSBench | LoadRunner | Locust |
| ---              | :---:     | :---:    | :---:      | :---:  |
|**GUI**           |:x:|:heavy_check_mark:|:heavy_check_mark:|:x:|
|**[Parameterization](https://github.com/emc-mongoose/mongoose/wiki/v3.6-Configuration#2-parametrization)**|:heavy_check_mark:|   :heavy_check_mark:  |            |:heavy_check_mark:(at the level of the language)|
|**Script (scenario) format**|[JSR-223](https://en.wikipedia.org/wiki/Scripting_for_the_Java_Platform) compatible languages|[XML](https://en.wikipedia.org/wiki/XML)|[ANSI C, Java, .Net, JS](https://en.wikipedia.org/wiki/LoadRunner)|[Python](https://en.wikipedia.org/wiki/Python)|

## Output
|                                        | Mongoose  | COSBench | LoadRunner | Locust |
| ---                                    | :---:     | :---:    | :---:      | :---:  |
|**Metrics for each operation available**|:heavy_check_mark:|:x:|            |:x:|
|**Saturation concurrency measurement**  |:heavy_check_mark:|:x:|            |:x:|

## Load generation patterns
|                       | Mongoose  | COSBench | LoadRunner | Locust |
| ---                   | :---:     | :---:    | :---:      | :---:  |
|**[Weighted load](https://github.com/emc-mongoose/mongoose/blob/feature-v4-doc/doc/design/weighted_load.md)**|:heavy_check_mark:| :heavy_check_mark:|            |:x:|
|**[Pipeline load](https://github.com/emc-mongoose/mongoose/blob/feature-v4-doc/doc/design/pipeline_load.md)**|:heavy_check_mark:| :x:|            |:x:|
|**[Recycle mode](https://github.com/emc-mongoose/mongoose/blob/feature-v4-doc/doc/design/recycle_mode.md)**|:heavy_check_mark: |:x:|        |:x:|

## Storages support

* Locust and LoadRunner are tools for testing web services

|                                            | Mongoose  | COSBench |
| ---                                        | :---:     | :---:    |
|**Supported storages**                      |<ul><li>Amazon S3</li><li>EMC Atmos</li><li>OpenStack Swift</li><li>Filesystem</li><li>HDFS</li><ul>|<ul><li>Amazon S3</li><li>Amplidata</li><li>OpenStack Swift</li><li>Scality</li><li>Ceph</li><li>Google Cloud Storage</li><li>Aliyun OSS</li><ul>|
|**Extensible to support custom storage API**|  :heavy_check_mark:   | :heavy_check_mark: |
