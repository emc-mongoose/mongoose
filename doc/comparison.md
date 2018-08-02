# Similiar tools
* [COSBench](https://github.com/intel-cloud/cosbench)
* [LoadRunner](https://software.microfocus.com/en-us/products/loadrunner-load-testing/overview)
* [Locust](https://locust.io/)

## General
|                   | Mongoose  | COSBench | LoadRunner         | Locust |
| ---               | :---:     | :---:    | :---:              | :---:  |
|**License**        |[MIT License](../LICENSE)|[Apache 2.0](https://github.com/intel-cloud/cosbench/blob/master/LICENSE)|[Proprietary](https://en.wikipedia.org/wiki/LoadRunner)|[MIT License](https://github.com/locustio/locust/blob/master/LICENSE)|
|**Open Source**    |:heavy_check_mark:|:heavy_check_mark:    |:x:|  :heavy_check_mark:|

## Purpose
|                   | Mongoose  | COSBench | LoadRunner | Locust |
| ---               | :---:     | :---:    | :---:      | :---:  |
|**[Load testing](https://en.wikipedia.org/wiki/Load_testing)** |:heavy_check_mark:|:heavy_check_mark:|:heavy_check_mark:|:heavy_check_mark:|
|**[Stress testing](https://en.wikipedia.org/wiki/Stress_testing)** |:heavy_check_mark:|:heavy_check_mark:|:heavy_check_mark:| TBD |
|**[Endurance testing](https://en.wikipedia.org/wiki/Soak_testing)**|:heavy_check_mark:| TBD |:heavy_check_mark:| TBD |

## Scalability
|                                                    | Mongoose  | COSBench | LoadRunner | Locust |
| ---                                                | :---:     | :---:    | :---:      | :---:  |
|**Horizontal** (Distributed Mode)                 |:heavy_check_mark:|:heavy_check_mark:| TBD |:heavy_check_mark:|
|**Vertical** (Max sustained concurrency per instance)|[1_048_576](https://github.com/emc-mongoose/mongoose/blob/feature-v4-doc/doc/features.md#12-fibers)|[1024](http://cosbench.1094679.n5.nabble.com/how-many-connections-users-can-cosbench-create-to-test-one-swift-storage-tp325p326.html)| TBD |[1_000_000](https://locust.io/)|

## Input
|                  | Mongoose  | COSBench | LoadRunner | Locust |
| ---              | :---:     | :---:    | :---:      | :---:  |
|**GUI**           |:x:|:heavy_check_mark:|:heavy_check_mark:|:heavy_check_mark:|
|**Parameterization**|:heavy_check_mark:| :heavy_check_mark: | TBD |:heavy_check_mark:(need to extend the functionality)|
|**Script language**| Any [JSR-223](https://en.wikipedia.org/wiki/Scripting_for_the_Java_Platform) compatible |[XML](https://en.wikipedia.org/wiki/XML)|[ANSI C, Java, .Net, JS](https://en.wikipedia.org/wiki/LoadRunner)|[Python](https://en.wikipedia.org/wiki/Python)|

## Output
|                                        | Mongoose  | COSBench | LoadRunner | Locust |
| ---                                    | :---:     | :---:    | :---:      | :---:  |
|**Highest-resolution (per each op) metrics**|:heavy_check_mark:|:x:| TBD |:x:|
|**Saturation concurrency measurement**  |:heavy_check_mark:|:x:| TBD |:x:|

## Load generation patterns
|                       | Mongoose  | COSBench | LoadRunner | Locust |
| ---                   | :---:     | :---:    | :---:      | :---:  |
|**[Weighted load](design/weighted_load.md)**|:heavy_check_mark:| :heavy_check_mark:| TBD |:x:|
|**[Pipeline load](design/pipeline_load.md)**|:heavy_check_mark:| :x:| TBD |:x:|
|**[Recycle mode](design/recycle_mode.md)**|:heavy_check_mark: |:x:| TBD |:x:|

## Storages support

* **Note**: Locust and LoadRunner are not designed for the storage performance testing explicitly so they are excluded
from the table below

|                                            | Mongoose  | COSBench |
| ---                                        | :---:     | :---:    |
|**Supported storages**                      |<ul><li>Amazon S3</li><li>EMC Atmos</li><li>OpenStack Swift</li><li>Filesystem</li><li>HDFS</li><ul>|<ul><li>Amazon S3</li><li>Amplidata</li><li>OpenStack Swift</li><li>Scality</li><li>Ceph</li><li>Google Cloud Storage</li><li>Aliyun OSS</li><ul>|
|**Extensible to support custom storage API**|  :heavy_check_mark:   | :heavy_check_mark: |
