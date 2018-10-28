# Atmos Storage Driver

## 1. Features

* API version: ?
* Authentification:
    * Requests are signed with a secret key if configured
    * Using subtenant
* Filesystem access
* SSL/TLS
* Item types:
    * `data` (--> "object")
    * `token` (-> "subtenant")
* Automatic subtenant creation on demand
* Data item operation types:
    * `create`
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
    * `read`
    * `delete`
    * `noop`

## 2. Usage

```bash
java -jar mongoose-<VERSION>.jar \
    --storage-driver-type=atmos \
    ...
```

### 2.1. Configuration Reference

| Name                                           | Type         | Default Value    | Description                                      |
|:-----------------------------------------------|:-------------|:-----------------|:-------------------------------------------------|
| storage-net-http-fsAccess                      | Flag | false | Specifies whether filesystem access is enabled or not
| storage-net-http-namespace                     | String | null | The HTTP storage namespace.

### 2.2. Notes

* To specify a subtenant use the `storage-auth-token` configuration option
