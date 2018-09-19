# Features

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

# Usage

Latest stable pre-built jar file is available at:
https://github.com/emc-mongoose/mongoose-storage-driver-atmos/releases/download/latest/mongoose-storage-driver-atmos.jar
This jar file may be downloaded manually and placed into the `ext`
directory of Mongoose to be automatically loaded into the runtime.

```bash
java -jar mongoose-<VERSION>.jar \
    --storage-driver-type=atmos \
    ...
```

## Notes

* To specify a subtenant use the `storage-auth-token` configuration option
