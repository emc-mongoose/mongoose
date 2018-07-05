**Note**:
In *distributed mode* a custom storage driver implementation jar file
should be located on the Mongoose remote node hosts (in the
`<USER_HOME>/.mongoose/<VERSION>/ext` directory).

# 1. Storage Drivers Inheritance

```
storage-driver
|── storage-driver-coop
|   |── storage-driver-coop-net
|   |   └── storage-driver-coop-net-http
|   |       |── storage-driver-coop-net-http-atmos
|   |       |── storage-driver-coop-net-http-s3
|   |       └── storage-driver-coop-net-http-swift
|   └── storage-driver-nio
|       └── storage-driver-nio-fs
└── storage-driver-preempt
```


# 2. Implementing Custom Storage Driver

2. Implement the custom storage driver by coding the required methods.

    Mongoose includes the following abstract storage driver classes:
    * `com.emc.mongoose.storage.driver.StorageDriverBase`
    * `com.emc.mongoose.storage.driver.coop.CooperativeStorageDriverBase`
    * `com.emc.mongoose.storage.driver.coop.nio.NioStorageDriverBase`
    * `com.emc.mongoose.storage.driver.coop.net.NetStorageDriverBase`
    * `com.emc.mongoose.storage.driver.coop.net.http.HttpStorageDriverBase`
    * `com.emc.mongoose.storage.driver.preemptive.PreemptiveStorageDriverBase`
    These are designed for the extension.

    Also there are some ready-to-use implementations provided with
    Mongoose itself which may be extended also.

3. Implement the
`com.emc.mongoose.storage.driver.base.StorageDriverFactory` interface
and extend the class `com.emc.mongoose.env.ExtensionBase`. The following
methods should be implemented:

    * `id`: return a constant **storage driver implementation
    identifier** (for example "atmos" or "hdfs")

    * `create`: return the new storage driver instance

    * `schemaProvider`: return the specific configuration *schema
    provider* instance

    * `defaultsFileName`: return the specific configuration defaults
    file name

    * `resourceFilesToInstall`: return the list of the relative file
    paths which should be installed (usually this is a specific defaults
    file)

4. Put into the resources directory the file with relative path
`META-INF/services/com.emc.mongoose.env.Extension`
and then put the fully qualified class name of the custom
`StorageDriverFactory` implementation (from the step #3) to that file.

5. Build the storage driver implementation jar file.

6. Put the storage driver implementation jar file into the
`<USER_HOME_DIR>/.mongoose/<VERSION>/ext` directory.

7. Run Mongoose with an argument `--storage-driver-type=X` where X is
the custom **storage driver implementation identifier**.

# 3. Cooperative Storage Driver

Cooperative storage drivers uses the fibers to process the I/O tasks in the most efficient way. Also it supports
composite I/O tasks.

## 3.1 NIO Storage Driver

This abstract implementation uses few I/O threads to execute a lot if I/O tasks in parallel. Actual I/O work should be
executed in the method `invokeNio(ioTask)` in the reentrant and non-blocking manner.

## 3.2. Netty-Based Storage Driver

This abstract implementation is intended to work with distributed storage with multiple endpoint nodes accessible via
the network. Provides high-performance connection pool, simple endpoint node balancing, SSL/TLS functionality.

### 3.2.1. HTTP Storage Driver

This abstract implementation inherits the Netty-Based one and adds the HTTP-related specific functionality.

# 4. Preemptive Storage Driver

Some storage APIs don't support non-blocking I/O calls. The storage driver should provide the OS thread entirely for
each connection/open file to drive the I/O. This way is not very efficient while high concurrency level is used either
large data blocks are transferred.
