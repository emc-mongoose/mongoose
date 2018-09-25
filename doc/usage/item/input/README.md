# Items Input

Items input is a source of the items which should be used to perform the load operations (create/read/etc).

## 1. File

```bash
java -jar mongoose-<VERSION>.jar --item-input-file=items.csv ...
```

## 2. Item Path Listing Input

```bash
java -jar mongoose-<VERSION>.jar --item-input-path=/path_to_list ...
```

## 3. New Items Input

New items generator is used if no items input file neither item input path is specified and the configured load
operations type is `create`.

### 3.1. Naming

A new items generator may use custom items naming scheme.

#### 3.1.1. Types

##### 3.1.1.1. Random

Random new items naming pattern is used by default

##### 3.1.1.2. Ascending



```bash
java -jar mongoose-<VERSION>.jar --item-naming-type=asc ...
```

##### 3.1.1.3. Descending

```bash
java -jar mongoose-<VERSION>.jar --item-naming-type=desc ...
```

#### 3.1.2. Prefix

```bash
java -jar mongoose-<VERSION>.jar --item-naming-prefix=item_prefix_ ...
```

#### 3.1.3. Radix

The radix option is used to encode the source number into the id. The radix should be in the range of \[2; 36].

```bash
java -jar mongoose-<VERSION>.jar --item-naming-radix=16 ...
```

#### 3.1.4. Offset

The offset configuration option is useful when ascending/descending ids order is used. The item ids will start from the
specified offset.

```bash
java -jar mongoose-<VERSION>.jar --item-naming-offset=9876543210 ...
```

#### 3.1.5. Length

The length option determines the id length for the new item ids. The minor bits are used if the source number is
truncated.

```bash
java -jar mongoose-<VERSION>.jar --item-naming-lenth=15 ...
```

