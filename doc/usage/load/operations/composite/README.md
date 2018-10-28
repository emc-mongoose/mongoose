# Composite Load Operations

## 1. Storage Side Concatenation

Some cloud storages support the concatenation of the data item parts written independently.
Storage-side concatenation (further - ***SSC***).

### 1.1. Limitations

1. The storage API supporting the SSC is used. These are `s3`, `emcs3` and `swift` currently.
2. In the distributed mode, all data item parts are processed by the single storage driver.
3. "Create" load type is used to split the *large* data items into the parts to write them
 separately.
4. It's strongly recommended to set "load-batch-size" configuration parameter to "1".

### 1.2. Approach

Mongoose has the so called *load operation* abstraction. Load operations are executed by the specific storage
drivers. The storage driver may be able to detect the special *composite* load operations and execute the
corresponding sequence of the *partial sub-operations*:

1. Initiate the SSC for the given data item.
2. Create the data item parts on the storage.
3. Commit the data item SSC on the storage.

### 1.3. Storage Drivers Support

#### 1.3.1. S3 Multipart Upload

https://docs.aws.amazon.com/AmazonS3/latest/dev/uploadobjusingmpu.html

#### 1.3.2. Swift Dynamic Large Objects

https://docs.openstack.org/swift/latest/overview_large_objects.html#direct-api

### 1.2. Configuration

The "***item-data-ranges-threshold***" configuration parameter controls the SSC behavior.
The value is the [size in bytes](../../../../interfaces/input/configuration#132-size). Any new generated object is
treated as "*large*" if its size is more or equal than the configured threshold. *Large* objects are
being split into the 2 or more parts with the size not more than the configured value above.

### 1.3. Reporting

#### 1.3.1. Parts List Output

The record containing the object name and the corresponding upload id is written to the
`parts.upload.csv` file if SSC operation is finished. The upload completion response latency is also
persisted in the 3rd column.

#### 1.3.2 Future Enhancements

* Support Read for the segmented objects
* Support Update for the segmented objects
* Support Copy for the segmented objects
