# Features

* API version: 2006-03-01
* Authentification:
    * Uid/secret key pair to sign each request
* SSL/TLS
* Item types:
    * `data` (--> "object")
    * `path` (--> "bucket")
* Automatic destination path creation on demand
* Path listing input (with XML response payload)
* Data item operation types:
    * `create`
        * [copy](doc/design/copy_mode/README.md)
        * [Multipart Upload](src/main/java/com/emc/mongoose/item/op/composite/README.md)
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
* Path item operation types:
    * `create`
    * `read`
    * `delete`
    * `noop`

# Usage

```bash
java -jar mongoose-<VERSION>.jar \
    --storage-driver-type=s3 \
    ...
```

## Notes

* A **bucket** may be specified with `item-input-path` either `item-output-path` configuration option
* Multipart upload should be enabled using the `item-data-ranges-threshold` configuration option
