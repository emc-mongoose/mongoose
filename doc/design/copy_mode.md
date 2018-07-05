# Overview

The copy mode functionality allows to perform a copy operation on the multiple
files instead of write one. The performance rates may be significantly
different for the copy and write operations. Some cloud storage APIs
also support copying the objects (S3 and Swift) so the functionality
may be a general. In case of S3 and Swift there's no payload sent while
copying the objects so these requests may be significantly faster than
writing new objects.

# Limitations

* Works only if the input path is different from the output one
* Copying the containers (Swift) is not supported
* Copying the buckets (S3) is not supported
* Copying the objects (Atmos) while using /rest/object interface is
  not supported yet

# Approach

## General

Copy mode is enabled if:
* `--load-type` option is set to `create` (this is by default) ***and***
* one of the item inputs is configured:
    * `--item-input-path` is set to existing bucket/container/directory ***or***
    * `--item-input-file` is set to existing items list file

## Filesystem Storage Case

In case of copying the filesystem directories there's a size may be
calculated, so there's a size and bandwidth (MB/sec) metrics
are available.

## HTTP Storage Case

> Note:
Cloud Storage object copy requests don't contain any payload so the
byte count related metrics are not calculated (remain zero).

### S3 Objects Copying

The source object path is specified with "**x-amz-copy-source**" header.

### Swift Objects Copying

There are two variants of object copy requests:

* Using HTTP method `COPY` and `Destination` header.
* Using HTTP method `PUT` and `X-Copy-From` header.

The 2nd variant is preferred ss far as COPY HTTP method is not standard.

The source object URI is specified with `X-Copy-From` header.

# Configuration

In order to perform a copy load step it's necessary:

* Use "create" load type.
* Specify "**--item-input-path**" (the source container/bucket/directory, contains the items to copy) or "**--item-input-file**"
    to a proper value.
* Specify "**--item-output-path*" (the target container/bucket/directory) to a proper value.

For details, see the example scenario located at:
`<USER_HOME_DIR>/.mongoose/<VERSION>/example/scenario/js/types/additional/copy_load_using_env_vars.js`.
