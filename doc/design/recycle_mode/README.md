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

The [Load Generator](../architecture/README.md#22-load-generator) component is
responsible for the operations recycling. There are two additional methods in
its interface to support the recycling:
* Check if the load generator is configured to recycle the operations:
    ```java
    boolean isRecycling();
    ```
* Enqueue the load operation for further recycling:
    ```java
    void recycle(final O op);
    ```

The [Load Step Context](../architecture/README.md#23-load-step-context) component keeps in memory the latest state for all items
being processed in the recycle mode to meet the requirement (5).

Also, there's a specific ***"nothing to recycle"*** state which requires
a Load Step Context to detect it to not to hang up the test step. Specific
conditions:
1. The Load Generator is finished to produce *new* load operations.
2. The Load Generator is recycling.
3. All new load operations executed at least once.
4. No successful load operation results.

## Recycle Flow

1. Load Generator:
    1. Produces the new load operations which count is no more than the
        configured recycle queue size
2. Storage Driver:
    1. Resets the next load operation state (status, timestamps, etc)
    2. Executes the next load operation
    3. Outputs the next completed load operation to the Load Controller
3. Load Step Context:
    1. Receives the next completed load operation
    2. Drops the load operation if its status is not successful
    3. Determines the load operation *origin* (the Load Generator produced
        this load operation)
    4. Checks if the resolved Load Generator *is in the recycling mode*
    5. Updates the latest results for the corresponding item.
    6. Enqueues the load operation back to the Load Generator for further
        recycle
4. Load Generator:
    1. Begins to recycle only if items input is exhausted
    2. Produces the recycling load operations from the recycle queue

# Configuration

1. `load-op-recycle`

    The flag to enable the recycle mode. Disabled by default.

2. `load-op-imit-recycle`

    The recycle queue size. Note that this queue size is also used by storage drivers internal queues.

# Output

## Items Output

The items are processed multiple times in the recycle mode. For
example, the file may be updated several times. The result of each
next update operation is different. So the Load Step Context doesn't
output the items info on the fly but keeps the latest items info in the
memory. The processed items info is dumped at the test step finish in
the recycle mode (once for each item).
