# Load Operation Types

## 1. Create

### 1.1. Basic

The `create` load type is used by default and it is intended to create (write) new items on the storage.

### 1.2. Copy Mode

Example scenario:
```javascipt
// prepare some items on the storage for further copying
PreconditionLoad
    .config(
        {
            "item": {
                "output": {
                    "path": "/item/src/path"
                }
            },
            "load": {
                "step": {
                    "limit": {
                        "size": "1GB"
                    }
                }
            }
        }
    )
    .run();

// copy the items from one storage path to another
Load
    .config(
        {
            "item": {
                "input": {
                    "path": "/item/src/path"
                },
                "output": {
                    "path": "/item/dst/path"
                }
            }
        }
    )
    .run();
```

For additional usage details refer to the [Copy Mode Specification](../../../design/copy_mode).

**Note**:
> Not all storage driver implementations support the copy mode. For details refer to the particular storage driver
> implementation

## 2. Read

The exact `read` operation behaviour depends on the particular item type and the storage driver used.

**Note**:
> Read operations require the obviously configured [items input](../../../item/input).
> The content verification is disabled by default.

### 2.1. Basic

By default the data is just being read from the storage and discarded immediately.
```bash
java -jar mongoose-<VERSION>.jar --read ...
```

### 2.2. Content Verification

The content verification capability allows to compare the item data being read from the storage with the expected data.
The expected data is being described by the [item data input](../../../item/types/#12-payload) and the
[items input](../../../item/input). Note that the storage path items listing input is not recommended to use if the
items have been updated (modified).

```bash
java -jar mongoose-<VERSION>.jar --read --item-data-verify --item-input-file=items.csv ...
```

An load operation is reported with the [status code](../../../../interfaces/output#232-files) 12 if the content verification fails.

## 3. Update

By default the `update` operation rewrites the whole corresponding data item on the storage. The different
[item data input]((../../../item/types/#12-payload)) may be used to rewrite the data item with different data sequence.

**Note**:
> * Update operations require the obviously configured [items input](../../../item/input).
> * Take care about the correct [item data input](../../../item/types/#12-payload) configuration if the updating items
>   should be read with enabled verification further.
> * Save the updated items list to the [item output file](.../../../item/output#1-file) to make it possible to read
>   these items with enabled verification later.

```bash
java -jar mongoose-<VERSION>.jar --update ...
```

## 4. Delete

Deletes the items on the storage if the operation is supported by the storage driver used.

**Note**:
> Delete operations require the obviously configured [items input](../../../item/input).

```bash
java -jar mongoose-<VERSION>.jar --delete ...
```

## 5. Noop

Does nothing with the items. May be useful for demo/testing either to
[enumerate the given item input storage path](../../../item/output#2-path).

```bash
java -jar mongoose-<VERSION>.jar --noop ...
```



