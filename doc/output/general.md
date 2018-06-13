# Logging Subsystem

Mongoose uses the [Apache Log4J2](http://logging.apache.org/log4j/2.x/) library to handle the
overwhelming majority of its output due to
[high performance capabilities](http://logging.apache.org/log4j/2.x/performance.html).

The Log4J2 configuration is bundled into the `mongoose-ui.jar` resources as `log4j2.json` file and
is not designated to be changed by user since v3.5. Also the `log4j2.component.properties` file
bundled into that jar to specify the additional options tuning the logging subsystem performance.

## Console

Console output is slightly colored by default for better readability.
To disable the console output coloring set the `output-color` configuration option to "false".

## Files

The most of log messages are written to the output files using dynamic output file path:
`<MONGOOSE_DIR>/log/<STEP_ID>/...` where "STEP_ID" may change during runtime.

# Categories

## CLI Arguments

The CLI arguments are logged once per run into the `cli.args.log` log
file.

## Configuration Dump

The configuration (defaults after the CLI arguments applied) is logged
once per run into the `config.json` log file.

## Scenario Dump

The scenario used for the test is logged once per run into the
`scenario.txt` log file.

## 3rd Party Log Messages

The log messages from the 3rd party components (dependency libraries) are intercepted and logged
into the `3rdparty.log` file with the *dynamic path*. Logging level: "INFO".

## Error Messages

* Error messages with level "INFO" or higher are displayed on the console with orange color.
* Error messages with level "DEBUG" or higher are written to the file `errors.log` with the
  *dynamic path*

## Informational Messages

* Info messages with level "INFO" or higher are displayed on the console with grey color.
* Info messages with level "DEBUG" or higher are written to the file `messages.log` with the
  *dynamic path*

## Multipart Upload

S3-specific multipart upload identifiers log file `parts.upload.csv` with the dynamic path.
Contains the comma-separated records with:
1. Full Item Path.
2. Upload Id.
3. Multipart upload completion response latency.

## Item List Files

To persist the info about the items processed by a load step the items output file should be used.

```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --test-step-limit-count=1000 --item-output-file=items.csv
```
In the example above the info about 1000 items processed by the load step will be persisted in the
`items.csv` output file.

The items list file may be useful if it's needed to perform another load step using these items:
```bash
java -jar <MONGOOSE_DIR>/mongoose.jar --read --item-input-file=items.csv
```

Items list file contains the CSV records each occupying a line.
Each record contains comma-separated values which are:
1. Full Item Path
2. Data ring buffer offset (hexadecimal)
3. Size (decimal)
4. A pair of [layer/mask](../design/data_reentrancy.md) values separated with "/" character
