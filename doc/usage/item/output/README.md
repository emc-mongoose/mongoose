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