# Swift Storage Driver

## 1. Features

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
        * [copy](../../../../../../doc/design/copy_mode/README.md)
        * [Dynamic Large Objects](../../../../../../src/main/java/com/emc/mongoose/item/op/composite/README.md)
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

## 2. Usage

```bash
java -jar mongoose-<VERSION>.jar \
    --storage-driver-type=swift \
    ...
```

### 2.1. Configuration Reference

| Name                                           | Type         | Default Value    | Description                                      |
|:-----------------------------------------------|:-------------|:-----------------|:-------------------------------------------------|
| storage-net-http-versioning                    | Flag | false | Specifies whether the versioning storage feature is used or not

### 2.2. Notes

* The default `storage-namespace` value (null) will not work in the case of Swift API
* A **container** may be specified with `item-input-path` either `item-output-path` configuration option
* DLO creation should be enabled using the `item-data-ranges-threshold` configuration option
