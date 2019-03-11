# Defaults

Mongoose uses the assumed defaults when run. It makes Mongoose quite simple to use, allowing *just to run* it:
```bash
java --jar mongoose-<VERSION>.jar
```

## 1. Input

### 1.1. Configuration

For each load step the default configuration values are loaded in the following order:

1. The defaults configuration file.
2. Command-line arguments.
3. The configuration options for the given load step from the scenario file.

Note that this is also the configuration options overriding order, e.g. the configuration options loaded from the
scenario overriding the same command-line configuration option values and the values from the defaults file.

### 1.2. Scenario

By default, Mongoose starts the *default* scenario file which contains just a single *linear* load step without any
specific:
```javascript
Load.run();
```

This behaviour may be overriden by supplying the custom scenario file name via CLI arguments:
```bash
java -jar mongoose-<VERSION>.jar --run-scenario=custom_scenario_file.js
```

## 2. Output

|   |   |
|---|---|
| Console colors | Yes
| Console metrics output period | 10 seconds
| Persist the periodic metrics also into the file | Yes
| Repeat the console metrics table header | Every 20 rows
| Persist the summary metrics also into the file | Yes
| Persist the metrics for every load operation into the file | No
| Account the separate metrics if the specified concurrency threshold is reached | No

## 3. Load Generation

|   |   |
|---|---|
| Item type | Data
| Item size | 1MB
| Item payload | Random bytes, defined by the default seed value
| Item input | New items generator
| Item input names | Random ids, no prefix, radix = 36, name length = 12
| Item output file | None, the items info will not be persisted by default
| Item output storage path | The value will be generate once on the fly using a timestamp
| Load type | Create
| Load step id | Not specified, generate on the fly using a timestamp
| Load step limits | None, infinite (until a user interrupts)
| Load step type | *Linear*

## 4. Storage

|   |   |
|---|---|
| Storage authentication | None (no uid/secret/token/etc is used)
| Storage driver | S3
| Storage nodes | 127.0.0.1:9020

## 4. Load Scaling

|                   |                  |
|-------------------|------------------|
| Rate limit        | none             |
| Concurrency Limit | 1                |
| Distributed mode  | off (standalone) |
