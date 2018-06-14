# Introduction

Some cases require to perform the load operations on the limited set of
objects/files in the unlimited manner (infinitely either using time limit).

# Limitations

1. Load type is one of
    * Noop
    * Read
    * Update
2. Items input is configured
3. Items input supplies the items which count is no more than the
    configured recycle queue size

# Requirements

1. Recycle the load operations on the items.
2. Sustain the initial items input order.
3. Respect the operation weight in case of the weighted load.
4. Respect the rate limit if configured.
5. Output only the latest items state.

# Approach

The [Load Generator](architecture.md#load-generator) component is
responsible for the tasks recycling. There are two additional methods in
its interface to support the recycling:
* Check if the load generator is configured to recycle the tasks:
    ```java
    boolean isRecycling();
    ```
* Enqueue the load task for further recycling:
    ```java
    void recycle(final O ioTask);
    ```

The [Load Controller](architecture.md#load-controller) component also
controls the recycling. It should keep in memory the latest state for
all items being processed in the recycle mode to meet the requirement
(5).

Also, there's a specific ***"nothing to recycle"*** state which requires
a Load Controller to detect it to not to hang up the test step. Specific
conditions:
1. Single Load Generator.
2. The Load Generator is finished to produce *new* load tasks.
3. The Load Generator is recycling.
4. All new load tasks executed at least once.
5. No successful load task results.

## Recycle Flow

1. Load Generator:
    1. Produces the new load tasks which count is no more than the
        configured recycle queue size
2. Storage Driver:
    1. Resets the next load task state (status, timestamps, etc)
    2. Executes the next load task
    3. Outputs the next completed load task to the Load Controller
3. Load Controller:
    1. Receives the next completed load task
    2. Drops the load task if its status is not successful
    3. Determines the load task *origin* (the Load Generator produced
        this load task)
    4. Checks if the resolved Load Generator *is in the recycling mode*
    5. Updates the latest results for the corresponding item.
    6. Enqueues the load task back to the Load Generator for further
        recycle
4. Load Generator:
    1. Begins to recycle only if items input is exhausted
    2. Produces the recycling load tasks from the recycle queue

# Configuration

1. `load-generator-recycle-enabled`

    The flag to enable the recycle mode. Disabled by default.

2. `load-generator-recycle-limit`

    The recycle queue size. Note that this queue size is also used by
    storage drivers internal queues.

# Output

## Items Output

The items are processed multiple times in the recycle mode. For
example, the file may be updated several times. The result of each
next update operation is different. So the Load Controller doesn't
output the items info on the fly but keeps the latest items info in the
memory. The processed items info is dumped at the test step finish in
the recycle mode (once for each item).
