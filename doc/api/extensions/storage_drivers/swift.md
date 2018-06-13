# Features

* API version: 1.0
* Authentification:
    * Uid/secret key pair for auth token operations
    * Auth token
* SSL/TLS
* Item types:
    * `data` (--> "object")
    * `path` (--> "container")
    * `token` (--> "auth token")
* Automatic auth token creation on demand
* Automatic destination path creation on demand
* Path listing input (with JSON response payload)
* Data item operation types:
    * `create`
        * [copy](../../../design/copy_mode.md)
        * [Dynamic Large Objects](../../../design/storage_side_concatenation.md)
    * `read`
        * full
        * random byte ranges
        * fixed byte ranges
        * content verification
    * `update`
        * full (overwrite)
        * random byte ranges
        * fixed byte ranges (with append mode)
    * `delete`
    * `noop`
* Token item operation types:
    * `create`
    * `noop`
* Path item operation types:
    * `create`
    * `read`
    * `delete`
    * `noop`

# Usage

Latest stable pre-built jar file is available at:
https://github.com/emc-mongoose/mongoose-storage-driver-swift/releases/download/latest/mongoose-storage-driver-swift.jar
This jar file may be downloaded manually and placed into the `ext`
directory of Mongoose to be automatically loaded into the runtime.

```bash
java -jar mongoose-<VERSION>/mongoose.jar \
    --storage-driver-type=swift \
    ...
```

## Notes

* A **container** may be specified with `item-input-path` either `item-output-path` configuration option
* DLO creation should be enabled using the `item-data-ranges-threshold` configuration option
