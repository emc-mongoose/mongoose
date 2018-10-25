# [Netty](https://netty.io/)-based Storage Driver

## 1. Configuration Reference

| Name                                           | Type         | Default Value    | Description                                      |
|:-----------------------------------------------|:-------------|:-----------------|:-------------------------------------------------|
| storage-net-node-addrs                         | List of strings | 127.0.0.1 | The list of the storage node IPs or hostnames to use for HTTP load. May include port numbers.
| storage-net-node-connAttemptsLimit             | Integer >= 0 | 0 | The limit for the subsequent connection attempts for each storage endpoint node. The node is excluded from the connection pool forever if the node has more subsequent connection failures. The default value (0) means no limit.
| storage-net-node-port                          | Integer > 0 | 9020 | The common port number to access the storage nodes, may be overriden adding the port number to the storage-driver-addrs, for example: "127.0.0.1:9020,127.0.0.1:9022,..."
| storage-net-node-slice                         | Flag | false | Slice the storage node addresses between the mongoose nodes using the greatest common divisor or not
| storage-net-bindBacklogSize                    | Integer >= 0 | 0 |
| storage-net-interestOpQueued                   | Flag | false |
| storage-net-keepAlive                          | Flag | true |
| storage-net-linger                             | Integer >= 0 | 0 |
| storage-net-reuseAddr                          | Flag | true |
| storage-net-rcvBuf                             | Fixed size >= 0 | 0 | The network connection input buffer size. Estimated automatically if 0 (default)
| storage-net-sndBuf                             | Fixed size >= 0 | 0 | The network connection output buffer size. Estimated automatically if 0 (default)
| storage-net-selectInterval                     | Integer > 0 | 100 |
| storage-net-tcpNoDelay                         | Flag | true |
| storage-net-timeoutMilliSec                    | Integer >= 0 | 1000000 | The socket timeout
| storage-net-ioRatio                            | 0 < Integer < 100 | 50 | Internal [Netty's I/O ratio parameter](https://github.com/netty/netty/issues/1154#issuecomment-14870909). It's recommended to make it higher for large request/response payload (>1MB)
| storage-net-transport                          | Enum | nio | The I/O transport to use (see the [details](http://netty.io/wiki/native-transports.html)). By default tries to use "nio" (the most compatible). For Linux try to use "epoll", for MacOS/BSD use "kqueue" (requires rebuilding).
| storage-net-ssl                                | Flag | false | The flag to enable the load through SSL/TLS. Currently only HTTPS implementation is available. Have no effect if configured storage type is filesystem.

## 2. Node Balancing

Mongoose uses the round-robin way to distribute I/O tasks if multiple storage endpoints are used.
Mongoose will try to distribute the active connections equally among the endpoints if a connection fails.

## 3. SSL/TLS

```bash
java -jar mongoose-<VERSION>.jar \
    --storage-net-ssl \
    --storage-net-node-port=9021 \
    ...
```

## 4. Connection Timeout

```bash
java -jar mongoose-<VERSION>.jar \
    --storage-net-timeoutMillisec=100000 \
    ...
```

## 5. I/O Buffer Size

Mongoose automatically adopts the input and output buffer sizes depending on the step info. For
example, for *create* I/O type the input buffer size is set to the minimal value (4KB) and the
output buffer size is set to configured data item size (if any). If *read* I/O type is used the
behavior is right opposite - specific input buffer size and minimal output buffer size. This
improves the I/O performance significantly. But users may set the buffer sizes manually.

Example: setting the *input* buffer to 100KB:
```bash
java -jar mongoose-<VERSION>.jar \
    --storage-net-rcvBuf=100KB \
    ...
```

Example: setting the *output* buffer to 10MB:
```bash
java -jar mongoose-<VERSION>.jar \
    --storage-net-sndBuf=10MB \
    ...
```
