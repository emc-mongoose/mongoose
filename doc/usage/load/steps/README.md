# Load Steps

## 1. Identification

The load step ids are used primarily to distinguish the load step results.
```bash
java -jar mongoose-<VERSION>.jar --load-step-id=custom_test_0 ...
```

See also the [output reference](../../../output#111-load-step-id)

## 2. Limits

By default the load steps are not limited explicitly. There are several ways to limit the load steps execution.

### 2.1. Operations Count

Limit the load step
```bash
java -jar mongoose-<VERSION>.jar --load-op-limit-count=1000000 ...
```

### 2.2. Time

### 2.3. Transfer Size

### 2.4. End Of Input
