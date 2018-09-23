# Load Scaling

## 1. Rate

It's possible to limit the load operations rate. Example:
```bash
java -jar mongoose-<VERSION>.jar --load-op-limit-rate=10000
```

***Note***
> The rate limit implementation uses the adaptive throttle, so it works with limited level of accuracy.

## 2. Concurrency

## 3. Distributed Mode
