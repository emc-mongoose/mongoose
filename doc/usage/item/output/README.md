# Items Output

## 1. File

It's possible to store the info about the item for each successful load operation.
```bash
java -jar mongoose-<VERSION>.jar --item-output-file=items.csv ...
```
**Note**:
> 1. The items output file includes also its modification state information for further reading and verification.
> 2. The unique items are stored in case of [load operations recycling](../../load/operations/recycling). So each stored
> item has the most recent modification information if the item was modified multiple times (`update` with recycling
> case).

## 2. Path

For new items (basic `create` case) the storage destination path may be specified.
```bash
java -jar mongoose-<VERSION>.jar --item-output-path=/storage/path/for/the/new/items
```

**Note**:
> The *storage path* has specific meaning in terms of the particular storage. For example, this means *directory* in
> case of the filesystem storage driver, *bucket* in case of the S3, *container* in case of Swift, etc.

### 2.1. Variable

The items output path also supports the [expression language](base/src/main/com/emc/mongoose/config/el/README.md).
Example: dynamic files output path defined by some particular "width" (16) and "depth" (2):
```bash
java -jar mongoose-<VERSION>.jar \
    --item-output-path=/mnt/storage/\$\{path\:random\(16\,\ 2\)\} \
    --storage-driver-type=fs \
    ...
```

#### 2.1.1. Multiuser

Let's realize the case when someone needs to perform a load using many (hundreds, thousands)
destination paths (S3 buckets, Swift containers, filesystem directories, etc) using many different
credentials.

```javascript
var multiUserConfig = {
    "item" : {
        "data" : {
            "size" : "10KB"
        },
        "output" : {
            "file" : "objects.csv",
            "path" : "/bucket-${rnd.nextInt(100)}"
        }
    },
    "load" : {
        "op" : {
            "limit" : {
                "count" : 10000
            }
        }
    },
    "storage" : {
        "auth" : {
            "file" : "credentials.csv",
        },
        "driver" : {
            "limit" : {
                "concurrency" : 10
            },
            "type" : "s3"
        }
    }
};

Load
    .config(multiUserConfig)
    .run();
```

**Note**:
> * The current externally loaded credentials count limit is 1 million.

In this case, the file "credentials.csv" should be prepared manually. Example contents:
```
/bucket-0,user-0,secret-0
/bucket-1,user-1,secret-1
/bucket-2,user-2,secret-2
...
/bucket-99,user-99,secret-99
```
(in the real world it is expected that the storage users with the listed names and secret keys are already existing)
