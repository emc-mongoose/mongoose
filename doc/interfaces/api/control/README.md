# Configuring the port

When you launch the Mongoose, a server set up that accepts control requests (Control API) and exports metrics
([Monitoring API](doc/interfaces/api/monitoring)). To configure the server port, the parameter `--run-port` is used.
By default `--run-port=9999`.

# REST

Control API is divided into 3 categories:

|Category|Requests|Description|
|---|---|---|
|Config API|<li>get config<li>get schema|Allows you to get the full configuration and scheme for the node|
|Runs API|<li>get list of runs<li>start new run<li>get status of run<li>stop run|Allows you to manage runs|
|Logs API|<li>get logs<li>delete logs|Allows you to manage logs of runs|

See the details [here](https://app.swaggerhub.com/apis-docs/veronikaKochugova/Mongoose/4.1.0)

# Examples

## Config

TODO

## Run

Start a new scenario run:
```bash
curl -v -X POST \
    -F defaults=@src/test/robot/data/aggregated_defaults.json \
    -F scenario=@src/test/robot/data/scenario_dummy.js \
    http://localhost:9999/run
```

If successful, the response will contain the ETag header with the hexadecimal timestamp (Unix epoch time):
```bash
...
< HTTP/1.1 202 Accepted
< Date: Mon, 26 Nov 2018 18:35:50 GMT
< ETag: 167514e6082
< Content-Length: 0
...
```

This ETag should be considered as a run id and may be used to check the run state (using HEAD request) either stop it
(using DELETE request). The `If-Match` header with the hexadecimal run id value should be used also:

Checking the run state:
```bash
curl -v -X HEAD -H "If-Match: 167514e6082" http://localhost:9999/run
...
< HTTP/1.1 200 OK
< Date: Mon, 26 Nov 2018 18:40:10 GMT
< Content-Length: 0
...
```

Stopping the run:
```bash
curl -v -X DELETE -H "If-Match: 167514e6082" http://localhost:9999/run
...
< HTTP/1.1 200 OK
< Date: Mon, 26 Nov 2018 18:41:26 GMT
< Content-Length: 0
```

## Logs

### Get The Log File Page From The Beginning

```bash
curl http://localhost:9999/logs/123/com.emc.mongoose.logging.Messages
```

### Get The Specified Log File Part

```bash
curl -H "Range: bytes=100-200" http://localhost:9999/logs/123/com.emc.mongoose.logging.Messages
r the type "dummy-mock"
2018-11-27T16:19:34,982 | DEBUG | LinearLoadStepClient | main | com.emc.mongoose.storage.driver.mock.DummyStorageDriverMock@6aecbb8d: shut down
2018-11-27T16:19:34,982 | DEBUG |
```

### Delete Log File

```bash
curl -X DELETE http://localhost:9999/logs/123/com.emc.mongoose.logging.Messages
```
