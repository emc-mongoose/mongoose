# Introduction

Frequently there's a requirement to perform a specific load which may be
defined by a ratio between different operation types. For example the
load step may be described as:
* 20 % write operations
* 80 % read operations

# Requirements

1. Ability to specify the relative operations weight for a set of load
sub-steps

# Approach

The weighted load is implemented as a specific scenario step:

```javascript
WeightedLoad
    .config(weightedWriteConfig)
    .config(weightedReadConfig)
    .run();
```

Internally, the weighted load step contains several
[load generator](doc/design/architecture/README.md#load-generator)s
(for each configuration element supplied) and single
[load step context](doc/design/architecture/README.md#load-step-context).
The load step context contains the
[weight throttle](https://github.com/akurilov/java-commons/blob/master/src/main/java/com/github/akurilov/commons/concurrent/throttle/SequentialWeightsThrottle.java)
shared among the load generators configured.

## Configuration

The configuration option `load-generator-weight` should be used to set
the relative operations weight for the given load generator. The actual
weight is calculated as the specified weight value divided by the
weights sum.

```javascript
var weightedWriteConfig = {
    ...
    "load": {
        "generator": {
            "weight": 20
        }
    },
    ...
};
var weightedReadConfig = {
    ...
    "load": {
        "generator": {
            "weight": 80
        },
        "type": "read"
    },
    ...
};
```

**Notes**
> * Full example may be found in the `example/scenario/js/types/weighted.js` scenario file.
> * Weight throttle will never permit the operations if the corresponding weight is 0

## Limitations

1. Weighted load step should contain at least 2 configuration elements.

## Output

Specific log messages:

1. `Run the weighted load step "<STEP_ID>"`
2. `Weighted load step "<STEP_ID>" started`
3. `Weighted load step "<STEP_ID>" done`
4. `Weighted load step "<STEP_ID>" timeout`
