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

```bash
java -jar mongoose-<VERSION>.jar \
    --storage-driver-type=atmos \
    ...
```

## Notes

* To specify a subtenant use the `storage-auth-token` configuration option
