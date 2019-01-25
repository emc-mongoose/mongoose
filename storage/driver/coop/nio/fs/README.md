# Introduction

The storage driver implementation may be used to perform file non-blocking I/O. Typically used totest CIFS/HDFS/NFS
shares mounted locally. However, testing distributed filesystems via the mounted shares may be not accurate due to
additional VFS layer. The measured rates may be:
* Inadequately low due to frequent system calls
* Higher than network bandwidth due to local caching by VFS

# Features

* Authentification: N/A
* Item types: `data` only (--> "file")
* Path listing input
* Automatic destination path creation on demand
* Data item operation types:
    * `create`, additional modes:
        * [copy](../../../../../doc/design/copy_mode/README.md)
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

# Usage

```bash
java --module-path mongoose-<VERSION>.jar --module com.emc.mongoose \
    --storage-driver-type=fs \
    ...
```
