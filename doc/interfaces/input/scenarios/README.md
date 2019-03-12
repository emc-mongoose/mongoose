# Scenarios

1. [Introduction](#1-introduction)<br/>
1.1. [JSR-223 Compliance](#11-jsr-223-compliance)<br/>
1.2. [New Version Changes](#12-new-version-changes)<br/>
2. [DSL](#2-dsl)<br/>
2.1. [Step](#21-step)<br/>
2.1.1. [Methods](#211-methods)<br/>
2.1.2. [Types](#212-types)<br/>
2.1.2.1. [Basic](#2121-basic)<br/>
2.1.2.2. [Additional Shortcuts](#2122-additional-shortcuts)<br/>
2.1.2.2.1. [Built-In](#21221-built-in)<br/>
2.1.2.2.2. [Custom](#21222-custom)<br/>
2.2. [Values Substitution](#22-values-substitution)<br/>
2.3. [Non-Blocking Execution](#23-non-blocking-execution)<br/>
2.4. [Resume Support](#24-resume-support)<br/>
3. [Examples](#3-examples)<br/>
3.1. [Javascript](#31-javascript)<br/>
3.1.1. [External Command](#311-external-command)<br/>
3.1.2. [Load](#312-load)<br/>
3.1.3. [Weighted Load](#313-weighted-load)<br/>
3.1.4. [Pipeline Load](#314-pipeline-load)<br/>
3.2. [Other Scripting Languages](#32-other-scripting-languages)

## 1. Introduction

The user scenarios are executed by the
[Java's scripting engine](https://docs.oracle.com/javase/8/docs/technotes/guides/scripting/).
Mongoose exposes the specific DSL to the user scenarios to run the performance tests.

### 1.1. JSR-223 Compliance

The JSR-223 compliant scenario engines support has the following benefits in comparison to the old
approach:
* The full power of scripting language including conditions, arithmetic, variables, etc
* Ability to customize and reuse the load steps
* Less complicated support (most of the logic and complexity goes to the 3rd party scripting engine
which is much more stable and established)

## 1.2. New Version Changes

* The JSON scenarios syntax  was deprecated in the previous major version. In the new version the
functionality has been removed.

* Parallel step wrapper was deprecated and replaced by the
[non-blocking execution](#23-non-blocking-execution) methods. New methods allow more flexible
execution flow control, including the [Resume Support](#24-resume-support).

* Command step type was removed. The new [external command](#311-external-command) should be called
using the particular scenario language method.

## 2. DSL

Generally, to define a scenario a user should:
1. Define at least one load step in the script
2. Invoke the defined steps using method ```run``` either ```start```

**Note:**
Javascript scenarios are supported out-of-the-box (the corresponding
engine is included in the JVM by default). So the examples below are in
Javascript. It's necessary to put the custom JSR-223 implementation jar
to the `<USER_HOME_DIR>/.mongoose/<VERSION>/ext` directory to use other scripting languages for
scenarios.

### 2.1. Load Step

The load step is the basic entity which should be used to make a scenario.
Mongoose's DSL defines few basic steps and a lot of additional shortcut
steps.

> **Note:**
> All scenario statements are executed locally except the load steps. In the
> distributed mode the steps are sliced and executed on the nodes (both entry and additional nodes).

#### 2.1.1. Methods

1. ```config(config)```

    Configure the load step. Merge the specified configuration parameters dictionary with already existing step's
    configuration. An argument should be a dictionary with a structure equivalent to the configuration structure (see
    `<USER_HOME_DIR>/.mongoose/<VERSION>/config/defaults.yaml` file for the reference). Returns the copied instance with
    the new configuration.

2. ```append(config)```

    Appends the configuration parameters dictionary to the step's sequence of the contexts. The resulting sequence of
    the appended configuration elements is used by complex load step implementations such as
    [Pipeline](#314-pipeline-load) either [Weighted](#313-weighted-load) load. Returns the copied instance with the new
    sequence of the contexts.

2. ```start()```

    Start/resume the step execution. Returns the same instance with state changed to *STARTED* if
    call was successful.

3. ```stop()```

    Stop (with further resumption capability) the step. Returns the same instance with state changed
    to *STOPPED* if call was successful.

4. ```await()```

    Wait while the step state is *STARTED*. Returns the same instance.

5. ```close()```

    Free the resources allocated by the step instance. Stops the step execution if was started. Should be invoked after
    all other step's methods (also terminate the call chain on the step if any). Returns nothing.

6. ```run()```

    The convenience method for blocking execution flow. Includes ```start()```, ```await()``` and
    ```close()``` calls sequence. Returns nothing.

#### 2.1.2. Types

##### 2.1.2.1. Basic

| Step Type Name | Description | Example |
|----------------|-------------|---------|
| Load | Execute a linear load | [link](#312-linear-load) |
| PipelineLoad | Execute a pipeline load | [link](#314-pipeline-load) |
| WeightedLoad | Execute a weighted load | [link](#313-weighted-load) |

##### 2.1.2.2. Additional Shortcuts

###### 2.1.2.2.1. Built-In

1. `PreconditionLoad`

    The same as load but with disabled metrics output.

2. `NoopLoad`

    A load with `noop` operations type.

3. `CreateLoad`

    A load with `create` operations type.

4. `ReadLoad`

    A load with `read` operations type.

5. `UpdateLoad`

    A load with `update` operations type.

6. `DeleteLoad`

    A load with `delete` operations type.

7. `ReadVerifyLoad`

    A load with `read` operations type and enabled content verification.

8. `ReadRandomRangeLoad`

    A load with `read` operations type. Performs the reading of a single
    random byte range per operation.

9. `ReadVerifyRandomRangeLoad`

    A load with `read` operations type. Performs the reading of a single
    random byte range per operation. The data being read is verified
    also.

10. `UpdateRandomRangeLoad`

    A load with `read` operations type. Performs the single byte range
    update operations.

###### 2.1.2.2.2. Custom

It's possible to define a custom shortcut step types what is especially
useful for the `load` step type and its derivatives. This is because the
`load` step supports only single configuration element so configuring
multiple configuration elements lead to merging them into the single.
The additivity rule works for the new configuration parameters (not set
before). If the same configuration parameter is set again in the
subsequent `config` call its value will be overridden.

Javascript example:

```javascript
var copyLoadUsingEnvVars = CreateLoad
    .config(
        {
            "item": {
                "input": {
                    "file": ITEM_INPUT_FILE,
                    "path": ITEM_INPUT_PATH
                },
                "output": {
                    "path": ITEM_OUTPUT_PATH
                }
            }
        }
    );

copyLoadUsingEnvVars.run();
```

The example applies the additional configuration to the `create_load`
step (which is derivative of the `load` step) making the new `copy_load`
step. The scenario uses the following environment variables:
`ITEM_INPUT_FILE`, `ITEM_INPUT_PATH`, `ITEM_OUTPUT_PATH`. To perform a
copy load step its necessary to supply only one valid items input, e.g.
specify `ITEM_INPUT_FILE` value either `ITEM_INPUT_PATH`. The
`ITEM_OUTPUT_PATH` value specifies the destination path for the items
being copied.

To use the scenario above to copy the items using the items input file
the following command may be issued:

```bash
export ITEM_INPUT_FILE=items2copy.csv
export ITEM_OUTPUT_PATH=/destination_path_bucket_container_etc
java -jar <MONGOOSE_DIR>/mongoose.jar \
    --run-scenario=copy.js
```

Otherwise, to use the scenario above to copy the items using the items
input path (copy from the source path/bucket/container/etc to the target
one):

```bash
export ITEM_INPUT_PATH=/source_path_bucket_container_etc
export ITEM_OUTPUT_PATH=/target_path_bucket_container_etc
java -jar <MONGOOSE_DIR>/mongoose.jar \
    --run-scenario=copy.js
```

### 2.2. Values Substitution

The environment variables are accessible from a scenario with the same
name:

```bash
export ZONE1_ADDRS=127.0.0.1
export ZONE2_ADDRS=192.168.1.136
java -jar mongoose-next/mongoose.jar \
    --run-scenario=scenario/jsr223/js/pipeline_with_delay.js
```

The Javascript example scenario which uses these environment variables:

```javascript
var ctx1 = {
    "item" : {
        "output" : {
            "delay" : "1m",
            "path" : "/default"
        }
    },
    "storage" : {
        "net" : {
            "node" : {
                "addrs" : ZONE1_ADDRS
            }
        }
    }
};

var ctx2 = {
    "load" : {
        "type" : "read"
    },
    "storage" : {
        "net" : {
            "node" : {
                "addrs" : ZONE2_ADDRS
            }
        }
    }
};

PipelineLoad
    .append(ctx1)
    .append(ctx2)
    .run();
```

## 2.3. Non-Blocking Execution

In the new version it's possible to execute the steps asynchronously. This allows to deprecate the
special ```parallel``` step wrapper.

Previous version (not supported anymore):
```javascript
Parallel
    .step(step1)
    .step(step2)
    .run();
```

New version:
```javascript

// start both steps
step1.start();
step2.start();

// wait for the 1st step to finish and stop the 2nd step immediately
step1.await();
step2.stop();

// free the resources
step1.close();
step2.close();
```

**Note**:
It's necessary to invoke ```close()``` method if asynchronous execution mode is used.

## 2.4. Resume Support

```start``` and ```stop``` methods may be used as resume and pause accordingly (if the particular
step implementation supports the pausing/resuming).

```javascript
var startedLoadStep = Load
    .config(...)
    .start()

// do something, wait some time, parse the logs, whatever
...

var pausedLoadStep = startedLoadStep.stop();

// do something more
...

var resumedLoadStep = pausedLoadStep.start();
...
```

## 3. Examples

The complete set of the example scenarios is available in the
`<MONGOOSE_DIR>/example/scenario` directory. The scenarios are sorted by
the language.

### 3.1. Javascript

Javascript is the default scripting language which should be used for
the custom user scenarios. Other scripting languages are not supported.
n user may use deprecated JSON scenarios either JSR-223 compliant
scripting language at his/her own risk.

#### 3.1.1. External Command

Example:

```javascript
// start the process
var cmd = new java.lang.ProcessBuilder()
    .command("sh", "-c", "echo Hello world!")
    .inheritIO()
    .start();

// wait until the command finishes
cmd.waitFor();
```

**Notes**:

1. The command is invoked using `/bin/sh -c <CMD>` command in order to support additional Unix shell
capabilities like the pipeline. This allows to run the complex commands like `ps alx | grep java`
but is not portable.

2. The ```config``` method accepts single string value as argument. Subsequent calls on the same
step discards previous value setting. **Returns** the new step instance of the same type so the
call may be included into the call chain.

#### 3.1.2. Linear Load

Example:

```javascript
Load.run();
```

The ```config``` method appends the configuration structure element to the step.
An argument should be a dictionary/map with a structure equivalent to the configuration structure
(see `<MONGOOSE_DIR>/config/defaults.yaml` file for the reference). Subsequent calls merge the
configurations (see the [details](#21222-custom)). **Returns** the new step instance of the same
type so the call may be included into the call chain.

#### 3.1.3. Weighted Load

See also the [weighted load spec](../design/weighted_load.md)

Example:

```javascript
WeightedLoad
    .append(
        {
            "load" : {
                "generator" : {
                    "weight" : 20
                },
                "step" : {
                    "limit" : {
                        "time" : "5m"
                    }
                }
                "type" : "create"
            }
        }
    )
    .append(
        {
            "load" : {
                "generator" : {
                    "recycle" : {
                        "enabled" : true
                    },
                    "weight" : 80
                },
                "type" : "read"
            }
        }
    )
    .run();
```

The ```config``` method appends the configuration structure element to the step.
An argument should be a dictionary/map with a structure equivalent to the configuration structure
(see `<MONGOOSE_DIR>/config/defaults.yaml` file for the reference). Subsequent calls on the same
step appends the configuration structure to the list. **Returns** the new step instance of the same
type so the call may be included into the call chain.

#### 3.1.4. Pipeline Load

See also the [pipeline load spec](../design/pipeline_load.md)

Example:

```javascript
var createCtx = {
    "load" : {
        "step" : {
            "limit" : {
                "time" : "1h"
            }
        }
    }
};

var readCtx = {
    "load" : {
        "type" : "read"
    }
};

var deleteCtx = {
    "item" : {
        "output" : {
            "file" : "items_passed_through_create_read_delete_pipeline.csv"
        }
    },
    "load" : {
        "type" : "delete"
    }
};

PipelineLoad
    .append(createCtx)
    .append(readCtx)
    .append(deleteCtx)
    .run();
```

The example using the *delay between the operations* capability:

```javascript
var ctx1 = {
    "item" : {
        "output" : {
            "delay" : "1m",
            "path" : "/default"
        }
    },
    "storage" : {
        "net" : {
            "node" : {
                "addrs" : ZONE1_ADDRS
            }
        }
    }
};

var ctx2 = {
    "load" : {
        "type" : "read"
    },
    "storage" : {
        "net" : {
            "node" : {
                "addrs" : ZONE2_ADDRS
            }
        }
    }
};

PipelineLoad
    .append(ctx1)
    .append(ctx2)
    .run();
```

Note the environment variables `ZONE1_ADDRS` and `ZONE2_ADDRS` which
should be set externally.

The ```config``` method appends the configuration structure element to
the step. An argument should be a dictionary/map with a structure
equivalent to the configuration structure (see
`<USER_HOME_DIR>/.mongoose/<VERSION>/config/defaults.yaml` file for the
reference). Subsequent calls on the same step appends the configuration
structure to the list. **Returns** the new step instance of the same
type so the call may be included into the call chain.

## 3.2. Other Scripting Languages

It's possible to use other JSR-223 compliant scripting language to write
down custom scripts. However there's no official support for other
scripting languages. The are example dockerfiles for the Groovy support
and for the Jython support are available for the reference.
