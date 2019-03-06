# Load Steps

## 1. Identification

The load step ids are used primarily to distinguish the load step results.
```bash
java -jar mongoose-<VERSION>.jar --load-step-id=custom_test_0 ...
```

See also the [output reference](../../../interfaces/output#111-load-step-id)

## 2. Limits

By default the load steps are not limited explicitly. There are several ways to limit the load steps execution.

### 2.1. Operations Count

Limit the load step by the operation count:
```bash
java -jar mongoose-<VERSION>.jar --load-op-limit-count=1000000 ...
```

### 2.2. Time

Limit the load step by the time (5 minutes):
```bash
java -jar mongoose-<VERSION>.jar --load-step-limit-time=5m ...
```

### 2.3. Transfer Size

Limit the load step by the transfer size:
```bash
java -jar mongoose-<VERSION>.jar --load-step-limit-size=1.234TB ...
```

### 2.4. End Of Input

> *"EOI" = "End Of Input"*

A load step is also limited by the load operations *EOI*. End of the load operations input is reached if:
* Load operations recycling is disabled and end of the items *EOI* is reached
* Load operations recycling is enabled but all the load operations are failed (there's no successfull load operations to
  recycle)

