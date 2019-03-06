# Item Types

## 1. Data

Data items are used by default. The exact meaning may depend on the storage selected for the test. This is a *file* in
case of the filesystem storage, *object* in case of Atmos/S3/Swift storage.

### 1.1. Size

The `item-data-size` configuration option is used to specify the payload size for any new data items.

#### 1.1.1. Fixed

Fixed data item size is used by default. The default size value is 1MB.
```bash
java -jar mongoose-<VERSION>.jar --item-data-size=10KB ...
```

#### 1.1.2. Random

It's possible to use the random size for the new items within the specified range:
```bash
java -jar mongoose-<VERSION>.jar --item-data-size=5MB-15MB ...
```

The payload size for the new items may be also biased to the lower either upper bound:
```bash
java -jar mongoose-<VERSION>.jar --item-data-size=0-100MB,0.2 ...
```

Note:
* The bias value is appended to the range after the comma (0.2 in the example above).
* The generated value is biased towards the high end if bias value is less than 1.
* The generated value is biased towards the low end if bias value is more than 1.

### 1.2. Payload

**Note**:
> Same `item-data-input-*` configuration options should be used for `create`, `update` and `read` load operations. The
> content verification read capability will not work correctly otherwise.

#### 1.2.1. Random Using A Seed

The uniform random data payload is used by default. It uses the configurable seed number to pre-generate some amount
(4MB by default) of the random uniform data. To use the custom seed use the following option:
```bash
java -jar mongoose-<VERSION>.jar --item-data-input-seed=5eed42b1gb00b5 ...
```

#### 1.2.2. Custom Using An External File

```bash
java -jar mongoose-<VERSION>.jar --item-data-input-file=<PATH_TO_CONTENT_FILE>
```

## 2. Path

The path items type may be useful to work with directories/buckets/containers (depending on the storage driver type
used):
```bash
java -jar mongoose-<VERSION>.jar --item-type=path ...
```

## 3. Token

There are also an additional item type which may represent some storage-specific tokens like *subtenant*(Atmos) or
*auth token* (Swift):
```bash
java -jar mongoose-<VERSION>.jar --item-type=token ...
```
