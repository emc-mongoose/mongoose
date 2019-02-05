* Core
    * Tests Solidification
    * Measurements Solidification
        * Fix the measurement issues like negative actual concurrency
        * Generate the baselines
    * Distributed Mode Complete Implementation
        Make the monitoring/control API available from any node of the Mongoose cluster, e.g. the metrics should be able
        to be aggregated from any node, the step may be stopped in the absence of the entry node, etc
* GUI
* Storage Drivers Implementation
    * Kafka
    * JDBC
        Required to prove that Mongoose is abstract enough to work against any kind of data storage
