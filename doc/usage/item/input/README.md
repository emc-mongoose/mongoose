# Items Input

Items input is a source of the items which should be used to perform the load operations (create/read/etc).

## 1. File

The items once created with Mongoose may be stored into the output CSV file. This file may be reused on input for
other load operations, such as a `copy`, `read`, `update`, `delete` and `noop`.
```bash
java mongoose-<VERSION>.jar --item-input-file=items.csv ...
```

## 2. Item Path Listing Input

In some cases the items list file is not available. The items list may be obtained on the fly from the *storage path*
listing.
```bash
java mongoose-<VERSION>.jar --item-input-path=/path_to_list ...
```
**Note**:
> It may be useful to combine this option with `noop` to enumerate the items on the storage located in the specified
> path. Combining also with `item-output-file` option allows to store the enumerated items information for a further
> usage.

## 3. New Items Input

New items generator is used if no *items input file* neither *item input path* is specified and the configured load
operations type is `create`.

### 3.1. Naming

A new items generator may use custom items naming scheme.

#### 3.1.1. Types

The item naming type defines the function used to calculate the next item id from the previous one.

##### 3.1.1.1. Random

Random item ids are used by default

##### 3.1.1.2. Serial

Generate new item ids in the ascending order (starting from 0):
```bash
java mongoose-<VERSION>.jar --item-naming-type=serial --item-naming-seed=-1 --item-naming-step=1 ...
```

Generate new item ids in the descending order (starting from 999):
```bash
java mongoose-<VERSION>.jar --item-naming-type=serial --item-naming-seed=1000 --item-naming-step=-1 ...
```

#### 3.1.2. Prefix

The prefix option may be used to prepend some value for each new item.

```bash
java mongoose-<VERSION>.jar --item-naming-prefix=item_prefix_ ...
```

The prefix may be dynamic (see [expressions](base/src/java/com/emc/mongoose/base/config/el/README.md)).

#### 3.1.3. Radix

The radix option is used to encode the source number into the id. The radix should be in the range of \[2; 36].

```bash
java mongoose-<VERSION>.jar --item-naming-radix=16 ...
```

#### 3.1.4. Seed

The item ids will start from the next value calculated using the specified seed.

```bash
java mongoose-<VERSION>.jar --item-naming-seed=9876543210 ...
```

#### 3.1.5. Length

The length option determines the id length for the new item ids. The minor bits are used if the source number is
truncated.

```bash
java mongoose-<VERSION>.jar --item-naming-lenth=15 ...
```

