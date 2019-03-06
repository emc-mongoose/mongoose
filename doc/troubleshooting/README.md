| # | Specific Condition | Symptom | Suggested Resolution |
|---|--------------------|--------|----------------------|
| 1 | High concurrency level configured | "Too many open files" error messages | Increase open files count limit, for example using the following shell command: <br/>`ulimit -n 1048576` |
| 2 | HTTP Storage Driver | 403 responses from the storage with message "time too skewed" | Align the clients system clock time with storage system clock |
| 3 | Mongoose node host has multiple external network interfaces | Local node fails to connect the remote Mongoose node because it binds to an IP that cannot be accessed from other hosts | You can explicitly specify the network interface to use via native Java's java.rmi.server.hostname configuration parameter. For example:<br/>`$ java -Djava.rmi.server.hostname=123.45.67.89 -jar mongoose-<VERSION>.jar` |
| 4 | Load using large data items I/O | Observed bandwidth is lower than using v3.4.x | Use lower `load-service-threads` value (1 or 2) |
| 5 | - Default ("s3") storage driver used<br> - "x-emc-*" custom header(s) is configured | 403 responses from the storage with message "signature mismatch" | Use "emcs3" storage driver
| 6 | Terminal doesn't support &gt;16 colors | Unreadable/meaningless symbols in the standard output | Try to set the configuration option `output-color` to `false`
| 7 | Java version < 11 | Exception w/ message: Class has been compiled by a more recent version of the Java Environment (class file version 55.0)... | Use Java version 11 or higher to run Mongoose

