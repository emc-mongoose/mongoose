# Recycling The Load Operations

> See also the [Recycling Specification](../../../../design/recycle_mode).

The load operations recycling capability allows to perform the load operations multiple times on each item. This  may be
useful to `read` or `update` the same items multiple times each.

**Example**:
```bash
java -jar mongoose-<VERSION>.jar --read --load-op-recycle --item-input-file=items.csv ...
```

**Note**:
> No more than 1M of unique load operations may be recycled by default. The larger value should be set if the count of
> the items from the specified input is more. The configuration option `load-op-limit-recycle` addresses this case.
